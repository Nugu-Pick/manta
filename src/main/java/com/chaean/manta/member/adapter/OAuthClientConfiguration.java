package com.chaean.manta.member.adapter;

import java.util.List;

import com.chaean.manta.member.config.OAuthProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@EnableConfigurationProperties(OAuthProperties.class)
public class OAuthClientConfiguration {

	@Bean
	ClientRegistrationRepository clientRegistrationRepository(OAuthProperties properties) {
		return new InMemoryClientRegistrationRepository(
			registration("google", properties.google(), "sub", List.of("email", "profile")),
			registration("kakao", properties.kakao(), "id", List.of("account_email")),
			registration("naver", properties.naver(), "response", List.of("email")));
	}

	private ClientRegistration registration(String registrationId, OAuthProperties.Provider properties,
		String userNameAttributeName, List<String> scopes) {
		return ClientRegistration.withRegistrationId(registrationId)
			.clientId(properties.clientId())
			.clientSecret(properties.clientSecret())
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri(properties.redirectUri())
			.scope(scopes)
			.authorizationUri(properties.authorizationUri())
			.tokenUri(properties.tokenUri())
			.userInfoUri(properties.userInfoUri())
			.userInfoAuthenticationMethod(AuthenticationMethod.HEADER)
			.userNameAttributeName(userNameAttributeName)
			.clientName(registrationId)
			.build();
	}
}
