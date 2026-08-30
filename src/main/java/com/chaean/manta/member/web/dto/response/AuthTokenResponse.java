package com.chaean.manta.member.web.dto.response;

import java.time.Instant;

import com.chaean.manta.member.entity.MemberStatus;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;

public record AuthTokenResponse(String accessToken, String tokenType, Instant expiresAt, MemberStatus memberStatus) {

	public static AuthTokenResponse from(AuthTokenPair tokens) {
		return new AuthTokenResponse(tokens.accessToken(), "Bearer", tokens.accessTokenExpiresAt(),
			tokens.memberStatus());
	}
}
