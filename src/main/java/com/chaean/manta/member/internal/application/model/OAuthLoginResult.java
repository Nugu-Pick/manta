package com.chaean.manta.member.internal.application.model;

public record OAuthLoginResult(AuthTokenPair tokens, SignupContext signupContext) {

	public OAuthLoginResult {
		if ((tokens == null) == (signupContext == null)) {
			throw new IllegalArgumentException("OAuth 결과는 login 또는 signup 하나여야 합니다.");
		}
	}

	public static OAuthLoginResult login(AuthTokenPair tokens) {
		return new OAuthLoginResult(tokens, null);
	}

	public static OAuthLoginResult signup(SignupContext signupContext) {
		return new OAuthLoginResult(null, signupContext);
	}

	public boolean requiresSignup() {
		return signupContext != null;
	}
}
