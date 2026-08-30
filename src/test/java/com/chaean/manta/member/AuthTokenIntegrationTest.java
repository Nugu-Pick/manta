package com.chaean.manta.member;

import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.member.adapter.OAuthProviderClient;
import com.chaean.manta.member.internal.application.model.OAuthProfile;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(AuthTokenIntegrationTest.RealJwtDecoderConfiguration.class)
class AuthTokenIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean(name = "googleOAuthProviderClient")
	private OAuthProviderClient googleOAuthProviderClient;

	@BeforeEach
	void setUpGoogleProviderClient() {
		when(googleOAuthProviderClient.provider()).thenReturn(OAuthProvider.GOOGLE);
		when(googleOAuthProviderClient.redirectUri())
			.thenReturn("http://localhost:8080/api/v1/auth/oauth/google/callback");
		when(googleOAuthProviderClient.createAuthorizationUrl(anyString(), anyString(), anyString()))
			.thenAnswer(invocation -> "https://accounts.google.com/o/oauth2/v2/auth?code_challenge="
				+ invocation.getArgument(1) + "&state=" + invocation.getArgument(0));
	}

	@Test
	@DisplayName("OAuth provider 로그인 시작 시 state와 PKCE verifier cookie를 생성한다")
	void startsGoogleOAuth() throws Exception {
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.contains("https://accounts.google.com/o/oauth2/v2/auth", "code_challenge=", "state="))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> hasCookieAttributes(
					cookie, "manta_test_oauth_state=", "HttpOnly", "Secure", "Path=/api/v1/auth"))
				.anyMatch(cookie -> hasCookieAttributes(
					cookie, "manta_test_oauth_verifier=", "HttpOnly", "Secure", "Path=/api/v1/auth")))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/oauth-start",
				ResourceSnippetParameters.builder()
					.summary("OAuth provider 로그인을 시작한다.")
					.description("PKCE verifier와 브라우저 결속용 OAuth state를 cookie에 저장한 뒤 provider authorization endpoint로 이동한다."),
				pathParameters(parameterWithName("provider").description("OAuth provider 이름")),
				responseHeaders(
					headerWithName("Location").description("OAuth provider authorization endpoint"),
					headerWithName("Set-Cookie").description("HttpOnly OAuth state와 PKCE verifier cookie"))
			));
	}

	@Test
	@DisplayName("OAuth callback은 다른 브라우저의 state를 거부하고 OAuth cookie를 삭제한다")
	void rejectsStateFromAnotherBrowser() throws Exception {
		jakarta.servlet.http.Cookie stateCookie = new jakarta.servlet.http.Cookie("manta_test_oauth_state",
			"different-browser-state");

		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", "callback-state")
				.cookie(stateCookie))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("M110"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_state=", "Max-Age=0"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_verifier=", "Max-Age=0")));
	}

	@Test
	@DisplayName("OAuth callback query parameter가 없으면 OAuth cookie를 삭제하고 거부한다")
	void rejectsCallbackWithoutQueryParametersAndClearsCookies() throws Exception {
		mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.cookie(
					new jakarta.servlet.http.Cookie("manta_test_oauth_state", "state"),
					new jakarta.servlet.http.Cookie("manta_test_oauth_verifier", "verifier")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("M110"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_state=", "Max-Age=0"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_verifier=", "Max-Age=0")));
	}

	@Test
	@DisplayName("OAuth callback은 Refresh Token cookie를 설정하고 프런트는 refresh로 Access Token을 받는다")
	void completesGoogleOAuthCallbackAndRefreshesAccessToken() throws Exception {
		String email = "callback-" + UUID.randomUUID() + "@example.com";
		when(googleOAuthProviderClient.exchangeAuthorizationCode(eq("provider-code"), anyString(),
			eq("http://localhost:8080/api/v1/auth/oauth/google/callback")))
			.thenReturn(new OAuthProfile("google-callback-subject-" + UUID.randomUUID(), email));
		MvcResult start = mockMvc.perform(get("/api/v1/auth/oauth/{provider}", "google"))
			.andExpect(status().isFound())
			.andReturn();
		String state = UriComponentsBuilder.fromUriString(start.getResponse().getHeader("Location"))
			.build()
			.getQueryParams()
			.getFirst("state");
		String stateCookie = cookieValue(start.getResponse(), "manta_test_oauth_state");
		String codeVerifierCookie = cookieValue(start.getResponse(), "manta_test_oauth_verifier");

		MvcResult callback = mockMvc.perform(get("/api/v1/auth/oauth/{provider}/callback", "google")
				.param("code", "provider-code")
				.param("state", state)
				.cookie(
					new jakarta.servlet.http.Cookie("manta_test_oauth_state", stateCookie),
					new jakarta.servlet.http.Cookie("manta_test_oauth_verifier", codeVerifierCookie)))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Location"))
				.isEqualTo("http://localhost:3000/auth/callback"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_refresh_token=", "HttpOnly", "Secure"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_state=", "Max-Age=0"))
				.anyMatch(cookie -> hasCookieAttributes(cookie, "manta_test_oauth_verifier=", "Max-Age=0")))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/oauth-callback",
				ResourceSnippetParameters.builder()
					.summary("OAuth callback을 처리한다.")
					.description("provider code를 교환하고 member·identity와 Refresh Token을 준비한 뒤 프런트로 이동한다."),
				pathParameters(parameterWithName("provider").description("OAuth provider 이름")),
				queryParameters(
					parameterWithName("code").description("OAuth provider authorization code"),
					parameterWithName("state").description("OAuth 시작 시 발급한 state")
				),
				responseHeaders(
					headerWithName("Location").description("고정 프런트 callback URI"),
					headerWithName("Set-Cookie").description("Refresh Token과 삭제된 OAuth cookie"))
			))
			.andReturn();

		Integer memberCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.member WHERE email = ?", Integer.class, email);
		Integer identityCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.member_identity WHERE provider = 'google' "
				+ "AND provider_subject LIKE 'google-callback-subject-%'",
			Integer.class);
		assertThat(memberCount).isEqualTo(1);
		assertThat(identityCount).isGreaterThanOrEqualTo(1);

		String firstCookie = refreshCookieValue(callback.getResponse());
		MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(firstCookie)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isString())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.memberStatus").value("ONBOARDING"))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/refresh",
				ResourceSnippetParameters.builder()
					.responseSchema(schema("AuthTokenResponse"))
					.summary("Refresh Token으로 Access Token을 갱신한다.")
					.description("HttpOnly Refresh Token을 rotation하고 새 Access Token과 Refresh Token을 반환한다."),
				requestCookies(cookieWithName("manta_test_refresh_token").description("현재 Refresh Token cookie")),
				responseHeaders(headerWithName("Set-Cookie").description("rotation된 Refresh Token cookie")),
				responseFields(
					fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("Manta Access Token"),
					fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입"),
					fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("Access Token 만료 시각"),
					fieldWithPath("data.memberStatus").type(JsonFieldType.STRING).description("회원 상태")
				)
			))
			.andReturn();

		String expiresAt = com.jayway.jsonpath.JsonPath.read(
			refresh.getResponse().getContentAsString(), "$.data.expiresAt");
		assertThat(java.time.OffsetDateTime.parse(expiresAt).toInstant())
			.isBetween(Instant.now().plusSeconds(1_790), Instant.now().plusSeconds(1_810));
		assertThat(refresh.getResponse().getHeaders("Set-Cookie"))
			.anyMatch(cookie -> hasCookieAttributes(
				cookie, "manta_test_refresh_token=", "HttpOnly", "Secure", "Path=/api/v1/auth"));
		String secondCookie = refreshCookieValue(refresh.getResponse());
		assertThat(secondCookie).isNotEqualTo(firstCookie);

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(firstCookie)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("M113"));
		Integer revokedTokenCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.refresh_token "
				+ "WHERE member_id = (SELECT id FROM orca.member WHERE email = ?) AND revoked_at IS NOT NULL",
			Integer.class, email);
		assertThat(revokedTokenCount).isEqualTo(2);

		mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie(secondCookie)))
			.andExpect(status().isNoContent())
			.andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie"))
				.contains("Max-Age=0", "HttpOnly", "Secure"))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/logout",
				ResourceSnippetParameters.builder()
					.summary("Refresh Token을 폐기하고 로그아웃한다.")
					.description("현재 Refresh Token을 폐기하고 인증 쿠키를 삭제한다."),
				responseHeaders(headerWithName("Set-Cookie").description("삭제된 Refresh Token cookie"))
			));
	}

	private String refreshCookieValue(MockHttpServletResponse response) {
		return cookieValue(response, "manta_test_refresh_token");
	}

	private String cookieValue(MockHttpServletResponse response, String name) {
		String header = response.getHeaders("Set-Cookie").stream()
			.filter(value -> value.startsWith(name + "="))
			.findFirst()
			.orElseThrow();
		String value = header.substring((name + "=").length());
		return value.substring(0, value.indexOf(';'));
	}

	private jakarta.servlet.http.Cookie refreshCookie(String value) {
		return new jakarta.servlet.http.Cookie("manta_test_refresh_token", value);
	}

	private boolean hasCookieAttributes(String cookie, String... expectedAttributes) {
		return java.util.Arrays.stream(expectedAttributes).allMatch(cookie::contains);
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
