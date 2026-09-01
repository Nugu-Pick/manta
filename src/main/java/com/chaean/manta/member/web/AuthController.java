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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest request) {
		AuthTokenPair tokens = authTokenCommandService.rotateRefreshToken(
			findCookie(request, AuthProperties.REFRESH_COOKIE_NAME), Instant.now());

		return ResponseEntity.ok()
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.header(HttpHeaders.PRAGMA, "no-cache")
			.header("Referrer-Policy", "no-referrer")
			.header(HttpHeaders.SET_COOKIE,
				refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresAt()).toString())
			.body(ApiResponse.of(AuthTokenResponse.from(tokens)));
	}

	@PostMapping("/signup/complete")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> completeSignup(
		@Valid @RequestBody SignupCompleteRequest request,
		HttpServletRequest httpRequest,
		HttpServletResponse response) {
		try {
			AuthTokenPair tokens = signupCommandService.complete(
				findCookie(httpRequest, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME),
				SignupCommand.of(request.legalDocumentIds(), request.gender(), request.ageGroup()), Instant.now());

			return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.header(HttpHeaders.PRAGMA, "no-cache")
				.header("Referrer-Policy", "no-referrer")
				.header(HttpHeaders.SET_COOKIE,
					refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresAt()).toString())
				.header(HttpHeaders.SET_COOKIE, clearCookie(AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString())
				.body(ApiResponse.of(AuthTokenResponse.from(tokens)));
		} catch (BusinessException exception) {
			if (exception.errorCode() == ErrorCode.AUTH_SIGNUP_CONTEXT_INVALID) {
				response.addHeader(HttpHeaders.SET_COOKIE,
					clearCookie(AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString());
			}
			throw exception;
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authTokenCommandService.revokeRefreshToken(
			findCookie(request, AuthProperties.REFRESH_COOKIE_NAME), Instant.now());

		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, clearCookie(AuthProperties.REFRESH_COOKIE_NAME).toString())
			.build();
	}

	private ResponseCookie refreshTokenCookie(String value, Instant expiresAt) {
		long maxAge = Math.max(expiresAt.getEpochSecond() - Instant.now().getEpochSecond(), 0);
		return ResponseCookie.from(AuthProperties.REFRESH_COOKIE_NAME, value)
			.httpOnly(true)
			.secure(true)
			.path("/api/v1/auth")
			.sameSite("Lax")
			.maxAge(maxAge)
			.build();
	}

	private ResponseCookie clearCookie(String name) {
		return ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(true)
			.path("/api/v1/auth")
			.sameSite("Lax")
			.maxAge(0)
			.build();
	}

	private String findCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return null;
		}

		for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
