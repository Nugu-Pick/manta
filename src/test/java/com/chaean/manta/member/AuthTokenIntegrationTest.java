package com.chaean.manta.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.member.adapter.OAuth2ProfileUser;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.internal.application.AuthTokenCommandService;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;
import com.chaean.manta.support.PostgresIntegrationTest;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@ActiveProfiles({"test", "auth-real-jwt"})
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

	@Autowired
	private AuthTokenCommandService tokenService;

	@Autowired
	private AuthProperties properties;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private DataSource dataSource;

	@ParameterizedTest
	@ValueSource(strings = {"http://localhost:3000", "http://localhost:5173"})
	@DisplayName("로컬 개발 서버의 CORS preflight를 허용한다")
	void allowsLocalDevelopmentCorsPreflight(String origin) throws Exception {
		mockMvc.perform(options("/api/v1/legal-documents/current")
				.header("Origin", origin)
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", origin))
			.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"issuer", "audience", "missing-audience", "expired", "signature", "subject", "zero", "negative"})
	@DisplayName("운영 JWT 검증 계약에 맞지 않는 토큰은 거부한다")
	void rejectsInvalidJwt(String invalidField) throws Exception {
		// given
		Member member = createMember();
		Instant now = Instant.now();
		String subject = switch (invalidField) {
			case "subject" -> "invalid";
			case "zero" -> "0";
			case "negative" -> "-1";
			default -> member.getId().toString();
		};
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
			.issuer(invalidField.equals("issuer") ? "wrong-issuer" : properties.issuer())
			.subject(subject)
			.issuedAt(now.minusSeconds(600))
			.expiresAt(invalidField.equals("expired") ? now.minusSeconds(300) : now.plusSeconds(300));
		if (!invalidField.equals("missing-audience")) {
			claims.audience(List.of(invalidField.equals("audience") ? "wrong-audience" : properties.audience()));
		}
		JwtEncoder encoder = invalidField.equals("signature")
			? NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
				"another-test-only-secret-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
				.algorithm(MacAlgorithm.HS256).build() : jwtEncoder;
		String token = encoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();

		// when & then
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("M002"))
			.andExpect(jsonPath("$.traceId").isNotEmpty());
	}

	@Test
	@DisplayName("삭제 시각이 없어도 비활성 회원의 JWT를 거부한다")
	void rejectsInactiveMember() throws Exception {
		// given
		Member member = createMember();
		AuthTokenPair tokens = tokenService.issueAccessAndRefreshTokenPair(member.getId(), Instant.now());
		member.withdraw();
		memberRepository.saveAndFlush(member);

		// when & then
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("M002"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"logout", "refresh"})
	@DisplayName("동시 재발급과 폐기 요청은 family 잠금 후 유효 토큰을 남기지 않는다")
	void serializesConcurrentRefreshAndRevocation(String operation) throws Exception {
		// given
		Member member = createMember();
		AuthTokenPair first = tokenService.issueAccessAndRefreshTokenPair(member.getId(), Instant.now());
		// 교체된 첫 행을 잠금 기준으로 유지하는 경우도 검증한다.
		AuthTokenPair current = tokenService.rotateRefreshToken(first.refreshToken(), Instant.now());
		UUID family = jdbcTemplate.queryForObject(
			"SELECT family_id FROM orca.refresh_token WHERE member_id = ? ORDER BY id LIMIT 1",
			UUID.class, member.getId());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<MvcResult> refresh;
			Future<MvcResult> competing;
			try (Connection connection = dataSource.getConnection()) {
				connection.setAutoCommit(false);
				try (PreparedStatement lock = connection.prepareStatement(
					"SELECT id FROM orca.refresh_token WHERE family_id = ? ORDER BY id LIMIT 1 FOR UPDATE")) {
					lock.setObject(1, family);
					lock.executeQuery().close();
				}

				// when: 두 HTTP 요청이 실제 PostgreSQL 잠금을 기다리는 것을 확인한다.
				refresh = executor.submit(() -> mockMvc.perform(post("/api/v1/auth/refresh")
					.cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, current.refreshToken()))).andReturn());
				competing = executor.submit(() -> mockMvc.perform(post("/api/v1/auth/" + operation)
					.cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, current.refreshToken()))).andReturn());
				try {
					awaitBlockedTokenRequests();
				} finally {
					connection.rollback();
				}
			}
			MvcResult firstResult = refresh.get(15, TimeUnit.SECONDS);
			MvcResult secondResult = competing.get(15, TimeUnit.SECONDS);

			// then
			if (operation.equals("logout")) {
				assertThat(firstResult.getResponse().getStatus()).isIn(200, 401);
				assertThat(secondResult.getResponse().getStatus()).isEqualTo(204);
			} else {
				assertThat(List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus()))
					.containsExactlyInAnyOrder(200, 401);
			}
			assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM orca.refresh_token WHERE family_id = ? AND revoked_at IS NULL",
				Integer.class, family)).isZero();
			for (MvcResult result : List.of(firstResult, secondResult)) {
				Cookie cookie = result.getResponse().getCookie(AuthProperties.REFRESH_COOKIE_NAME);
				if (cookie != null && !cookie.getValue().isEmpty()) {
					mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
						.andExpect(status().isUnauthorized());
				}
			}
		} finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
		}
	}

	private void awaitBlockedTokenRequests() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			Integer blocked = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pg_stat_activity WHERE datname = current_database() "
					+ "AND wait_event_type = 'Lock' AND query LIKE '%refresh_token%'", Integer.class);
			if (blocked >= 2) {
				return;
			}
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
		}
		throw new AssertionError("두 토큰 요청의 DB 잠금 대기를 확인하지 못했습니다.");
	}

	@Test
	@DisplayName("탈퇴한 회원의 기존 JWT는 인증 단계에서 거부한다")
	void rejectsWithdrawnMemberToken() throws Exception {
		// given
		Member member = createMember();
		AuthTokenPair tokens = tokenService.issueAccessAndRefreshTokenPair(member.getId(), Instant.now());
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
			.andExpect(status().isOk());

		// when
		mockMvc.perform(delete("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
			.andExpect(status().isOk());

		// then
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("M002"));
	}

	@Test
	@DisplayName("교체된 토큰으로 로그아웃해도 같은 family의 새 토큰을 폐기한다")
	void logoutRevokesRotatedFamily() throws Exception {
		// given
		Member member = createMember();
		AuthTokenPair original = tokenService.issueAccessAndRefreshTokenPair(member.getId(), Instant.now());
		AuthTokenPair other = tokenService.issueAccessAndRefreshTokenPair(member.getId(), Instant.now());
		MvcResult rotated = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, original.refreshToken())))
			.andExpect(status().isOk()).andReturn();
		String next = rotated.getResponse().getCookie(AuthProperties.REFRESH_COOKIE_NAME).getValue();

		// when
		mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, original.refreshToken())))
			.andExpect(status().isNoContent());

		// then
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, next)))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie(AuthProperties.REFRESH_COOKIE_NAME, other.refreshToken())))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("OAuth 업무 실패는 오류 화면으로 이동하고 세션을 폐기한다")
	void redirectsBusinessFailure() throws Exception {
		// given
		String subject = "missing-" + UUID.randomUUID();
		memberIdentityRepository.saveAndFlush(MemberIdentity.create(Long.MAX_VALUE, "google", subject, Instant.now()));
		when(accessTokenResponseClient.getTokenResponse(any())).thenReturn(OAuth2AccessTokenResponse
			.withToken("private-provider-token").tokenType(OAuth2AccessToken.TokenType.BEARER).build());
		OAuth2User user = new DefaultOAuth2User(List.of(), Map.of("sub", subject), "sub");
		when(oauth2UserService.loadUser(any())).thenReturn(new OAuth2ProfileUser(user,
			new OAuthProfile(OAuthProvider.GOOGLE, subject, "private@example.com")));
		MvcResult start = mockMvc.perform(get("/api/v1/auth/oauth/google")).andReturn();
		MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
		OAuth2AuthorizationRequest authorization = (OAuth2AuthorizationRequest) session
			.getAttribute(AUTHORIZATION_REQUEST_SESSION_ATTRIBUTE);

		// when & then
		mockMvc.perform(get("/api/v1/auth/oauth/google/callback").session(session)
				.param("code", "private-code").param("state", authorization.getState()))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
				.isEqualTo("http://localhost:3000/auth/error?error=oauth_failed"))
			.andExpect(result -> assertThat(session.isInvalid()).isTrue())
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.noneMatch(cookie -> cookie.startsWith(AuthProperties.REFRESH_COOKIE_NAME + "=")));
	}

	private Member createMember() {
		return memberRepository.saveAndFlush(Member.register("test@example.com",
			"회원_" + UUID.randomUUID().toString().substring(0, 8), null, null));
	}

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
				.anyMatch(cookie -> cookie.startsWith("manta_signup_context=")
					&& cookie.matches(".*Max-Age=(599|600)(;|$).*")))
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

}
