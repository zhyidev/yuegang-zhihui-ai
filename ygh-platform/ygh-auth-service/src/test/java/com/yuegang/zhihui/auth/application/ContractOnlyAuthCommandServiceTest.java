package com.yuegang.zhihui.auth.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractOnlyAuthCommandServiceTest {

    private final ContractOnlyAuthCommandService service = new ContractOnlyAuthCommandService();

    private static void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.errorCode())
                    .isEqualTo(ErrorCode.DEPENDENCY_UNAVAILABLE));
    }

    @Test
    void everyUnimplementedUseCaseFailsClosedAsDependencyUnavailable() {
        assertUnavailable(() -> service.register(null));
        assertUnavailable(() -> service.login(null, null));
        assertUnavailable(() -> service.refresh(null));
        assertUnavailable(() -> service.logout(null, null));
        assertUnavailable(service::captcha);
        assertUnavailable(() -> service.requestPasswordReset(null));
        assertUnavailable(() -> service.confirmPasswordReset(null));
    }
}
