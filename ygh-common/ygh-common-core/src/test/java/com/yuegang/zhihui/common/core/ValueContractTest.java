package com.yuegang.zhihui.common.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValueContractTest { // 定义对象契约测试类
    @Test
        // 标记为测试方法
    void externalIdKeepsValueBeyondScriptSafeIntegerAsText() { // 测试外部 ID 是否将超过 JS 安全整数范围的值保存为文本
        var id = ExternalId.of("9007199254740993");// 传入一个大整数（超过 2^53-1）

        assertThat(id.value()).isEqualTo("9007199254740993"); // 断言内部存储的数据正确
        assertThat(id.value()).isEqualTo("9007199254740993"); // 端元转为字符串后的表现正确

    }

    @Test
        // 标记为测试方法
    void externalIdRejectMissingOrSurroundedWhitespace() { // 测试外部 ID 是否拒绝空格输入
        assertThatThrownBy(() -> ExternalId.of(null)) // 拒绝 null
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExternalId.of(" ")) // 拒绝纯空格
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExternalId.of("user-1")) // 拒绝前后空格的字符串
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
        //标记为测试方法
    void moneyUsesExactBigDecimalScaleAndTheOnlySupportCurrency() { //测试金额是否使用精确的 Scale （小数位） 和唯一支持的货币
        var money = Money.cny(new BigDecimal("99.80")); // 创建 99.80 人民币对象

        assertThat(money.amount()).isEqualByComparingTo("99.80"); // 使用笔记断言（忽略末尾 0 差异，虽然在此处 Scale 强制为 2）
        assertThat(money.amount()).scale().isEqualTo(Money.SCALE);// 断言精确度等 -2
        assertThat(money.currency()).isEqualTo(CurrencyCode.CNY); // 断言货币为人名币
        assertThat(money.currency().code()).isEqualTo("CNY"); // 断言代码字符串中却
        assertThat(CurrencyCode.values()).containsExactly(CurrencyCode.CNY); // 断言目前系统仅支持人名币一种货币
    }

    @Test
        // 标记为测试方法
    void moneyNeverRoundsOrChangesScaleImplicitly() { // 测试金额对象绝不今昔隐式舍入或改变精度
        assertThatThrownBy(() -> Money.cny(new BigDecimal("99.8"))) // 拒绝小数位不足两位的输入
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("scale");
        assertThatThrownBy(() -> Money.cny(null)) // 拒绝空值
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
        // 标记为测试方法
    void enumCoderAreExplicitStableAndIndependentFromDisplayText() { // 测试美剧编码是否显示、稳定且独立鱼显示文本
        assertThat(CurrencyCode.CNY.code()).isEqualTo("CNY"); // 代码为 CNY
        assertThat(CurrencyCode.CNY.displayName()).isEqualTo("人民币"); // 显示文本为中文
        assertThat(CurrencyCode.CNY.code()).isNotEqualTo(CurrencyCode.CNY.displayName()); // 确保代码和显示文本没有耦合

    }
}
