package com.chaean.manta.member;

import static com.epages.restdocs.apispec.Schema.schema;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(TestJwtDecoderConfiguration.class)
class MemberOnboardingIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUpLegalDocuments() {
		LegalDocumentFixture.createCurrentDocuments(jdbcTemplate);
	}

	@Test
	@DisplayName("온보딩 전에는 프로필 변경을 제한하고 별도 인증 상태 API를 제공하지 않는다")
	void restrictsActiveFeaturesBeforeOnboarding() throws Exception {
		// given
		String token = Long.toString(insertOnboardingMember());

		// when & then
		mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"온보딩 전\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("M116"));
		mockMvc.perform(get("/api/v1/me/auth-status").header("Authorization", "Bearer " + token))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/members/{memberId}", token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("M116"));
	}

	@Test
	@DisplayName("필수 약관과 프로필을 한 번에 저장하고 회원을 활성화한다")
	void completesOnboardingInOneTransaction() throws Exception {
		// given
		String token = Long.toString(insertOnboardingMember());
		List<Number> documentIds = currentDocumentIds();
		String ids = documentIds.stream().map(Number::toString).collect(java.util.stream.Collectors.joining(","));

		// when & then
		mockMvc.perform(post("/api/v1/me/onboarding").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "legalDocumentIds": [%s],
					  "gender": "FEMALE",
					  "ageGroup": "TWENTIES"
					}
					""".formatted(ids)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.memberId").isNumber())
			.andExpect(jsonPath("$.data.status").value("ACTIVE"))
			.andDo(MockMvcRestDocumentationWrapper.document(
				"member/onboarding",
				ResourceSnippetParameters.builder()
					.requestSchema(schema("MemberOnboardingRequest"))
					.responseSchema(schema("MemberOnboardingResponse"))
					.summary("회원 온보딩을 완료한다.")
					.description("필수 약관 동의와 초기 프로필을 하나의 트랜잭션으로 저장하고 회원을 활성화한다."),
				requestHeaders(headerWithName("Authorization").description("Bearer Manta access token")),
				requestFields(
					fieldWithPath("legalDocumentIds").type(JsonFieldType.ARRAY).description("동의할 현재 약관 문서 ID 목록"),
					fieldWithPath("gender").type(JsonFieldType.STRING).description("성별"),
					fieldWithPath("ageGroup").type(JsonFieldType.STRING).description("연령대")
				),
				responseFields(
					fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
					fieldWithPath("data.status").type(JsonFieldType.STRING).description("회원 상태")
				)
			));

		Integer agreementCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.member_agreement WHERE member_id = ?", Integer.class, Long.parseLong(token));
		String memberStatus = jdbcTemplate.queryForObject(
			"SELECT status FROM orca.member WHERE id = ?", String.class, Long.parseLong(token));
		org.assertj.core.api.Assertions.assertThat(agreementCount).isEqualTo(2);
		org.assertj.core.api.Assertions.assertThat(memberStatus).isEqualTo("ACTIVE");
		mockMvc.perform(post("/api/v1/me/onboarding").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"legalDocumentIds": [%s], "gender": "FEMALE", "ageGroup": "TWENTIES"}
					""".formatted(ids)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("M115"));
	}

	@Test
	@DisplayName("필수 약관이 빠지면 회원과 약관을 일부 저장하지 않는다")
	void rollsBackOnboardingWhenRequiredAgreementIsMissing() throws Exception {
		// given
		String token = Long.toString(insertOnboardingMember());
		Long documentId = currentDocumentIds().get(0).longValue();

		// when & then
		mockMvc.perform(post("/api/v1/me/onboarding").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"legalDocumentIds":[%d],"gender":"FEMALE","ageGroup":"TWENTIES"}
					""".formatted(documentId)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("M114"));

		Integer agreementCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.member_agreement WHERE member_id = ?", Integer.class, Long.parseLong(token));
		String memberStatus = jdbcTemplate.queryForObject(
			"SELECT status FROM orca.member WHERE id = ?", String.class, Long.parseLong(token));
		org.assertj.core.api.Assertions.assertThat(agreementCount).isZero();
		org.assertj.core.api.Assertions.assertThat(memberStatus).isEqualTo("ONBOARDING");
	}

	private Long insertOnboardingMember() {
		return jdbcTemplate.queryForObject(
			"INSERT INTO orca.member "
				+ "(nickname, email, status, role, created_at, updated_at) "
				+ "VALUES (?, ?, 'ONBOARDING', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id",
			Long.class,
			"테스트온보딩회원_" + UUID.randomUUID().toString().substring(0, 8),
			"onboarding-" + UUID.randomUUID() + "@example.com");
	}

	private List<Number> currentDocumentIds() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/legal-documents/current"))
			.andExpect(status().isOk())
			.andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data[*].id");
	}
}
