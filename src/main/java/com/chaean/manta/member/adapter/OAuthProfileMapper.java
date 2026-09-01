package com.chaean.manta.member.adapter;

import java.util.Map;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

public final class OAuthProfileMapper {

	private OAuthProfileMapper() {
	}

	public static OAuthProfile from(OAuthProvider provider, Map<String, Object> attributes) {
		try {
			return switch (provider) {
				case GOOGLE -> google(attributes);
				case KAKAO -> kakao(attributes);
				case NAVER -> naver(attributes);
			};
		} catch (BusinessException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw BusinessException.of(ErrorCode.AUTH_PROFILE_INVALID);
		}
	}

	private static OAuthProfile google(Map<String, Object> attributes) {
		if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
			throw invalidProfile();
		}
		return new OAuthProfile(OAuthProvider.GOOGLE, requiredString(attributes, "sub"),
			requiredString(attributes, "email"));
	}

	private static OAuthProfile kakao(Map<String, Object> attributes) {
		Map<?, ?> account = requiredMap(attributes, "kakao_account");
		if (!Boolean.TRUE.equals(account.get("is_email_valid"))
			|| !Boolean.TRUE.equals(account.get("is_email_verified"))) {
			throw invalidProfile();
		}
		return new OAuthProfile(OAuthProvider.KAKAO, requiredSubject(attributes.get("id")),
			requiredString(account, "email"));
	}

	private static OAuthProfile naver(Map<String, Object> attributes) {
		Map<?, ?> response = requiredMap(attributes, "response");
		return new OAuthProfile(OAuthProvider.NAVER, requiredString(response, "id"),
			requiredString(response, "email"));
	}

	private static String requiredString(Map<?, ?> values, String key) {
		Object value = values.get(key);
		if (!(value instanceof String stringValue) || stringValue.isBlank()) {
			throw invalidProfile();
		}
		return stringValue;
	}

	private static String requiredSubject(Object value) {
		if (value instanceof Number number) {
			return number.toString();
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw invalidProfile();
	}

	private static Map<?, ?> requiredMap(Map<String, Object> values, String key) {
		Object value = values.get(key);
		if (!(value instanceof Map<?, ?> map)) {
			throw invalidProfile();
		}
		return map;
	}

	private static BusinessException invalidProfile() {
		return BusinessException.of(ErrorCode.AUTH_PROFILE_INVALID);
	}
}
