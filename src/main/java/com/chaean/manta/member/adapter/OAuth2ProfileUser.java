package com.chaean.manta.member.adapter;

import java.util.Collection;
import java.util.Map;

import com.chaean.manta.member.internal.application.model.OAuthProfile;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class OAuth2ProfileUser implements OAuth2User {

	private final OAuth2User delegate;
	private final OAuthProfile profile;

	public OAuth2ProfileUser(OAuth2User delegate, OAuthProfile profile) {
		this.delegate = delegate;
		this.profile = profile;
	}

	public OAuthProfile profile() {
		return profile;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return delegate.getAuthorities();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}
}
