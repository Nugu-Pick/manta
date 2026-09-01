package com.chaean.manta.member.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthProfileMapperTest {

	@Test
	@DisplayName("Naver profile은 response.id와 동의한 email을 정규화한다")
	void mapsNaverProfile() {
		// given
		Map<String, Object> attributes = Map.of(
			"response", Map.of("id", "naver-subject", "email", "user@example.com"));

		// when
		OAuthProfile profile = OAuthProfileMapper.from(OAuthProvider.NAVER, attributes);

		// then
		assertThat(profile).isEqualTo(new OAuthProfile(OAuthProvider.NAVER, "naver-subject", "user@example.com"));
	}

	@Test
	@DisplayName("Google profile은 검증된 email이 없으면 거부한다")
	void rejectsUnverifiedGoogleEmail() {
		// given
		Map<String, Object> attributes = Map.of(
			"sub", "google-subject", "email", "user@example.com", "email_verified", false);

		// when & then
		assertThatThrownBy(() -> OAuthProfileMapper.from(OAuthProvider.GOOGLE, attributes))
			.isInstanceOf(BusinessException.class);
	}
}
