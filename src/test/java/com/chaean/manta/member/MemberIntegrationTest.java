package com.chaean.manta.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;

@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(MemberIntegrationTest.TestJwtConfiguration.class)
class MemberIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                claims.put("email", "user@example.com");
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
