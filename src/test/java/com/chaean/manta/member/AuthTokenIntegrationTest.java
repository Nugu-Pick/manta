package com.chaean.manta.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.member.adapter.OAuth2ProfileUser;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;
import com.chaean.manta.support.PostgresIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(AuthTokenIntegrationTest.RealJwtDecoderConfiguration.class)
class AuthTokenIntegrationTest extends PostgresIntegrationTest {

	private static final String AUTHORIZATION_REQUEST_SESSION_ATTRIBUTE =
		"org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository."
			+ "AUTHORIZATION_REQUEST";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberIdentityRepository memberIdentityRepository;

	@MockitoBean
	private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

	@MockitoBean
	private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;

	@Test
	@DisplayName("OAuth provider 로그인 시작 시 HTTP Session에 authorization request를 저장한다")
	void startsGoogleOAuth() throws Exception {
		MvcResult startResult = mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andExpect(mvcResult -> assertThat(mvcResult.getRequest().getSession(false)).isNotNull())
			.andExpect(mvcResult -> assertThat(mvcResult.getResponse().getHeader("Location"))
				.startsWith("http://localhost/google/authorize")
				.contains("client_id=test-google-client", "redirect_uri=", "state=", "code_challenge=",
					"code_challenge_method=S256"))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/oauth-start",
				ResourceSnippetParameters.builder()
					.summary("OAuth provider 로그인을 시작한다.")
					.description("OAuth authorization request를 HTTP Session에 저장한 뒤 provider "
						+ "authorization endpoint로 이동한다."),
				pathParameters(parameterWithName("provider").description("OAuth provider 이름")),
				responseHeaders(
					headerWithName("Location").description("OAuth provider authorization endpoint"))
			))
			.andReturn();

		assertThat(startResult.getRequest().getSession(false)).isInstanceOf(MockHttpSession.class);
	}

	@Test
	@DisplayName("OAuth callback에 authorization request가 없으면 고정 오류 화면으로 이동한다")
	void rejectsCallbackWithoutAuthorizationRequest() throws Exception {
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", "callback-state"))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.isEqualTo("http://localhost:3000/auth/error?error=oauth_failed"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.noneMatch(cookie -> cookie.startsWith("manta_oauth_authorization_request=")));
	}

	@Test
	@DisplayName("OAuth callback state가 authorization request와 다르면 재사용할 수 없다")
	void rejectsMismatchedState() throws Exception {
		// given
		MvcResult start = mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andReturn();
		MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);

		// when & then
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", "different-state")
				.session(session))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.isEqualTo("http://localhost:3000/auth/error?error=oauth_failed"))
			.andExpect(result -> assertThat(session.isInvalid()).isTrue());
	}

	@Test
	@DisplayName("신규 OAuth callback은 회원을 만들지 않고 signup context를 발급한 뒤 "
		+ "Session을 폐기한다")
	void completesNewOAuthCallbackWithoutCreatingMember() throws Exception {
		// given
		when(accessTokenResponseClient.getTokenResponse(any())).thenReturn(OAuth2AccessTokenResponse.withToken(
			"provider-access-token")
			.tokenType(OAuth2AccessToken.TokenType.BEARER)
			.expiresIn(300)
			.build());
		OAuth2User providerUser = new DefaultOAuth2User(List.of(), Map.of(
			"sub", "new-google-subject",
			"email", "new-google@example.com",
			"email_verified", true), "sub");
		when(oauth2UserService.loadUser(any())).thenReturn(new OAuth2ProfileUser(providerUser,
			new OAuthProfile(OAuthProvider.GOOGLE, "new-google-subject", "new-google@example.com")));

		MvcResult start = mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andReturn();
		MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
		String encodedState = UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl())
			.build()
			.getQueryParams()
			.getFirst("state");
		String state = URLDecoder.decode(encodedState, StandardCharsets.UTF_8);
		OAuth2AuthorizationRequest authorizationRequest = (OAuth2AuthorizationRequest) session.getAttribute(
			AUTHORIZATION_REQUEST_SESSION_ATTRIBUTE);
		assertThat(authorizationRequest.getState()).isEqualTo(state);

		// when & then
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", state)
				.session(session))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.isEqualTo("http://localhost:3000/signup/terms"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> cookie.startsWith("manta_signup_context=") && cookie.contains("Max-Age=600")))
			.andExpect(result -> assertThat(session.isInvalid()).isTrue())
			.andExpect(result -> assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM orca.member_identity WHERE provider = ? AND provider_subject = ?", Integer.class,
				"google", "new-google-subject")).isZero())
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/oauth-callback",
				ResourceSnippetParameters.builder()
					.summary("OAuth callback 결과를 고정 프런트 URI로 전달한다.")
					.description("신규 identity는 signup context cookie를, 기존 identity는 Refresh Token cookie를 "
						+ "발급합니다. 실패 시 오류 URI로 이동하며 모든 경우 OAuth HTTP Session을 폐기합니다."),
				pathParameters(parameterWithName("provider").description("OAuth provider 이름")),
				queryParameters(
					parameterWithName("code").description("provider authorization code"),
					parameterWithName("state").description("OAuth state")),
				responseHeaders(
					headerWithName("Location").description("상태에 따른 고정 프런트 callback URI"),
					headerWithName("Set-Cookie").description("상태에 따른 signup context 또는 Refresh Token cookie"))
			));
	}

	@Test
	@DisplayName("기존 OAuth callback은 Refresh Token을 발급하고 로그인 화면으로 이동한 뒤 "
		+ "Session을 폐기한다")
	void completesExistingOAuthCallbackWithRefreshToken() throws Exception {
		// given
		Instant now = Instant.now();
		Member member = memberRepository.saveAndFlush(
			Member.register("existing-google@example.com", "existing-google", null, null));
		memberIdentityRepository.saveAndFlush(
			MemberIdentity.create(member.getId(), "google", "existing-google-subject", now));
		when(accessTokenResponseClient.getTokenResponse(any())).thenReturn(OAuth2AccessTokenResponse.withToken(
			"provider-access-token")
			.tokenType(OAuth2AccessToken.TokenType.BEARER)
			.expiresIn(300)
			.build());
		OAuth2User providerUser = new DefaultOAuth2User(List.of(), Map.of(
			"sub", "existing-google-subject",
			"email", "existing-google@example.com",
			"email_verified", true), "sub");
		when(oauth2UserService.loadUser(any())).thenReturn(new OAuth2ProfileUser(providerUser,
			new OAuthProfile(OAuthProvider.GOOGLE, "existing-google-subject", "existing-google@example.com")));

		MvcResult start = mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andReturn();
		MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
		String encodedState = UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl())
			.build()
			.getQueryParams()
			.getFirst("state");
		String state = URLDecoder.decode(encodedState, StandardCharsets.UTF_8);

		// when & then
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", state)
				.session(session))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.isEqualTo("http://localhost:3000/auth/callback"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> cookie.startsWith("manta_refresh_token=") && cookie.contains("HttpOnly")))
			.andExpect(result -> assertThat(session.isInvalid()).isTrue())
			.andExpect(result -> assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM orca.refresh_token WHERE member_id = ?", Integer.class, member.getId()))
				.isEqualTo(1));
	}

	@TestConfiguration
	static class RealJwtDecoderConfiguration {

		@Bean
		JwtDecoder jwtDecoder(AuthProperties properties) {
			return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
				properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		}
	}
}
