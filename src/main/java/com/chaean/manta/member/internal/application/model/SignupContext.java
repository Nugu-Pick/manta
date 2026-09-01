package com.chaean.manta.member.internal.application.model;

import java.time.Instant;

public record SignupContext(
	OAuthProvider provider,
	String providerSubject,
	String email,
	Instant issuedAt,
	Instant expiresAt) {

	public SignupContext {
		if (provider == null || providerSubject == null || providerSubject.isBlank()
			|| email == null || email.isBlank() || issuedAt == null || expiresAt == null
			|| !expiresAt.isAfter(issuedAt)) {
			throw new IllegalArgumentException("signup context 값이 올바르지 않습니다.");
		}
	}
}
