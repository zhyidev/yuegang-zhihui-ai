package com.yuegang.zhihui.user.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.user.security.UserInternalServiceVerifier;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 该类用于微服务内部调用，根据组织架构（部门、职位等）查询对应的用户 ID 列表
 */
@RestController // 标识为控制器类
@RequestMapping("/internal/v1/organization")
public final class InternalOrganizationTargetController { // 定义内部组织目标控制器类
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final UserInternalServiceVerifier verifier; // 声明内部服务验证器

    public InternalOrganizationTargetController(
            DataSource d, UserInternalServiceVerifier v) { // 构造函数初始化数据源和验证器
        jdbc = new JdbcTemplate(d);
        verifier = v;
    }

    private static long positive(String id) { // 内部静态辅助方法：将字符串解析为正数ID
        try {
            long v = Long.parseLong(id); // 解析长整形
            if (v <= 0) {
                throw new NumberFormatException();
            } // 小于等于9则视为无效格式
            return v;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 解析失败抛出业务验证异常
        }
    }

    @GetMapping("/targets")
    ApiResponse<List<String>> targets(
            @RequestParam String type,
            @RequestParam String id,
            HttpServletRequest r) { // 根据类型查询目标用户 ID 集合
        verifier.verify(r); // 执行内部服务间的签名验证（鉴权）
        List<String> users = switch (type) { // 根据查询类型执行不同的 SQL 逻辑
                    case "DEPARTMENT" -> {
                        long target = positive(id);
                        yield jdbc.queryForList(
                                "SELECT CAST(user_id AS CHAR) FROM user_employee WHERE department_id=? AND employment_status='ACTIVE'",
                                String.class,
                                target); // 按部门查询在职员工的用户ID
                    }
                    case "POSITION" -> {
                        long target = positive(id);
                        yield jdbc.queryForList(
                                "SELECT CAST(e.user_id AS CHAR) FROM user_employee e JOIN user_employee_position ep ON ep.employee_id=e.id WHERE ep.position_id=? AND e.employment_status='ACTIVE'",
                                String.class,
                                target); // 按职位查询在职员工的用户ID
                    }
                    case "EMPLOYEE" ->
                            jdbc.queryForList(
                                    "SELECT CAST(user_id AS CHAR) FROM user_employee WHERE (CAST(id AS CHAR)=? OR CAST(user_id AS CHAR)=? OR employee_no=?) AND employment_status='ACTIVE'",
                                    String.class,
                                    id,
                                    id,
                                    id); // 支持按员工ID、用户ID或工号查询（确保在职）
                    default ->
                            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 类型不匹配抛出校验异常
                };
        return ApiResponse.success(users, TraceIdResolver.resolve(r)); // 返回查询结果及追踪ID
    }
}
