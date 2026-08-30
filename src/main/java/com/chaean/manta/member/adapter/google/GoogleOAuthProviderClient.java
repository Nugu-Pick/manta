package com.chaean.manta.member.adapter.google;

import java.util.Map;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.config.OAuthProperties;
import com.chaean.manta.member.adapter.OAuthProviderClient;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

public final class GoogleOAuthProviderClient implements OAuthProviderClient {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
		new ParameterizedTypeReference<>() {
		};

	private final OAuthProperties.Provider properties;
	private final RestClient restClient;

	public GoogleOAuthProviderClient(OAuthProperties.Provider properties, RestClient restClient) {
		this.properties = properties;
		this.restClient = restClient;
	}

	@Override
	public OAuthProvider provider() {
		return OAuthProvider.GOOGLE;
	}

	@Override
	public String redirectUri() {
		return properties.redirectUri();
	}

	@Override
	public String createAuthorizationUrl(String state, String codeChallenge, String redirectUri) {
		return UriComponentsBuilder.fromUriString(properties.authorizationUri())
			.queryParam("client_id", properties.clientId())
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "openid email profile")
			.queryParam("state", state)
			.queryParam("code_challenge", codeChallenge)
			.queryParam("code_challenge_method", "S256")
			.encode()
			.build()
			.toUriString();
	}

	@Override
	public OAuthProfile exchangeAuthorizationCode(
		String authorizationCode,
		String codeVerifier,
		String redirectUri) {
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("client_id", properties.clientId());
			form.add("client_secret", properties.clientSecret());
			form.add("code", authorizationCode);
			form.add("code_verifier", codeVerifier);
			form.add("redirect_uri", redirectUri);
			form.add("grant_type", "authorization_code");

			Map<String, Object> token = postForm(properties.tokenUri(), form);
			String accessToken = required(token, "access_token");

			Map<String, Object> profile = getProfile(properties.userInfoUri(), accessToken);

			return new OAuthProfile(required(profile, "sub"), verifiedEmail(profile));
		} catch (RestClientException exception) {
			throw BusinessException.of(ErrorCode.AUTH_PROVIDER_REQUEST_FAILED);
		} catch (IllegalArgumentException exception) {
			throw BusinessException.of(ErrorCode.AUTH_PROFILE_INVALID);
		}
	}

	private Map<String, Object> postForm(String uri, MultiValueMap<String, String> form) {
		Map<String, Object> response = restClient.post()
			.uri(uri)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(MAP_TYPE);

		return response == null ? Map.of() : response;
	}

	private Map<String, Object> getProfile(String uri, String accessToken) {
		Map<String, Object> response = restClient.get()
			.uri(uri)
			.headers(headers -> headers.setBearerAuth(accessToken))
			.retrieve()
			.body(MAP_TYPE);

		return response == null ? Map.of() : response;
	}

	private String required(Map<String, Object> values, String key) {
		Object value = values.get(key);
		if (!(value instanceof String stringValue) || stringValue.isBlank()) {
			throw new IllegalArgumentException(key);
		}

		return stringValue;
	}

	private String verifiedEmail(Map<String, Object> profile) {
		if (!Boolean.TRUE.equals(profile.get("email_verified"))) {
			throw new IllegalArgumentException("email_verified");
		}

		return required(profile, "email");
	}
}
