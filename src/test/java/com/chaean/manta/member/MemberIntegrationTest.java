package com.chaean.manta.member;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.chaean.manta.support.PostgresIntegrationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(MemberIntegrationTest.TestJwtConfiguration.class)
class MemberIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 이메일의 서로 다른 Supabase subject는 별도 회원으로 프로비저닝한다")
    void provisionsDifferentMembersForDifferentSubjects() throws Exception {
        // given

        // when
        MvcResult first = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer subject-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/me",
                        ResourceSnippetParameters.builder()
                                .summary("인증된 회원의 내 정보를 조회한다.")
                                .description("검증된 Supabase JWT로 현재 회원 정보를 조회한다.")
                                .requestHeaders(headerWithName("Authorization")
                                        .description("Bearer Supabase access token"))
                                .responseFields(
                                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                        fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                                .description("회원 닉네임"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING)
                                                .description("OAuth provider가 제공한 이메일"),
                                        fieldWithPath("data.gender").type(JsonFieldType.STRING).description("성별")
                                                .optional(),
                                        fieldWithPath("data.ageGroup").type(JsonFieldType.STRING).description("연령대")
                                                .optional(),
                                        fieldWithPath("data.description").type(JsonFieldType.STRING).description("회원 설명")
                                                .optional(),
                                        fieldWithPath("data.avatarAssetId").type(JsonFieldType.NUMBER)
                                                .description("프로필 이미지 asset ID").optional(),
                                        fieldWithPath("data.role").type(JsonFieldType.STRING)
                                                .description("회원 역할")
                                )
                ))
                .andReturn();
        MvcResult repeat = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer subject-a"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer subject-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andReturn();

        // then
        Long firstId = readMemberId(first);
        Long repeatedId = readMemberId(repeat);
        Long secondId = readMemberId(second);
        org.assertj.core.api.Assertions.assertThat(repeatedId).isEqualTo(firstId);
        org.assertj.core.api.Assertions.assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    @DisplayName("인증된 회원은 프로필을 수정할 수 있다")
    void updatesMyProfile() throws Exception {
        // given
        String token = "profile-subject";
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "  맛집탐험가  ",
                                  "gender": "FEMALE",
                                  "ageGroup": "TWENTIES",
                                  "description": "새로운 맛집을 찾습니다.",
                                  "avatarAssetId": 101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("맛집탐험가"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.ageGroup").value("TWENTIES"))
                .andExpect(jsonPath("$.data.description").value("새로운 맛집을 찾습니다."))
                .andExpect(jsonPath("$.data.avatarAssetId").value(101))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/me-update",
                        ResourceSnippetParameters.builder()
                                .summary("인증된 회원의 내 정보를 수정한다.")
                                .description("닉네임과 공개 프로필 정보를 수정한다.")
                                .requestHeaders(headerWithName("Authorization")
                                        .description("Bearer Supabase access token"))
                                .requestFields(
                                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                        fieldWithPath("gender").type(JsonFieldType.STRING).description("성별")
                                                .optional(),
                                        fieldWithPath("ageGroup").type(JsonFieldType.STRING).description("연령대")
                                                .optional(),
                                        fieldWithPath("description").type(JsonFieldType.STRING).description("회원 설명")
                                                .optional(),
                                        fieldWithPath("avatarAssetId").type(JsonFieldType.NUMBER)
                                                .description("프로필 이미지 asset ID").optional()
                                )
                                .responseFields(
                                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                        fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                                .description("회원 닉네임"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING)
                                                .description("OAuth provider가 제공한 이메일"),
                                        fieldWithPath("data.gender").type(JsonFieldType.STRING).description("성별"),
                                        fieldWithPath("data.ageGroup").type(JsonFieldType.STRING).description("연령대"),
                                        fieldWithPath("data.description").type(JsonFieldType.STRING).description("회원 설명"),
                                        fieldWithPath("data.avatarAssetId").type(JsonFieldType.NUMBER)
                                                .description("프로필 이미지 asset ID"),
                                        fieldWithPath("data.role").type(JsonFieldType.STRING).description("회원 역할")
                                )
                ));
    }

    @Test
    @DisplayName("설명은 기존 160자 제한을 넘어도 수정하고 응답한다")
    void updatesAndReturnsLongDescription() throws Exception {
        // given
        String token = "long-description-subject";
        String longDescription = "가".repeat(161);
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + longDescription + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value(longDescription));
    }

    @Test
    @DisplayName("공개 회원 프로필은 인증 없이 조회할 수 있다")
    void getsPublicProfileWithoutAuthentication() throws Exception {
        // given
        MvcResult result = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer public-profile-subject"))
                .andExpect(status().isOk())
                .andReturn();
        Long memberId = readMemberId(result);

        // when & then
        mockMvc.perform(get("/api/v1/members/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "member/public-profile",
                        ResourceSnippetParameters.builder()
                                .summary("공개 회원 프로필을 조회한다.")
                                .description("인증 없이 회원의 공개 프로필만 조회한다.")
                                .pathParameters(parameterWithName("memberId").description("회원 ID"))
                                .responseFields(
                                        fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("회원 ID"),
                                        fieldWithPath("data.nickname").type(JsonFieldType.STRING)
                                                .description("회원 닉네임"),
                                        fieldWithPath("data.gender").type(JsonFieldType.STRING).description("성별")
                                                .optional(),
                                        fieldWithPath("data.ageGroup").type(JsonFieldType.STRING).description("연령대")
                                                .optional(),
                                        fieldWithPath("data.description").type(JsonFieldType.STRING).description("회원 설명")
                                                .optional(),
                                        fieldWithPath("data.avatarAssetId").type(JsonFieldType.NUMBER)
                                                .description("프로필 이미지 asset ID").optional()
                                )
                ));
    }

    @Test
    @DisplayName("사용 중인 닉네임으로 프로필을 수정하면 충돌 오류를 반환한다")
    void rejectsDuplicateNickname() throws Exception {
        // given
        MvcResult first = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer nickname-owner"))
                .andExpect(status().isOk())
                .andReturn();
        String nickname = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(),
                "$.data.nickname");
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer nickname-requester"))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer nickname-requester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + nickname + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("M104"));
    }

    @Test
    @DisplayName("회원 탈퇴 시 로그인 식별자만 정리하고 공개 프로필을 닫는다")
    void deletesMemberAndClearsLoginIdentity() throws Exception {
        // given
        String token = "withdraw-subject";
        MvcResult result = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Long memberId = readMemberId(result);
        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gender": "FEMALE",
                                  "ageGroup": "TWENTIES",
                                  "description": "새로운 맛집을 찾습니다.",
                                  "avatarAssetId": 101
                                }
                                """))
                .andExpect(status().isOk());

        // when
        mockMvc.perform(delete("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/api/v1/members/" + memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("M101"));
        Map<String, Object> member = jdbcTemplate.queryForMap(
                "SELECT supabase_subject, nickname, email, gender, age_group, description, avatar_asset_id, deleted_at "
                        + "FROM orca.member WHERE id = ?", memberId);
        org.assertj.core.api.Assertions.assertThat(member.get("supabase_subject")).isNull();
        org.assertj.core.api.Assertions.assertThat(member.get("nickname")).isEqualTo("탈퇴회원_" + memberId);
        org.assertj.core.api.Assertions.assertThat(member.get("email")).isEqualTo("user@example.com");
        org.assertj.core.api.Assertions.assertThat(member.get("gender")).isEqualTo("FEMALE");
        org.assertj.core.api.Assertions.assertThat(member.get("age_group")).isEqualTo("TWENTIES");
        org.assertj.core.api.Assertions.assertThat(member.get("description")).isEqualTo("새로운 맛집을 찾습니다.");
        org.assertj.core.api.Assertions.assertThat(((Number) member.get("avatar_asset_id")).longValue()).isEqualTo(101L);
        org.assertj.core.api.Assertions.assertThat(member.get("deleted_at")).isNotNull();
    }

    @Test
    @DisplayName("필수 JWT claim이 없으면 인증을 거부한다")
    void rejectsJwtWithoutEmail() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer missing-email"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("M002"));
    }

    @Test
    @DisplayName("일반 회원이 관리자 API에 접근하면 인가를 거부한다")
    void rejectsNonAdminForAdminApi() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/v1/admin/members").header("Authorization", "Bearer normal-member"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("M003"));
    }

    @Test
    @DisplayName("인증 토큰이 없으면 회원 API 요청을 거부한다")
    void rejectsUnauthenticatedRequest() throws Exception {
        // given

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized()).andReturn();

        // then
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .contains("\"code\":\"M002\"");
    }

    @TestConfiguration
    static class TestJwtConfiguration {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                Map<String, Object> claims = new HashMap<>();
                claims.put("iss", "http://127.0.0.1:54321/auth/v1");
                claims.put("sub", token);
                claims.put("aud", "authenticated");
                if (!token.equals("missing-email")) {
                    claims.put("email", "user@example.com");
                }
                claims.put("app_metadata", Map.of("provider", token.equals("subject-a") ? "google" : "kakao"));
                claims.put("iat", Instant.now().minusSeconds(10));
                claims.put("exp", Instant.now().plusSeconds(300));
                return Jwt.withTokenValue(token).headers(headers -> headers.put("alg", "RS256"))
                        .claims(values -> values.putAll(claims)).build();
            };
        }
    }

    private Long readMemberId(MvcResult result) throws Exception {
        Number memberId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return memberId.longValue();
    }
}
