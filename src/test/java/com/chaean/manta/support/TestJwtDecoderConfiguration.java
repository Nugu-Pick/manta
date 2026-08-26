package com.chaean.manta.support;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class TestJwtDecoderConfiguration {

    @Bean
    JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .headers(headers -> headers.put("alg", "RS256"))
                .claims(claims -> {
                    claims.put("iss", "http://127.0.0.1:54321/auth/v1");
                    claims.put("sub", token);
                    claims.put("aud", "authenticated");
                    claims.put("email", "user@example.com");
                    claims.put("app_metadata", Map.of("provider", "test"));
                    claims.put("iat", Instant.now().minusSeconds(10));
                    claims.put("exp", Instant.now().plusSeconds(300));
                })
                .build();
    }
}
