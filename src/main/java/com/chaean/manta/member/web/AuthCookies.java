package com.chaean.manta.member.web;

import java.time.Instant;

import org.springframework.http.ResponseCookie;

public final class AuthCookies {

	private AuthCookies() {
	}

	public static ResponseCookie create(String name, String value, Instant expiresAt, Instant now) {
		return cookie(name, value, Math.max(expiresAt.getEpochSecond() - now.getEpochSecond(), 0));
	}

	public static ResponseCookie clear(String name) {
		return cookie(name, "", 0);
	}

	private static ResponseCookie cookie(String name, String value, long maxAge) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/api/v1/auth")
			.maxAge(maxAge)
			.build();
	}
}
