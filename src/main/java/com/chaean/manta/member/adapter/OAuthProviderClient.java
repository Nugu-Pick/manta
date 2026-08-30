package com.chaean.manta.member.adapter;

import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

public interface OAuthProviderClient {

	OAuthProvider provider();

	String redirectUri();

	String createAuthorizationUrl(String state, String codeChallenge, String redirectUri);

	OAuthProfile exchangeAuthorizationCode(String authorizationCode, String codeVerifier, String redirectUri);
}
