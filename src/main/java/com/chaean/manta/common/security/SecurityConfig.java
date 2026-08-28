package com.chaean.manta.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http, Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> jwtAuthenticationConverter,
		ObjectMapper objectMapper) throws Exception {
		ApiAuthenticationEntryPoint authenticationEntryPoint = new ApiAuthenticationEntryPoint(objectMapper);
		ApiAccessDeniedHandler accessDeniedHandler = new ApiAccessDeniedHandler(objectMapper);

		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health/**", "/scalar", "/openapi3.yaml", "/error").permitAll()
				.requestMatchers("/api/v1/members/**").permitAll()
				.requestMatchers("/api/v1/legal-documents/**").permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/**").authenticated()
				.anyRequest().permitAll())
			.oauth2ResourceServer(resourceServer -> resourceServer
				.authenticationEntryPoint(authenticationEntryPoint)
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}
}
