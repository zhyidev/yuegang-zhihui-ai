package com.yuegang.zhihui.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 256) String currentPassword,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 256) String newPassword,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 256) String confirmPassword) {
    @JsonIgnore
    @AssertTrue(message = "两次输入的新密码不一致")
    public boolean isPasswordsMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest[secrets=[REDACTED]]";
    }
}
