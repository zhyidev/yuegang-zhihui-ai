package com.yuegang.zhihui.common.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;

public final class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException() {
        super(ErrorCode.PERMISSION_DENIED);
    }

    public PermissionDeniedException(String message) {
        super(ErrorCode.PERMISSION_DENIED, message);
    }
}
