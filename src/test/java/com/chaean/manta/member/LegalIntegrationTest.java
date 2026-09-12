package com.chaean.manta.member;

import static com.epages.restdocs.apispec.Schema.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.member.entity.LegalDocumentType;
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
class LegalIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUpLegalDocuments() {
		LegalDocumentFixture.createCurrentDocuments(jdbcTemplate);
	}

	@Test
	@DisplayName("현재 약관은 인증 없이 문서 ID와 생성 시각으로 조회할 수 있다")
	void readsCurrentLegalDocumentsWithoutAuthentication() throws Exception {
		// given

		// when & then
		mockMvc.perform(get("/api/v1/legal-documents/current"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].id").isNumber())
			.andExpect(jsonPath("$.data[0].documentType").isString())
			.andDo(MockMvcRestDocumentationWrapper.document(
				"legal/current-documents",
				ResourceSnippetParameters.builder()
					.responseSchema(schema("LegalDocumentResponse"))
					.summary("현재 약관을 조회한다.")
					.description("로그인 전에도 동의할 약관의 최신 문서를 조회한다."),
				responseFields(
					fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("약관 문서 ID"),
					new EnumFields(LegalDocumentType.class).withPath("data[].documentType").description("약관 유형"),
					fieldWithPath("data[].title").type(JsonFieldType.STRING).description("약관 제목"),
					fieldWithPath("data[].content").type(JsonFieldType.STRING).description("약관 본문"),
					fieldWithPath("data[].required").type(JsonFieldType.BOOLEAN).description("필수 동의 여부"),
					fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성 시각")
				)
			));
	}

	@Test
	@DisplayName("추가 약관 동의 API를 제공하지 않고 활성 회원은 바로 프로필을 수정한다")
	void doesNotProvideAgreementsApi() throws Exception {
		// given
		String token = Long.toString(insertActiveMember());

		// when & then
		mockMvc.perform(post("/api/v1/me/agreements").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"legalDocumentIds\":[1]}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"description\":\"활성 회원 프로필\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.description").value("활성 회원 프로필"));
	}

	@Test
	@DisplayName("약관 유형별 가장 최근에 생성된 문서를 현재 약관으로 조회한다")
	void readsLatestDocumentPerType() throws Exception {
		// given
		Long latestDocumentId = jdbcTemplate.queryForObject(
			"INSERT INTO orca.legal_document "
				+ "(document_type, title, content, is_required, created_at, updated_at) "
				+ "VALUES ('TERMS_OF_SERVICE', '최신 이용약관', '최신 약관 본문', TRUE, CURRENT_TIMESTAMP, "
				+ "CURRENT_TIMESTAMP) "
				+ "RETURNING id",
			Long.class);

		// when
		MvcResult result = mockMvc.perform(get("/api/v1/legal-documents/current"))
			.andExpect(status().isOk())
			.andReturn();

		// then
		String response = result.getResponse().getContentAsString();
		List<Number> currentIds = com.jayway.jsonpath.JsonPath.read(response, "$.data[*].id");
		assertThat(currentIds.stream()
			.map(Number::longValue)
			.toList())
			.contains(latestDocumentId);
	}

	private Long insertActiveMember() {
		return jdbcTemplate.queryForObject(
			"INSERT INTO orca.member "
				+ "(nickname, email, status, role, created_at, updated_at) "
				+ "VALUES (?, 'legal@example.com', 'ACTIVE', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id",
			Long.class, "테스트약관회원_" + UUID.randomUUID().toString().substring(0, 8));
	}
}
