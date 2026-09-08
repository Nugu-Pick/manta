package com.chaean.manta.member.internal.security;

import java.util.Collection;
import java.util.List;

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
public final class JwtAuthenticationConverter
	implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

	private final MemberAuthorization memberAuthorization;

	public JwtAuthenticationConverter(MemberAuthorization memberAuthorization) {
		this.memberAuthorization = memberAuthorization;
	}

	@Override
	public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
		long memberId = parseMemberId(jwt.getSubject());

		AuthenticatedMember principal = new AuthenticatedMember(memberId);

		return new AuthenticatedMemberAuthenticationToken(principal, jwt, authorities(memberId));
	}

	private long parseMemberId(String subject) {
		if (subject == null || subject.isBlank()) {
			throw new BadCredentialsException("JWT에 memberId가 없습니다.");
		}

		try {
			long memberId = Long.parseLong(subject);
			if (memberId <= 0) {
				throw new BadCredentialsException("JWT의 memberId가 올바르지 않습니다.");
			}
			return memberId;
		} catch (NumberFormatException exception) {
			throw new BadCredentialsException("JWT의 memberId가 올바르지 않습니다.", exception);
		}
	}

	private Collection<? extends GrantedAuthority> authorities(long memberId) {
		MemberRole role = memberAuthorization.findRoleByMemberId(memberId)
			.orElseThrow(() -> new BadCredentialsException("인증 가능한 회원이 없습니다."));
		return List.of(new SimpleGrantedAuthority(role.authority()));
	}
}
