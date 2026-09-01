package com.chaean.manta.member;

import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.member.internal.application.SignupContextCodec;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.application.model.SignupContext;
import com.chaean.manta.member.fixture.LegalDocumentFixture;
import com.chaean.manta.support.PostgresIntegrationTest;
import com.chaean.manta.support.TestJwtDecoderConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(TestJwtDecoderConfiguration.class)
class SignupIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SignupContextCodec contextCodec;

	@BeforeEach
	void setUpLegalDocuments() {
		LegalDocumentFixture.createCurrentDocuments(jdbcTemplate);
	}

	@Test
	@DisplayName("필수 약관만 동의하고 선택 프로필을 건너뛰어 가입한다")
	void completesSignupWithNullableOptionalProfile() throws Exception {
		// given
		String email = uniqueEmail();
		String subject = "signup-subject-" + UUID.randomUUID();
		List<Long> documentIds = currentDocumentIds();
		String context = contextCookie(OAuthProvider.GOOGLE, subject, email);

		// when & then
		MvcResult signup = mockMvc.perform(post("/api/v1/auth/signup/complete")
				.cookie(new jakarta.servlet.http.Cookie("manta_signup_context", context))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"legalDocumentIds\":" + documentIds + ",\"gender\":null,\"ageGroup\":null}"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
			.andExpect(header().string("Referrer-Policy", "no-referrer"))
			.andExpect(jsonPath("$.data.accessToken").isString())
			.andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
			.andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
				.anyMatch(cookie -> cookie.startsWith("manta_refresh_token=") && cookie.contains("HttpOnly"))
				.anyMatch(cookie -> cookie.contains("manta_signup_context=") && cookie.contains("Max-Age=0")))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/signup-complete",
				ResourceSnippetParameters.builder()
					.requestSchema(schema("SignupCompleteRequest"))
					.responseSchema(schema("AuthTokenResponse"))
					.summary("소셜 OAuth 가입을 완료한다.")
					.description("필수 약관과 선택 프로필을 검증하고 회원·identity·동의·Refresh Token을 "
						+ "원자적으로 생성한다."),
				requestCookies(cookieWithName("manta_signup_context").description("10분 signup context cookie")),
				requestFields(
					fieldWithPath("legalDocumentIds").type(JsonFieldType.ARRAY)
						.attributes(key("itemsType").value("number"))
						.description("동의할 현재 약관 문서 ID 목록"),
					fieldWithPath("gender").type(JsonFieldType.STRING).description("성별 또는 null").optional(),
					fieldWithPath("ageGroup").type(JsonFieldType.STRING).description("연령대 또는 null").optional()
				),
				responseHeaders(headerWithName("Set-Cookie").description("Refresh Token 발급 및 signup context 삭제")),
				responseFields(
					fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("Manta Access Token"),
					fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입"),
					fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("Access Token 만료 시각"),
					fieldWithPath("data.memberStatus").type(JsonFieldType.STRING).description("회원 상태")
				)
			))
			.andReturn();

		Long memberId = jdbcTemplate.queryForObject("SELECT id FROM orca.member WHERE email = ?", Long.class, email);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member_identity WHERE member_id = ?",
			Integer.class, memberId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member_agreement WHERE member_id = ?",
			Integer.class, memberId)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.refresh_token WHERE member_id = ?",
			Integer.class, memberId)).isEqualTo(1);

		String firstRefreshToken = cookieValue(signup.getResponse(), "manta_refresh_token");
		MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("manta_refresh_token", firstRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
			.andExpect(header().string("Referrer-Policy", "no-referrer"))
			.andExpect(jsonPath("$.data.accessToken").isString())
			.andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/refresh",
				ResourceSnippetParameters.builder()
					.responseSchema(schema("AuthTokenResponse"))
					.summary("Refresh Token으로 Access Token을 갱신한다.")
					.description("HttpOnly Refresh Token을 rotation하고 새 Access Token과 Refresh Token을 반환한다."),
				requestCookies(cookieWithName("manta_refresh_token").description("현재 Refresh Token cookie")),
				responseHeaders(headerWithName("Set-Cookie").description("rotation된 Refresh Token cookie")),
				responseFields(
					fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("Manta Access Token"),
					fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입"),
					fieldWithPath("data.expiresAt").type(JsonFieldType.STRING).description("Access Token 만료 시각"),
					fieldWithPath("data.memberStatus").type(JsonFieldType.STRING).description("회원 상태")
				)
			))
			.andReturn();
		String secondRefreshToken = cookieValue(refresh.getResponse(), "manta_refresh_token");
		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("manta_refresh_token", firstRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("M113"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.refresh_token "
			+ "WHERE member_id = ? AND revoked_at IS NOT NULL", Integer.class, memberId)).isEqualTo(2);
		mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(new jakarta.servlet.http.Cookie("manta_refresh_token", secondRefreshToken)))
			.andExpect(status().isNoContent())
			.andDo(MockMvcRestDocumentationWrapper.document(
				"auth/logout",
				ResourceSnippetParameters.builder()
					.summary("Refresh Token을 폐기하고 로그아웃한다.")
					.description("현재 Refresh Token을 폐기하고 인증 쿠키를 삭제한다."),
				responseHeaders(headerWithName("Set-Cookie").description("삭제된 Refresh Token cookie"))
			));
	}

	@Test
	@DisplayName("필수 약관이 빠지면 회원과 인증 정보를 만들지 않는다")
	void rejectsSignupWithoutRequiredAgreement() throws Exception {
		// given
		String email = uniqueEmail();
		String context = contextCookie(OAuthProvider.KAKAO, "missing-agreement-" + UUID.randomUUID(), email);
		Long firstDocumentId = currentDocumentIds().get(0);

		// when & then
		MvcResult failure = mockMvc.perform(post("/api/v1/auth/signup/complete")
				.cookie(new jakarta.servlet.http.Cookie("manta_signup_context", context))
				.contentType("application/json")
				.content("{\"legalDocumentIds\":[" + firstDocumentId + "],\"gender\":\"MALE\",\"ageGroup\":\"TEENS\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("M114"))
			.andReturn();
		assertThat(failure.getResponse().getHeaders("Set-Cookie"))
			.noneMatch(cookie -> cookie.startsWith("manta_signup_context=") && cookie.contains("Max-Age=0"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member WHERE email = ?", Integer.class, email))
			.isZero();
	}

	@Test
	@DisplayName("같은 이메일의 서로 다른 provider subject는 별도 회원으로 가입한다")
	void allowsDuplicateEmailAcrossProviders() throws Exception {
		// given
		String email = uniqueEmail();
		List<Long> documentIds = currentDocumentIds();

		// when
		complete(email, OAuthProvider.GOOGLE, "google-" + UUID.randomUUID(), documentIds);
		complete(email, OAuthProvider.KAKAO, "kakao-" + UUID.randomUUID(), documentIds);

		// then
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member WHERE email = ?", Integer.class, email))
			.isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member_identity WHERE provider_subject LIKE ?",
			Integer.class, "google-%")).isGreaterThanOrEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member_identity WHERE provider_subject LIKE ?",
			Integer.class, "kakao-%")).isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("만료되거나 없는 signup context는 가입을 완료할 수 없다")
	void rejectsMissingSignupContext() throws Exception {
		// when & then
		mockMvc.perform(post("/api/v1/auth/signup/complete")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"legalDocumentIds\":" + currentDocumentIds() + ",\"gender\":null,\"ageGroup\":null}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("M112"));
	}

	@Test
	@DisplayName("가입 완료의 성별과 연령대는 정해진 값만 허용한다")
	void rejectsInvalidOptionalProfile() throws Exception {
		// given
		String email = uniqueEmail();
		String context = contextCookie(OAuthProvider.GOOGLE, "invalid-profile-" + UUID.randomUUID(), email);

		// when & then
		MvcResult failure = mockMvc.perform(post("/api/v1/auth/signup/complete")
				.cookie(new jakarta.servlet.http.Cookie("manta_signup_context", context))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"legalDocumentIds\":" + currentDocumentIds() + ",\"gender\":\"OTHER\",\"ageGroup\":null}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("M117"))
			.andReturn();
		assertThat(failure.getResponse().getHeaders("Set-Cookie"))
			.noneMatch(cookie -> cookie.startsWith("manta_signup_context=") && cookie.contains("Max-Age=0"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member WHERE email = ?", Integer.class, email))
			.isZero();
	}

	@Test
	@DisplayName("같은 provider subject로 가입을 다시 완료하면 충돌 오류를 반환한다")
	void rejectsDuplicateProviderSubject() throws Exception {
		// given
		String email = uniqueEmail();
		String subject = "duplicate-subject-" + UUID.randomUUID();
		List<Long> documentIds = currentDocumentIds();
		String context = contextCookie(OAuthProvider.GOOGLE, subject, email);
		complete(email, OAuthProvider.GOOGLE, subject, documentIds);

		// when & then
		mockMvc.perform(post("/api/v1/auth/signup/complete")
				.cookie(new jakarta.servlet.http.Cookie("manta_signup_context", context))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"legalDocumentIds\":" + documentIds + ",\"gender\":null,\"ageGroup\":null}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("M112"));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orca.member WHERE email = ?", Integer.class, email))
			.isEqualTo(1);
	}

	private void complete(String email, OAuthProvider provider, String subject, List<Long> documentIds) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup/complete")
				.cookie(new jakarta.servlet.http.Cookie("manta_signup_context", contextCookie(provider, subject, email)))
				.contentType("application/json")
				.content("{\"legalDocumentIds\":" + documentIds + ",\"gender\":\"FEMALE\",\"ageGroup\":\"TWENTIES\"}"))
			.andExpect(status().isOk());
	}

	private String contextCookie(OAuthProvider provider, String subject, String email) {
		Instant issuedAt = Instant.now();
		return contextCodec.encode(new SignupContext(provider, subject, email, issuedAt, issuedAt.plusSeconds(600)));
	}

	private List<Long> currentDocumentIds() {
		return jdbcTemplate.queryForList("SELECT id FROM ("
			+ "SELECT id, ROW_NUMBER() OVER (PARTITION BY document_type ORDER BY created_at DESC, id DESC) AS row_number "
			+ "FROM orca.legal_document) current_documents WHERE row_number = 1 ORDER BY id", Long.class);
	}

	private String uniqueEmail() {
		return "signup-" + UUID.randomUUID() + "@example.com";
	}

	private String cookieValue(MockHttpServletResponse response, String name) {
		String header = response.getHeaders("Set-Cookie").stream()
			.filter(value -> value.startsWith(name + "="))
			.findFirst()
			.orElseThrow();
		String value = header.substring((name + "=").length());
		return value.substring(0, value.indexOf(';'));
	}

}
