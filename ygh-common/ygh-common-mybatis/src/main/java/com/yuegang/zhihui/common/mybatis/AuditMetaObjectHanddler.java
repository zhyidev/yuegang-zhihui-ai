package com.yuegang.zhihui.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class AuditMetaObjectHanddler implements MetaObjectHandler {

    private static final String CREATED_BY = "createdBy";
    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_BY = "updatedBy";
    private static final String UPDATED_AT = "updatedAt";

    private final AuditorProvider audiorProvider;
    private final Clock clock;

    public AuditMetaObjectHanddler(AuditorProvider auditorProvider,Clock clock){
        this.audiorProvider = Objects.requireNonNull(auditorProvider, "auditorProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }


    @Override
    public void insertFill(MetaObject metaObject) {
        Objects.requireNonNull(metaObject, "metaObject must not be null");
        if (!hasAnyAuditProperty(metaObject)){
            return;
        }

        String auditor = resolveAuditor();
        Instant now = clock.instant();
        setIfPresent(metaObject,CREATED_BY,auditor);
        setIfPresent(metaObject,CREATED_AT,now);
        setIfPresent(metaObject,UPDATED_BY,auditor);
        setIfPresent(metaObject,UPDATED_AT,now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Objects.requireNonNull(metaObject, "metaObject must not be null");
        if (!hasAnyAuditProperty(metaObject)){
            return;
        }

        String auditor = resolveAuditor();
        Instant now = clock.instant();
        setIfPresent(metaObject,UPDATED_BY,auditor);
        setIfPresent(metaObject,UPDATED_AT,now);
    }

    private  String resolveAuditor(){
        String auditor = audiorProvider.currentAuditor();
        if (auditor == null || auditor.isBlank()){
            throw new IllegalArgumentException("auditor must not be null or blank");
        }
        return auditor;
    }


    private static boolean hasAnyAuditProperty(MetaObject metaObject){
        return metaObject.hasSetter(CREATED_BY)
                || metaObject.hasSetter(CREATED_AT)
                || metaObject.hasSetter(UPDATED_BY)
                || metaObject.hasSetter(UPDATED_AT);
    }

    private static void setIfPresent(MetaObject metaObject,String property,Object value){
        if (metaObject.hasSetter(property)){
            metaObject.setValue(property,value);
        }
    }

}
