package com.chaean.manta.member.adapter;

import com.chaean.manta.member.adapter.google.GoogleOAuthProviderClient;
import com.chaean.manta.member.adapter.kakao.KakaoOAuthProviderClient;
import com.chaean.manta.member.config.OAuthProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthProviderClientConfig {

	@Bean
	OAuthProviderClient googleOAuthProviderClient(OAuthProperties properties, RestClient restClient) {
		return new GoogleOAuthProviderClient(properties.google(), restClient);
	}

	@Bean
	OAuthProviderClient kakaoOAuthProviderClient(OAuthProperties properties, RestClient restClient) {
		return new KakaoOAuthProviderClient(properties.kakao(), restClient);
	}
}
