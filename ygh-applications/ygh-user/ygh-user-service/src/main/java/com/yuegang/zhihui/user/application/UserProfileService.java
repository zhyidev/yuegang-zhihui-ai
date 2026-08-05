package com.yuegang.zhihui.user.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.user.api.UpdateUserProfileRequest;
import com.yuegang.zhihui.user.api.UserProfileView;
import com.yuegang.zhihui.user.domain.UserProfileRepository;

import java.net.URI;
import java.time.ZoneId;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 用户个人资料相关的业务逻辑和校验
 */
public class UserProfileService { // 定义用户资料服务类
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9][0-9 -]{5,31}"); // 电话号码正则：允许+号开头，包含5到31位数字/空格/横杠
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"); // 简易电子邮箱正则
    private final UserProfileRepository repository; // 仓储成员变量

    public UserProfileService(UserProfileRepository repository) { // 构造函数并进行非空检查
        this.repository = Objects.requireNonNull(repository);
    }
// AI-generated: method implementation
    public UserProfileView get(String userId) {
        long id = parse(userId);
        return repository.findByUserId(id)
                .orElseGet(() -> repository.save(id, defaultRequest())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_CONFLICT)));
    }
// AI-generated: method implementation
    public UserProfileView update(String userId, UpdateUserProfileRequest request) {
        validate(request);
        long id = parse(userId);
        return repository.save(id, request)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_CONFLICT));
    }

    private static void validate(UpdateUserProfileRequest request) {
        Objects.requireNonNull(request, "request is null");
        try {
            ZoneId zoneId = ZoneId.of(request.timezone()); // 校验时区是否合法
            Objects.requireNonNull(zoneId);
        } catch (RuntimeException invalid) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid timezone: " + request.timezone()); // 抛出验证异常
        }
        if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) { // 如果填了头像 URL
            try {
                URI uri = URI.create(request.avatarUrl());
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null) {
                    throw new IllegalArgumentException("Invalid avatar url: " + request.avatarUrl()); // 校验必要现有主机名
                }
            } catch (RuntimeException invalid2) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid avatar url: " + request.avatarUrl()); // URL 格式不正确
            }
        }
        if (request.phone() != null && !request.phone().isBlank() && !PHONE.matcher(request.phone().trim()).matches()) // 校验电话号码格式
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (request.email() != null && !request.email().isBlank() && !EMAIL.matcher(request.email().trim()).matches()) // 校验电子邮箱格式
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }


    private static long parse(String value) { // 内部辅助：解析用户 ID ，失败时为注册验证问题
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();

            return id;
        } catch (NumberFormatException invalid) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, invalid.getMessage()); // 解析失败抛出未认证异常
        }
    }

    private static UpdateUserProfileRequest defaultRequest() {
        return new UpdateUserProfileRequest("新用户", null, "zh-CN", "Asia/Shanghai", 0);
    }
}