package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.AiProviderConfigView;
import com.yuegang.zhihui.system.api.InternalAiProviderConfig;
import com.yuegang.zhihui.system.api.UpdateAiProviderConfigRequest;
import com.yuegang.zhihui.system.security.SystemSecretCipher;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

// 该业务类负责管理 AI 供应商（如豆包）的配置信息，包含密钥加密存储与配置变更审计。
public class AiProviderConfigService { // 定义最终类: AI 供应商配置服务
    private static final String SELECT = """
            SELECT provider,base_url,chat_model,embedding_model,web_search_enabled,api_key_ciphertext,api_key_nonce,
            version,updated_at
            FROM system_ai_provider_config WHERE config_id = 1
            """; // 定义静态常量:查询单条AI 配置的sQL 语句
    private final JdbcTemplate jdbc; // 声明 JDBC 模板对象
    private final SystemSecretCipher secrets; // 声明密钥加密对象

    public AiProviderConfigService(DataSource dataSource, SystemSecretCipher secrets) { // 构造函数
        this.jdbc = new JdbcTemplate(dataSource); // 初始化
        this.secrets = secrets; // 初始化加密器
    } // 构造函数结束

    private static AiProviderConfigView toView(String provider, String baseUrl, String chatModel,
                                               String embeddingModel, boolean webSearchEnabled,
                                               String ciphertext, long version,
                                               Timestamp updatedAt) {
        boolean configured = ciphertext != null && !ciphertext.isBlank(); // 判断密钥是否已配置
        OffsetDateTime changed = updatedAt == null ? null : updatedAt.toInstant().atOffset(ZoneOffset.UTC); // 时间戳转 OffsetDateTime
        return new AiProviderConfigView(provider, baseUrl, chatModel, embeddingModel, webSearchEnabled, configured, configured ? "......" : "未配置", version, changed); // 返回脱敏之后的视图对象
    } // 方法结束

    private static void validateBaseUrl(String value) {
        try { // 开启校验
            URI uri = URI.create(value.trim()); // 创建 URI 对象
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null // 强制要求 HTTP 和主机名
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) { // 检查 URI 的各个部分是否合法
                throw new IllegalArgumentException("Invalid base URL: " + value);
            }
        } catch (RuntimeException failure) { // 铺货解析异常
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 统一抛出校验失败业务异常
        }
    }

    private static String digest(String value) { // 私有静态方法：生成数据摘要
        try { // 开启哈希计算
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256") // 使用 SHA-256 算法
                    .digest(value.getBytes(StandardCharsets.UTF_8))); // 返回十六进制哈希字符串
        } catch (Exception impossible) { // 理论上不会发生的异常处理
            throw new IllegalArgumentException(impossible); // 抛出非法状态异常
        }
    } // 方法结束

    private static long nextId() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // 利 UUID 生成正整数型 ID
    }//方法结束

    public AiProviderConfigView update(UpdateAiProviderConfigRequest request, long operator) { // 方法：更新 AI 配置
        validateBaseUrl(request.baseUrl()); // 校验传入的基础 URL 格式
        var current = jdbc.queryForMap(SELECT); // 获取数据库中当前的配置快照
        String oldCiphertext = (String) current.get("api_key_ciphertext"); // 获取旧的
        String ciphertext = oldCiphertext; // 默认密文不变
        String nonce = (String) current.get("api_key_nonce"); // 默认随机数不变
        if (request.apiKey() != null && !request.apiKey().isBlank()) { // 如果请求中携带了新的明文密钥
            String normalized = request.apiKey().trim(); // 去除首尾空格
            if (normalized.length() < 16) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 密钥长度不足则抛错
            var encrypted = secrets.encrypt(normalized); // 调用加密器加密密钥
            ciphertext = encrypted.ciphertext(); // 更新密文
            nonce = encrypted.nonce(); // 更新加密随机数
        } // 密钥处理结束
        int updated = jdbc.update("""
                        UPDATE system_ai_provider_config SET provider=?,base_url=?,chat_model=?,embedding_model=?,
                        web_search_enabled=?,api_key_ciphertext=?,api_key_nonce=?,
                        updated_by=?,version=version+1
                        WHERE config_id=1 AND version=?
                        """, request.provider(), request.baseUrl().trim(), request.chatModel().trim(),
                request.embeddingModel().trim(), request.webSearchEnabled(), ciphertext, nonce, operator,
                request.version()); // 执行更新SQL
        if (updated != 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 更新行数不为1说明版本冲突
        // 计算旧配置的哈希摘要，用于审计
        String oldDigest = digest(String.join("|", current.get("provider").toString(),
                current.get("base_url").toString(), current.get("chat_model").toString(),
                current.get("embedding_model").toString(), current.get("web_search_enabled").toString(),
                oldCiphertext == null ? "" : oldCiphertext));
// 计算新配置的哈希摘要
        String newDigest = digest(String.join("|", request.provider(), request.baseUrl(),
                request.chatModel(), request.embeddingModel(), Boolean.toString(request.webSearchEnabled()),
                oldCiphertext == null ? "" : oldCiphertext));
// 将变更记录插入配置审计表
        jdbc.update("""
                INSERT INTO system_configuration_audit(
                    id,config_type,config_key,old_digest,new_digest,operator_user_id) VALUES(?,?,?,?,?,?)
                """, nextId(), "AI_PROVIDER", "primary", oldDigest, newDigest, operator); // 执行插入审计记录
        return view(); // 返回更新后的视图

    } // 方法结束

    public AiProviderConfigView view() { // 方法：获取前端展示用的配置视图
        return jdbc.queryForObject(SELECT, (row, index) -> toView(  //执行查询并转换为视图对象
                row.getString(1), row.getString(2), row.getString(3), row.getString(4), // 映射供应商、URL、模型
                row.getBoolean(5), row.getString(6), row.getLong(8), row.getTimestamp(9) // 映射供应商、URL、模型
        ));
    } //方法结束

    public InternalAiProviderConfig internal() { // 方法：获取系统内部调用的配置（含解密后密钥）
        return jdbc.queryForObject(SELECT, (row, index) -> new InternalAiProviderConfig(
                row.getString(1), row.getString(2), row.getString(3), row.getString(4), // 获取基础信息
                row.getBoolean(5), row.getNString(6), row.getLong(8) // 映射搜索开关，解密后的密码、版本
        )); // 方法结束
    }

}
