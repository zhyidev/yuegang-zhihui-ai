package com.yuegang.zhihui.common.security;

import java.security.Permission;

public final class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException(){
        super(ErrorCode.PERMISSION_DENITED);
    }

    public PermissionDeniedException(String message){
        super(ErrorCode.PERMISSION_DENTED,message);
    }
}
