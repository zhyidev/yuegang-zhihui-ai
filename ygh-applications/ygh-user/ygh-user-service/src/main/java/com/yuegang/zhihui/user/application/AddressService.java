package com.yuegang.zhihui.user.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.user.api.AddressOperationResponse;
import com.yuegang.zhihui.user.api.AddressView;
import com.yuegang.zhihui.user.api.CreateAddressRequest;
import com.yuegang.zhihui.user.api.UpdateAddressRequest;
import com.yuegang.zhihui.user.domain.AddressRepository;

import java.util.List;

public final class AddressService { // 定义地址业务服务类
    private final AddressRepository repository; // 地址仓储成员变量
    private final UserIdGenerator ids; // ID生成器成员变量

    public AddressService(AddressRepository repository, UserIdGenerator ids) { // 构造函数注入依赖
        this.repository = repository;
        this.ids = ids;
    }

    private static BusinessException conflict() {
        return new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 统一定义冲突异常
    }

    private static long parse(String value) { // 内部辅助方法：解析字符串 ID 为 Long
        try {
            long id = Long.parseLong(value); // 转换数字
            if (id <= 0) throw new NumberFormatException(); // 校验 ID 必须大户0
            return id; // 返回有效 ID
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        } // 转换失败抛出验证错误
    }

    public List<AddressView> list(String userId) {
        return repository.findAll(parse(userId));
    } // 获取指定用户的所有地址列表

    public AddressView create(String userId, CreateAddressRequest request) { // 创建新地址
        return repository.create(ids.nextId(), parse(userId), request); // 生成新ID并调用仓储层保存
    }

    public AddressView update(String userId, String addressId, UpdateAddressRequest request) { // 更新现有地址
        return repository.update(parse(addressId), parse(userId), request).orElseThrow(AddressService::conflict); // 更新失败抛出业务冲突异常
    }

    public AddressOperationResponse delete(String userId, String addressId, long version) { // 删除地址
        if (!repository.delete(parse(addressId), parse(userId), version)) throw conflict(); // 乐观锁删除失败则抛出异常
        return new AddressOperationResponse(true); // 返回操作成功响应
    }

    public AddressView makeDefault(String userId, String addressId, long version) { // 设置为默认地址
        return repository.makeDefault(parse(addressId), parse(userId), version).orElseThrow(AddressService::conflict); // 操作失败抛出异常
    }
}