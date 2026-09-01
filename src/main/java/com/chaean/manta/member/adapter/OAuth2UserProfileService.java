package com.chaean.manta.member.adapter;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class OAuth2UserProfileService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

	public OAuth2UserProfileService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
		this.delegate = delegate;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) {
		OAuth2User user = delegate.loadUser(userRequest);
		try {
			OAuthProvider provider = OAuthProvider.from(userRequest.getClientRegistration().getRegistrationId());
			return new OAuth2ProfileUser(user, OAuthProfileMapper.from(provider, user.getAttributes()));
		} catch (BusinessException exception) {
			throw new OAuth2AuthenticationException(
				new OAuth2Error("invalid_provider_profile"), exception.getMessage(), exception);
		}
	}
}
