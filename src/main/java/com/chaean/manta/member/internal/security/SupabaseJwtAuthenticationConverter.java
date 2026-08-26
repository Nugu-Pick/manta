package com.chaean.manta.member.internal.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.member.api.MemberAuthorization;
import com.chaean.manta.member.entity.MemberRole;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class SupabaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private final MemberAuthorization memberAuthorization;

    public SupabaseJwtAuthenticationConverter(MemberAuthorization memberAuthorization) {
        this.memberAuthorization = Objects.requireNonNull(
                memberAuthorization, "memberAuthorization must not be null");
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
            throw new BadCredentialsException("Supabase JWT에 이메일이 없습니다.");
        }

        AuthenticatedMember principal = new AuthenticatedMember(subject, email, provider(jwt));
        // 관리자 권한은 JWT metadata가 아니라 Manta 회원 DB를 기준으로 부여한다.
        return new AuthenticatedMemberAuthenticationToken(principal, jwt, authorities(subject));
    }

    private Collection<? extends GrantedAuthority> authorities(String subject) {
        return memberAuthorization.findRoleBySubject(subject)
                .map(MemberRole::authority)
                .map(SimpleGrantedAuthority::new)
                .<Collection<? extends GrantedAuthority>>map(List::of)
                .orElseGet(List::of);
    }

    private String provider(Jwt jwt) {
        Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
        if (appMetadata == null) {
            return "unknown";
        }
        Object provider = appMetadata.get("provider");
        return provider instanceof String value && !value.isBlank() ? value : "unknown";
    }

    static final class AuthenticatedMemberAuthenticationToken
            extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

        private final AuthenticatedMember principal;

        private AuthenticatedMemberAuthenticationToken(AuthenticatedMember principal, Jwt token,
                Collection<? extends GrantedAuthority> authorities) {
            super(token, authorities);
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public String getName() {
            return principal.subject();
        }

        @Override
        public Map<String, Object> getTokenAttributes() {
            return getToken().getClaims();
        }
    }
}
