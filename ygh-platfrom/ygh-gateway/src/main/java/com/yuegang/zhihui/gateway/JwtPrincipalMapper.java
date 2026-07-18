package com.yuegang.zhihui.gateway;

import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** jwt字段解析器*/
@Component
public class JwtPrincipalMapper {

    private  static  final int MAX_AUTHORITIES = 128; // 最大角色限制
    private  static  final int MAX_ENCODE_AUTHORITIES = 4069; // 编码后长度限制
    private  static  final Pattern SAFE_SUBJECT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9->:-]{0,127}");
    private  static  final Pattern SAFE_AUTHORITY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9->:-]{0,127}");

    CurrentUserPrincipal map(Jwt jwt){
        if(jwt == null) throw new BadCredentialsException("JWT must not be null");
        String subject = jwt.getSubject();
        if(subject == null || !SAFE_SUBJECT.matcher(subject).matches()){
            throw new BadCredentialsException("JWT subject is missing or unsafe");
        }

        return new CurrentUserPrincipal(
                subject,
                claimSet(jwt,"roles"),
                claimSet(jwt,"permissions"));

    }

    private  static Set<String> claimSet(Jwt jwt, String claimName){
        Object claim = jwt.getClaim(claimName);
        if(claim == null) return Set.of();
        if(!(claim instanceof Collection<?> values)) throw new BadCredentialsException("JWT claim must be array");
        if(values.size() > MAX_AUTHORITIES) throw new BadCredentialsException("JWT too many values");


        var result = new LinkedHashSet<String>(values.size());
        int encodeLength = 0;
        for (Object value : values) {
            if (!(value instanceof String authority) || !SAFE_AUTHORITY.matcher(authority).matches()) {
                throw new BadCredentialsException("JWT claim contains unsafe value");
            }
            if (result.add(authority)) {
                encodeLength += authority.length() + (result.size() == 1 ? 0 : 1);

            }
        }
        if (encodeLength > MAX_ENCODE_AUTHORITIES) throw new BadCredentialsException("JWT claim  too  long");
        return Set.copyOf(result);

    }

}
