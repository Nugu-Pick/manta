package com.chaean.manta.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("manta.oauth")
public record OAuthProperties(Provider google, Provider kakao) {

	public record Provider(String clientId, String clientSecret, String authorizationUri, String tokenUri,
		String userInfoUri, String redirectUri) {
	}
}
