package com.chaean.manta.member.internal.application.model;

public record OAuthProfile(OAuthProvider provider, String providerSubject, String email) {

	public OAuthProfile {
		if (provider == null || providerSubject == null || providerSubject.isBlank()
			|| email == null || email.isBlank()) {
			throw new IllegalArgumentException("OAuth profile 값이 올바르지 않습니다.");
		}
	}
}
