package com.chaean.manta.member.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.application.model.SignupContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupContextCodecTest {

	private static final Instant ISSUED_AT = Instant.parse("2026-08-31T00:00:00Z");

	@Test
	@DisplayName("signup context를 암호화해 다시 복호화한다")
	void roundTripsSignupContext() {
		// given
		SignupContextCodec codec = new SignupContextCodec(properties());
		SignupContext context = new SignupContext(OAuthProvider.GOOGLE, "provider-subject", "user@example.com",
			ISSUED_AT, ISSUED_AT.plus(Duration.ofMinutes(10)));

		// when
		String encoded = codec.encode(context);

		// then
		assertThat(codec.decode(encoded, ISSUED_AT.plusSeconds(1))).isEqualTo(context);
	}

	@Test
	@DisplayName("만료된 signup context는 거부한다")
	void rejectsExpiredContext() {
		// given
		SignupContextCodec codec = new SignupContextCodec(properties());
		SignupContext context = new SignupContext(OAuthProvider.KAKAO, "subject", "user@example.com", ISSUED_AT,
			ISSUED_AT.plus(Duration.ofMinutes(10)));

		// when & then
		assertThatThrownBy(() -> codec.decode(codec.encode(context), ISSUED_AT.plusSeconds(601)))
			.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("변조된 signup context는 거부한다")
	void rejectsTamperedContext() {
		// given
		SignupContextCodec codec = new SignupContextCodec(properties());
		SignupContext context = new SignupContext(OAuthProvider.NAVER, "subject", "user@example.com", ISSUED_AT,
			ISSUED_AT.plus(Duration.ofMinutes(10)));
		String encoded = codec.encode(context);
		int tamperIndex = encoded.length() / 2;
		char tamperedCharacter = encoded.charAt(tamperIndex) == 'A' ? 'B' : 'A';
		String tampered = encoded.substring(0, tamperIndex) + tamperedCharacter + encoded.substring(tamperIndex + 1);

		// when & then
		assertThatThrownBy(() -> codec.decode(tampered, ISSUED_AT))
			.isInstanceOf(BusinessException.class);
	}

	private AuthProperties properties() {
		return new AuthProperties("issuer", "audience", "jwt-secret", Duration.ofMinutes(30), Duration.ofDays(30),
			"http://localhost:3000/auth/callback", "http://localhost:3000/signup/terms",
			"http://localhost:3000/auth/error", "Lax");
	}
}
