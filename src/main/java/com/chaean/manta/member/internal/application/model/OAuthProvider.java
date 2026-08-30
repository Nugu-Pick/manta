package com.chaean.manta.member.internal.application.model;

import java.util.Locale;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;

public enum OAuthProvider {

	GOOGLE,
	KAKAO;

	public static OAuthProvider from(String value) {
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw BusinessException.of(ErrorCode.AUTH_PROVIDER_NOT_SUPPORTED);
		}
	}

	public String value() {
		return name().toLowerCase(Locale.ROOT);
	}
}
