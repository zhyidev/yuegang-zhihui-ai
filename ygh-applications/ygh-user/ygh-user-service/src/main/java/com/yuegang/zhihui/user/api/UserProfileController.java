package com.yuegang.zhihui.user.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.user.application.UserProfileService;
import com.yuegang.zhihui.user.security.TrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 该类负责管理当前登录用户的个人资料
 */
@RestController // 标识为控制器
@RequestMapping("/api/v1/users/me") // 设置"关于我"即个人资料的路径
public final class UserProfileController { // 定义用户资料控制器类
    private final UserProfileService profiles; // 用户资料业务逻辑组件
    private final TrustedUserContextResolver users; // 用户身份解析组件

    public UserProfileController(UserProfileService profiles, TrustedUserContextResolver users) { // 构造函数注入
        this.profiles = profiles;
        this.users = users;
    }

    @GetMapping
    public ApiResponse<UserProfileView> get(HttpServletRequest request) { // 获取当前登录用户的个人资料
        return ApiResponse.success(profiles.get(users.resolve(request).userId()), TraceIdResolver.resolve(request)); // 解析用户 ID 并查询
    }

    @PutMapping
    public ApiResponse<UserProfileView> update(@Valid @RequestBody UpdateUserProfileRequest body, HttpServletRequest request) { // 更新当前登录用户的个人资料
        return ApiResponse.success(profiles.update(users.resolve(request).userId(), body), TraceIdResolver.resolve(request)); // 解析用户 ID 并保存
    }


}
