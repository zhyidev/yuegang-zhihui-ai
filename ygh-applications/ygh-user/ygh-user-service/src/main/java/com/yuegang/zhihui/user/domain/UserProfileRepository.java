package com.yuegang.zhihui.user.domain;

import com.yuegang.zhihui.user.api.UpdateUserProfileRequest;
import com.yuegang.zhihui.user.api.UserProfileView;

import java.util.Optional;

/**
 * 用户个人资料仓储接口，定义了用户信息访问与更新的领域契约
 */
public interface UserProfileRepository {

    /**
     * 根据用户 ID 获取个人资料视图
     */
    Optional<UserProfileView> findByUserId(long userId); //声明方法:通过唯一标识检索个人详情，可能返回空

    /**
     * 保存或更新用户的个人资料
     */
    Optional<UserProfileView> save(long userId, UpdateUserProfileRequest request); // 声明方法：持久化用户资料变更，返回更新后的可选结果视图

}// 接口定义结束
