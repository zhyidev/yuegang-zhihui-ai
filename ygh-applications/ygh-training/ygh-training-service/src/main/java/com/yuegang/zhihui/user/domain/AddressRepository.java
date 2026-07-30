package com.yuegang.zhihui.user.domain;

import com.yuegang.zhihui.user.api.AddressView;
import com.yuegang.zhihui.user.api.CreateAddressRequest;
import com.yuegang.zhihui.user.api.UpdateAddressRequest;

import java.util.List;
import java.util.Optional;

/**
 * 地址仓储接口，定义了对用户收货地址进行持久化操作的领域契约
 */
public interface AddressRepository {

    /**
     * 查询指定用户的所有收货地址
     */
    List<AddressView> findAll(long userId); // 声明方法:根据用户唯一标识查询其名下所有地址列表

    /**
     * 为用户创建新收货地址
     */
    AddressView create(long id, long userId, CreateAddressRequest request); // 声明方法:传入预生成ID、用户ID及创建请求，返回创建成功的视图

    /**
     * 更新现有的收货地址信息
     */
    Optional<AddressView> update(long id, long userId, UpdateAddressRequest request); // 声明方法：更新指定地址，使用optional包装以处理地址不存在

    /**
     * 删除指定的收货地址
     */
    boolean delete(long id, long userId, long version); //声明方法:根据地址ID、用户ID及版本号（乐观锁）删除地址，返回操作是否成功

    /**
     * 将指定地址设为用户的默认地址
     */
    Optional<AddressView> makeDefault(long id, long userId, long version); //声明方法:置顶默认地址，包含业务逻辑状态变更，返回更新后的地址
} // 接口定义结束