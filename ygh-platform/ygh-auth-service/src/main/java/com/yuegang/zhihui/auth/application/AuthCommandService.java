package com.yuegang.zhihui.auth.application;

import com.yuegang.zhihui.auth.api.dto.*;

/**
 * Application boundary implemented incrementally by BE-0322 through BE-0325.
 */
public interface AuthCommandService {
    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse login(LoginRequest request, LoginSecurityContext context);

    TokenResponse refresh(RefreshTokenRequest request);

    OperationResponse logout(LogoutRequest request, String authorization);

    CaptchaResponse captcha();

    PasswordResetRequestedResponse requestPasswordReset(PasswordResetRequest request);

    OperationResponse confirmPasswordReset(PasswordResetConfirmRequest request);
}
