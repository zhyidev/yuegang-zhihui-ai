package com.yuegang.zhihui.user.api;

public record PositionView( // 定义公共记录类，岗位信息展示 DTO
                            String id, // 属性：岗位唯一主键ID
                            String code, // 属性：岗位业务编码
                            String name, // 属性：岗位名称
                            String description, // 属性：岗位职能描述
                            boolean enabled, // 属性：状态（启用/禁用）
                            long version // 属性：乐观锁版本号
) {
} // 类定义结束
