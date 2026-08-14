package com.yuegang.zhihui.auth.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 管理JWT签名用的RSA密钥环，负责从文件系统安全加载PEM格式的私钥和公钥集，并进行完整权限校验
 */
public final class RsaSigningKeyRing { // 定义 RSA 签名密钥环类
    private static final Pattern SAFE_KID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"); // 安全 Key ID 正则
    private static final int MAX_PUBLIC_KEYS = 8; // 密钥环允许的最大公钥数量 (用于 Key Rotation)
    private final RSAKey activeKey; // 当前用于签名的活动密钥 (含私钥)
    private final List<JWK> publicKeys; // 公钥集合 (用于外部验证)

    private RsaSigningKeyRing(RSAKey activeKey, List<JWK> publicKeys) { // 私有构造函数
        this.activeKey = activeKey;
        this.publicKeys = List.copyOf(publicKeys);
    }

    public static RsaSigningKeyRing load(Path directory, String activeKid) { // 静态工厂，从目录中加载密钥
        Objects.requireNonNull(directory, "directory must not be null"); // 参数校验
        if (activeKid == null || !SAFE_KID.matcher(activeKid).matches()) {
            throw new IllegalArgumentException("active key is unsafe"); // KID 安全校验
        }
        Path root = directory.toAbsolutePath().normalize(); // 路径规范化
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("JWT key directory is unavailable"); // 确保目录存在且不是符号链接
        }
        try {
            requirePrivatePermissions(root); // 核心安全：校验目录权限（防止被非Root用户写入）
            List<Path> publicFiles;
            try (var paths = Files.list(root)) { // 获取目录下的所有文件
                publicFiles = paths.filter(path -> path.getFileName().toString().endsWith(".public.pem")) // 筛选公钥文件
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList(); // 排序以便确定性
            }
            if (publicFiles.isEmpty() || publicFiles.size() > MAX_PUBLIC_KEYS) {
                throw new IllegalStateException("JWT public key count is outside allowed bounds"); // 公钥数量检查
            }
            var publicKeys = new ArrayList<JWK>(); // 临时列表
            RSAKey activePublic = null; // 声明匹配活动ID的公钥
            for (Path publicFile : publicFiles) { // 遍历处理公钥
                requireRegularChild(root, publicFile); // 安全校验：确保文件是直接子文件且权限正确
                String fileName = publicFile.getFileName().toString();
                String kid = fileName.substring(0, fileName.length() - ".public.pem".length()); // 提取 Key ID
                if (!SAFE_KID.matcher(kid).matches()) throw new IllegalStateException("JWT key id is unsafe"); // 内部ID校验
                RSAPublicKey publicKey = readPublic(publicFile); // 从 PEM 文件读取公钥
                requireStrength(publicKey); // 核心安全：校验公钥模数长度（必须 >= 2048位）
                RSAKey jwk = new RSAKey.Builder(publicKey).keyID(kid).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build(); // 组装 JWT 对象
                publicKeys.add(jwk); // 加入集合
                if (kid.equals(activeKid)) activePublic = jwk; // 标记匹配活动ID的公钥项
            }
            if (activePublic == null) throw new IllegalStateException("active JWT public key is missing"); // 找不到活动公钥报错
            Path privateFile = root.resolve(activeKid + ".private.pem").normalize(); // 定位私钥文件
            requireRegularChild(root, privateFile); // 安全校验私钥文件
            RSAPrivateKey privateKey = readPrivate(privateFile); // 读取私钥
            // 校验公钥和私钥函数是否匹配
            if (!privateKey.getModulus().equals(activePublic.toRSAPublicKey().getModulus())) {
                throw new IllegalStateException("active JWT key pair does not match");
            }
            // 进一步通过公指数校验匹配度（如果是 CRT 密钥）
            if (!(privateKey instanceof RSAPrivateKey crtKey) || !crtKey.getPrivateExponent().equals(activePublic.toRSAPublicKey().getPublicExponent())) {
                throw new IllegalStateException("active JWT pair exponent does not match");
            }
            verifyKeyPair(privateKey, activePublic.toRSAPublicKey()); // 核心验证: 通过一次模拟签名/验签确认密钥对有效
            RSAKey active = new RSAKey.Builder(activePublic).privateKey(privateKey).build(); // 组装完整私钥 JWK
            return new RsaSigningKeyRing(active, publicKeys); // 返回密钥环

        } catch (IOException | java.security.GeneralSecurityException | JOSEException failure) { // 捕获底层安全/IO异常
            throw new IllegalStateException("JWT signing key ring cannot be loaded", failure);
        }
    }

    private static void requireRegularChild(Path root, Path file) throws IOException { // 文件属性强制校验方法
        Path normalized = file.toAbsolutePath().normalize(); // 规范化
        if (!normalized.getParent().equals(root) || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(normalized)) { // 禁止跨目录、禁止非普通文件、禁止符号链接
            throw new IOException("JWT key file is not a regular direct child");
        }
        if (normalized.getFileName().toString().endsWith("private.pem"))
            requirePrivatePermissions(normalized); // 私钥需要额外权限检查
    }

    private static void requirePrivatePermissions(Path file) throws IOException { // POSIX 权限严格检查
        try {
            var permissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS); // 获取文件权限
            var forbidden = java.util.EnumSet.of( // 定义严禁出现的权限: 任何组或他人的读、写、执行权限
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE);
            if (permissions.stream().anyMatch(forbidden::contains)) { // 如果存在危险权限
                throw new IOException("JWT private key permissions are too broad"); // 报错: 私钥权限范围过大
            }
        } catch (UnsupportedOperationException ignoredOnNonPosixFilesSystem) { // Windows环境忽略 POSIX 检

        }
    }

    private static void requireDirectoryPermissions(Path directory) throws IOException { // 目录权限校验（防止同组用户替换密钥）
        try {
            var permissions = Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS);
            if (permissions.contains(PosixFilePermission.GROUP_WRITE) || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                throw new IOException("JWT kwy directory is writeable by group or others"); // 禁止其他用户有写权限
            }
        } catch (UnsupportedEncodingException ignoredOnNonPosixFilesSystem) {
            // windows 环境同上（暂时省略）
        }
    }

    private static void verifyKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) throws java.security.GeneralSecurityException { // 密钥对可行性
        byte[] challenge = new byte[32]; // 准备 32 字节随机挑战数据
        byte[] signatureBytes = null;
        new java.security.SecureRandom().nextBytes(challenge); // 生成随机数
        try {
            var signer = java.security.Signature.getInstance("SHA256withRSA"); // 使用 SHA256withRSA 算法
            signer.initSign(privateKey); // 私钥初始化签名
            signer.update(challenge); // 输入数据
            signatureBytes = signer.sign(); // 生成签名
            var verifier = java.security.Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey); // 公钥初始化验证
            verifier.update(challenge);
            if (!verifier.verify(signatureBytes))
                throw new java.security.InvalidKeyException("JWT key pair verification filed"); // 签名匹配失败
        } finally { // 安全清理
            Arrays.fill(challenge, (byte) 0);
            if (signatureBytes != null) Arrays.fill(signatureBytes, (byte) 0);
        }
    }

    private static RSAPublicKey readPublic(Path path) throws IOException, java.security.GeneralSecurityException { // 读取公钥辅助方法
        byte[] encoded = decodePem(path, "PUBLIC KEY"); // PEM解码
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded)); // 生成公钥对象
        } finally {
            Arrays.fill(encoded, (byte) 0); // 擦除中间数据
        }
    }

    private static RSAPrivateKey readPrivate(Path path) throws IOException, java.security.GeneralSecurityException { // 读取系统私钥辅助方法
        byte[] encoded = decodePem(path, "PRIVATE KEY"); // PEM解码
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded)); // 生成私钥对象
        } finally {
            Arrays.fill(encoded, (byte) 0); // 擦除
        }
    }

    private static byte[] decodePem(Path path, String type) throws IOException { // PEM 格式解析逻辑
        byte[] pem = readWithoutFollowingLinks(path); // 读取文件内容
        byte[] begin = ("-----BEGIN " + type + "-----").getBytes(StandardCharsets.US_ASCII); // 构建头标识
        byte[] end = ("-----END " + type + "-----").getBytes(StandardCharsets.US_ASCII); // 构建尾标识
        byte[] body = null;
        try {
            int beginAt = indexOf(pem, begin, 0); // 定位开始位置
            int endAt = indexOf(pem, end, begin.length); // 定位结束位置
            if (beginAt != 0 || endAt < 0 || !onlyAsciiWhitespace(pem, endAt + end.length)) {
                throw new IOException("invalid PEM envelope"); // 信封格式错误
            }
            body = Arrays.copyOfRange(pem, begin.length, endAt); // 截取 Base64 正文
            if (!validBase64Body(body)) throw new IOException("invalid PEM body characters"); // 字符集校验
            try {
                return Base64.getMimeDecoder().decode(body);
            } // 执行 MIME 解码
            catch (IllegalArgumentException malformed) {
                throw new IOException("invalid PEM body", malformed);
            }
        } catch (IllegalArgumentException malformed) {
            throw new IOException("invalid PEM body", malformed);
        } finally {
            Arrays.fill(pem, (byte) 0); // 擦除所有中间数据
            if (body != null) Arrays.fill(body, (byte) 0);
        }
    }

    private static byte[] readWithoutFollowingLinks(Path path) throws IOException { // 安全读取文件
        Path parent = path.getParent();
        try (var directory = Files.newDirectoryStream(parent)) { // 获取目录流
            java.nio.channels.SeekableByteChannel channel;
            if (directory instanceof SecureDirectoryStream<Path> secureDirectory) { // 针对支持安全流的 OS 使用原子句柄
                channel = secureDirectory.newByteChannel(path.getFileName(), java.util.Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            } else {
                // 如果不支持安全流，至少在打开前检查权限视图是否一致
                if (Files.getFileAttributeView(parent, java.nio.file.attribute.PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) != null)
                    throw new IOException("secure JWT key directory access is unavailable");
                channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
            }

            try (channel) { // 限制文件大小在 32KB 内（防止非授权大文件耗尽内存）
                if (channel.size() <= 0 || channel.size() > 32 * 1024)
                    throw new IOException("JWT key file size is invalid");
                ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(channel.size()));
                try {
                    while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                    } // 读取内容
                    if (buffer.hasRemaining())
                        throw new IOException("JWT key file was truncated while reading"); // 防止读写竞争导致截断
                    return Arrays.copyOf(buffer.array(), buffer.position()); // 返回最终字节
                } finally {
                    Arrays.fill(buffer.array(), (byte) 0); // erase
                }
            }

        }
    }

    private static int indexOf(byte[] source, byte[] target, int from) { // 字节数组寻找子集的辅助方法
        outer:
        for (int index = from; index <= source.length - target.length; index++) {
            for (int offset = 0; offset < target.length; offset++) {
                if (source[index + offset] != target[offset]) continue outer;
            }
            return index;
        }
        return -1; // -1表示没找到
    }

    private static boolean onlyAsciiWhitespace(byte[] source, int from) { // 校验末尾是否仅含空白符
        for (int index = from; index < source.length; index++) {
            byte value = source[index];
            if (value != ' ' && value != '\r' && value != '\n' && value != '\t') return false;
        }
        return true;
    }

    private static boolean validBase64Body(byte[] body) { // 手动校验 Base64 字符合法性（含换行符）
        for (byte value : body) {
            boolean base64 = value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z' || value >= '0' && value <= '9' || value == '+' || value == '/' || value == '=';
            boolean whitespace = value == ' ' || value == '\r' || value == '\n' || value == '\t';
            if (!base64 && !whitespace) return false;
        }
        return true;
    }

    private static void requireStrength(RSAPublicKey key) { // 密钥强度策略强制执行
        if (key.getModulus().bitLength() < 2048)
            throw new IllegalStateException("JWT RSA key is weaker than 2048 bits"); // 低于2048位视为不安全
    }

    RSAKey activeSigningKey() {
        return activeKey;
    } // 获取活动签名私钥

    public Map<String, Object> publicJwkSet() {
        return new JWKSet(publicKeys).toJSONObject();
    } // 获取供外部查询的公钥集 JSON (JWKS)

}
