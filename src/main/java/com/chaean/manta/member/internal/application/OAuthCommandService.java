package com.chaean.manta.member.internal.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.adapter.OAuthProviderClient;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthAuthorization;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthCommandService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final List<OAuthProviderClient> providerClients;
	private final MemberCommandService memberCommandService;
	private final AuthTokenCommandService authTokenCommandService;

	public OAuthAuthorization startOAuthAuthorization(OAuthProvider provider) {
		OAuthProviderClient providerClient = providerClient(provider);

		String state = randomValue(32);
		String codeVerifier = randomValue(48);

		String authorizationUrl = providerClient.createAuthorizationUrl(
			state, codeChallenge(codeVerifier), providerClient.redirectUri());

		return OAuthAuthorization.of(authorizationUrl, state, codeVerifier);
	}

	public AuthTokenPair completeOAuthCallback(
		OAuthProvider provider,
		String authorizationCode,
		String state,
		String expectedState,
		String codeVerifier,
		Instant now) {
		if (authorizationCode == null || authorizationCode.isBlank() || codeVerifier == null || codeVerifier.isBlank()
			|| !matchesState(state, expectedState)) {
			throw BusinessException.of(ErrorCode.AUTH_STATE_INVALID);
		}

		OAuthProviderClient providerClient = providerClient(provider);
		OAuthProfile profile = providerClient.exchangeAuthorizationCode(
			authorizationCode, codeVerifier, providerClient.redirectUri());

		Member member = memberCommandService.findOrRegisterOAuthMember(provider, profile, now);

		return authTokenCommandService.issueAccessAndRefreshTokenPair(member.getId(), now);
	}

	private OAuthProviderClient providerClient(OAuthProvider provider) {
		return providerClients.stream()
			.filter(client -> client.provider() == provider)
			.findFirst()
			.orElseThrow(() -> BusinessException.of(ErrorCode.AUTH_PROVIDER_NOT_SUPPORTED));
	}

	private boolean matchesState(String state, String expectedState) {
		if (state == null || state.isBlank() || expectedState == null || expectedState.isBlank()) {
			return false;
		}

		return MessageDigest.isEqual(
			state.getBytes(StandardCharsets.US_ASCII), expectedState.getBytes(StandardCharsets.US_ASCII));
	}

	private String codeChallenge(String verifier) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(verifier));
	}

	private byte[] digest(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
		}
	}

	private String randomValue(int byteLength) {
		byte[] bytes = new byte[byteLength];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
