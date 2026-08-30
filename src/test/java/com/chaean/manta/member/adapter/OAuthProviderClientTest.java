package com.chaean.manta.member.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chaean.manta.member.adapter.google.GoogleOAuthProviderClient;
import com.chaean.manta.member.adapter.kakao.KakaoOAuthProviderClient;
import com.chaean.manta.member.config.OAuthProperties;
import com.chaean.manta.member.internal.application.model.OAuthProfile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OAuthProviderClientTest {

	@Test
	@DisplayName("Google adapter는 authorization code와 PKCE verifier로 provider profile을 조회한다")
	void exchangesGoogleAuthorizationCode() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		GoogleOAuthProviderClient client = new GoogleOAuthProviderClient(
			provider("http://google/token", "http://google/profile"), builder.build());
		server.expect(requestTo("http://google/token"))
			.andExpect(method(POST))
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
			.andExpect(content().string(org.hamcrest.Matchers.allOf(
				org.hamcrest.Matchers.containsString("code=provider-code"),
				org.hamcrest.Matchers.containsString("code_verifier=pkce-verifier"))))
			.andRespond(withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://google/profile"))
			.andExpect(method(GET))
			.andExpect(header("Authorization", "Bearer google-access-token"))
			.andRespond(withSuccess("{\"sub\":\"google-subject\",\"email\":\"user@example.com\",\"email_verified\":true}",
				MediaType.APPLICATION_JSON));

		// when
		OAuthProfile profile = client.exchangeAuthorizationCode(
			"provider-code", "pkce-verifier", "http://localhost/callback");

		// then
		assertThat(profile).isEqualTo(new OAuthProfile("google-subject", "user@example.com"));
		server.verify();
	}

	@Test
	@DisplayName("Kakao adapter는 숫자 provider subject와 계정 이메일을 profile로 변환한다")
	void exchangesKakaoAuthorizationCode() {
		// given
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KakaoOAuthProviderClient client = new KakaoOAuthProviderClient(
			provider("http://kakao/token", "http://kakao/profile"), builder.build());
		server.expect(requestTo("http://kakao/token"))
			.andExpect(method(POST))
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_FORM_URLENCODED))
			.andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://kakao/profile"))
			.andExpect(method(GET))
			.andExpect(header("Authorization", "Bearer kakao-access-token"))
			.andRespond(withSuccess("{\"id\":12345,\"kakao_account\":{\"email\":\"user@example.com\","
				+ "\"is_email_valid\":true,\"is_email_verified\":true}}",
				MediaType.APPLICATION_JSON));

		// when
		OAuthProfile profile = client.exchangeAuthorizationCode(
			"provider-code", "pkce-verifier", "http://localhost/callback");

		// then
		assertThat(profile).isEqualTo(new OAuthProfile("12345", "user@example.com"));
		server.verify();
	}

	private OAuthProperties.Provider provider(String tokenUri, String userInfoUri) {
		return new OAuthProperties.Provider("client-id", "client-secret", "http://provider/authorize", tokenUri,
			userInfoUri, "http://localhost/callback");
	}
}
