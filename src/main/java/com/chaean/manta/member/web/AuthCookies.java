package com.chaean.manta.member.web;

import java.time.Instant;

import com.chaean.manta.member.config.AuthProperties;

import org.springframework.http.ResponseCookie;

public final class AuthCookies {

	private AuthCookies() {
	}

	public static ResponseCookie create(AuthProperties properties, String name, String value, Instant expiresAt, Instant now) {
		return cookie(properties, name, value, Math.max(expiresAt.getEpochSecond() - now.getEpochSecond(), 0));
	}

	public static ResponseCookie clear(AuthProperties properties, String name) {
		return cookie(properties, name, "", 0);
	}

	private static ResponseCookie cookie(AuthProperties properties, String name, String value, long maxAge) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(true)
			.sameSite(properties.cookieSameSite())
			.path("/api/v1/auth")
			.maxAge(maxAge)
			.build();
	}
}
