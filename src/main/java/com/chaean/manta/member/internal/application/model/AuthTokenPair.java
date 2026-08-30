package com.chaean.manta.member.internal.application.model;

import java.time.Instant;

import com.chaean.manta.member.entity.MemberStatus;

public record AuthTokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
	Instant refreshTokenExpiresAt, MemberStatus memberStatus) {

	public static AuthTokenPair of(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
		Instant refreshTokenExpiresAt, MemberStatus memberStatus) {
		return new AuthTokenPair(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, memberStatus);
	}
}
