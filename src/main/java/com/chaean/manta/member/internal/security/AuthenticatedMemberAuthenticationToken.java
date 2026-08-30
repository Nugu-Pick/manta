package com.chaean.manta.member.internal.security;

import java.util.Collection;
import java.util.Map;

import com.chaean.manta.common.security.AuthenticatedMember;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

final class AuthenticatedMemberAuthenticationToken
	extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

	private final AuthenticatedMember principal;

	AuthenticatedMemberAuthenticationToken(AuthenticatedMember principal, Jwt token,
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
		return Long.toString(principal.memberId());
	}

	@Override
	public Map<String, Object> getTokenAttributes() {
		return getToken().getClaims();
	}
}
