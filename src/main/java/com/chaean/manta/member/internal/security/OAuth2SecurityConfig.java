package com.chaean.manta.member.internal.security;

import com.chaean.manta.member.adapter.OAuth2UserProfileService;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.OAuthCommandService;
import com.chaean.manta.member.internal.application.SignupContextCodec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuth2SecurityConfig {

	@Bean
	OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
		return new OAuth2UserProfileService(new DefaultOAuth2UserService());
	}

	@Bean
	OAuth2AuthorizedClientRepository authorizedClientRepository() {
		return new HttpSessionOAuth2AuthorizedClientRepository();
	}

	@Bean
	OAuth2SuccessHandler oauth2SuccessHandler(
		OAuthCommandService oauthCommandService,
		SignupContextCodec signupContextCodec,
		AuthProperties properties,
		OAuth2FailureHandler failureHandler) {
		return new OAuth2SuccessHandler(oauthCommandService, signupContextCodec, properties, failureHandler);
	}

	@Bean
	OAuth2FailureHandler oauth2FailureHandler(AuthProperties properties) {
		return new OAuth2FailureHandler(properties);
	}

	@Bean
	@Order(1)
	SecurityFilterChain oauth2SecurityFilterChain(
		HttpSecurity http,
		OAuth2AuthorizedClientRepository authorizedClientRepository,
		OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
		OAuth2SuccessHandler successHandler,
		OAuth2FailureHandler failureHandler) throws Exception {
		http.securityMatcher("/api/v1/auth/oauth/**")
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
			.oauth2Login(oauth2 -> oauth2
				.authorizationEndpoint(endpoint -> endpoint
					.baseUri("/api/v1/auth/oauth"))
				.redirectionEndpoint(endpoint -> endpoint
					.baseUri("/api/v1/auth/oauth/*/callback"))
				.userInfoEndpoint(endpoint -> endpoint.userService(oauth2UserService))
				.authorizedClientRepository(authorizedClientRepository)
				.successHandler(successHandler)
				.failureHandler(failureHandler));
		return http.build();
	}
}
