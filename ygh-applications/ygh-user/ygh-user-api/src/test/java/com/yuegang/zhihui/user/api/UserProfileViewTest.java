package com.yuegang.zhihui.user.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileViewTest {
    @Test
    void keepsIdsAsStringsAndRejectsUnsafeValues() {
        assertThat(new UserProfileView("42", "Alice", null, 0).userId()).isEqualTo("42");
        assertThatThrownBy(() -> new UserProfileView("0", "Alice", null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
