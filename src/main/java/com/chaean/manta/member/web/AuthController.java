package com.chaean.manta.member.web;

import java.time.Instant;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.AuthTokenCommandService;
import com.chaean.manta.member.internal.application.SignupCommandService;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.SignupCommand;
import com.chaean.manta.member.web.dto.request.SignupCompleteRequest;
import com.chaean.manta.member.web.dto.response.AuthTokenResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthTokenCommandService authTokenCommandService;
	private final SignupCommandService signupCommandService;
	private final AuthProperties properties;

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
		@CookieValue(name = AuthProperties.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
		AuthTokenPair tokens = authTokenCommandService.rotateRefreshToken(refreshToken, Instant.now());

		return ResponseEntity.ok()
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.header(HttpHeaders.PRAGMA, "no-cache")
			.header("Referrer-Policy", "no-referrer")
			.header(HttpHeaders.SET_COOKIE,
				AuthCookies.create(properties, AuthProperties.REFRESH_COOKIE_NAME, tokens.refreshToken(),
					tokens.refreshTokenExpiresAt(), Instant.now()).toString())
			.body(ApiResponse.of(AuthTokenResponse.from(tokens)));
	}

	@PostMapping("/signup/complete")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> completeSignup(
		@Valid @RequestBody SignupCompleteRequest request,
		@CookieValue(name = AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME, required = false) String signupContext,
		HttpServletResponse response) {
		try {
			AuthTokenPair tokens = signupCommandService.complete(
				signupContext,
				SignupCommand.of(request.legalDocumentIds(), request.gender(), request.ageGroup()), Instant.now());

			return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.header(HttpHeaders.PRAGMA, "no-cache")
				.header("Referrer-Policy", "no-referrer")
				.header(HttpHeaders.SET_COOKIE,
					AuthCookies.create(properties, AuthProperties.REFRESH_COOKIE_NAME, tokens.refreshToken(),
						tokens.refreshTokenExpiresAt(), Instant.now()).toString())
				.header(HttpHeaders.SET_COOKIE, AuthCookies.clear(properties, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString())
				.body(ApiResponse.of(AuthTokenResponse.from(tokens)));
		} catch (BusinessException exception) {
			if (exception.errorCode() == ErrorCode.AUTH_SIGNUP_CONTEXT_INVALID) {
				response.addHeader(HttpHeaders.SET_COOKIE,
					AuthCookies.clear(properties, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString());
			}
			throw exception;
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		@CookieValue(name = AuthProperties.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
		authTokenCommandService.revokeRefreshToken(refreshToken, Instant.now());

		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, AuthCookies.clear(properties, AuthProperties.REFRESH_COOKIE_NAME).toString())
			.build();
	}
}
