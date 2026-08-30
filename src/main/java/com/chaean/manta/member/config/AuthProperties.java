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
	String refreshCookieName,
	String oauthStateCookieName,
	String oauthVerifierCookieName,
	String frontendCallbackUri,
	Duration oauthAuthorizationTtl) {
}
