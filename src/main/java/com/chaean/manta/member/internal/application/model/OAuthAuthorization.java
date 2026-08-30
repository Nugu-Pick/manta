package com.chaean.manta.member.internal.application.model;

public record OAuthAuthorization(String authorizationUrl, String state, String codeVerifier) {

	public static OAuthAuthorization of(String authorizationUrl, String state, String codeVerifier) {
		return new OAuthAuthorization(authorizationUrl, state, codeVerifier);
	}
}
