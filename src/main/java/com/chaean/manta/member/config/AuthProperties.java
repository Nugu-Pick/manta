package com.chaean.manta.member.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("manta.auth")
public record AuthProperties(
	String issuer,
	String audience,
	String jwtSecret,
	Duration accessTokenTtl,
	Duration refreshTokenTtl,
	String frontendLoginCallbackUri,
	String frontendSignupCallbackUri,
	String frontendErrorCallbackUri) {

	public static final String REFRESH_COOKIE_NAME = "manta_refresh_token";
	public static final String SIGNUP_CONTEXT_COOKIE_NAME = "manta_signup_context";
}
