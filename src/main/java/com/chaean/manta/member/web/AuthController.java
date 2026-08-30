package com.chaean.manta.member.web;

import java.net.URI;
import java.time.Instant;

import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.AuthTokenCommandService;
import com.chaean.manta.member.internal.application.OAuthCommandService;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthAuthorization;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.web.dto.response.AuthTokenResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final OAuthCommandService oauthCommandService;
	private final AuthTokenCommandService authTokenCommandService;
	private final AuthProperties authProperties;

	@GetMapping("/oauth/{provider}")
	public ResponseEntity<Void> startOAuth(@PathVariable String provider) {
		OAuthAuthorization authorization = oauthCommandService.startOAuthAuthorization(
			OAuthProvider.from(provider));

		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(authorization.authorizationUrl()))
			.header(HttpHeaders.SET_COOKIE,
				oauthCookie(authProperties.oauthStateCookieName(), authorization.state(),
					authProperties.oauthAuthorizationTtl().toSeconds()).toString())
			.header(HttpHeaders.SET_COOKIE,
				oauthCookie(authProperties.oauthVerifierCookieName(), authorization.codeVerifier(),
					authProperties.oauthAuthorizationTtl().toSeconds()).toString())
			.build();
	}

	@GetMapping("/oauth/{provider}/callback")
	public ResponseEntity<Void> completeOAuthCallback(
		@PathVariable String provider,
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String state,
		HttpServletRequest request,
		HttpServletResponse response) {
		boolean callbackCompleted = false;

		try {
			AuthTokenPair tokens = oauthCommandService.completeOAuthCallback(
				OAuthProvider.from(provider),
				code,
				state,
				findCookie(request, authProperties.oauthStateCookieName()),
				findCookie(request, authProperties.oauthVerifierCookieName()),
				Instant.now());

			ResponseEntity<Void> callbackResponse = ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(authProperties.frontendCallbackUri()))
				.header(HttpHeaders.SET_COOKIE,
					refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresAt()).toString())
				.header(HttpHeaders.SET_COOKIE,
					oauthCookie(authProperties.oauthStateCookieName(), "", 0).toString())
				.header(HttpHeaders.SET_COOKIE,
					oauthCookie(authProperties.oauthVerifierCookieName(), "", 0).toString())
				.header(HttpHeaders.CACHE_CONTROL, "no-store")
				.header(HttpHeaders.PRAGMA, "no-cache")
				.header("Referrer-Policy", "no-referrer")
				.build();

			callbackCompleted = true;

			return callbackResponse;
		} finally {
			if (!callbackCompleted) {
				response.addHeader(HttpHeaders.SET_COOKIE,
					oauthCookie(authProperties.oauthStateCookieName(), "", 0).toString());
				response.addHeader(HttpHeaders.SET_COOKIE,
					oauthCookie(authProperties.oauthVerifierCookieName(), "", 0).toString());
			}
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest request) {
		AuthTokenPair tokens = authTokenCommandService.rotateRefreshToken(
			findCookie(request, authProperties.refreshCookieName()), Instant.now());

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE,
				refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenExpiresAt()).toString())
			.body(ApiResponse.of(AuthTokenResponse.from(tokens)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authTokenCommandService.revokeRefreshToken(
			findCookie(request, authProperties.refreshCookieName()), Instant.now());

		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie("", Instant.now()).toString())
			.build();
	}

	private ResponseCookie refreshTokenCookie(String value, Instant expiresAt) {
		return ResponseCookie.from(authProperties.refreshCookieName(), value)
			.httpOnly(true)
			.secure(true)
			.path("/api/v1/auth")
			.sameSite("Lax")
			.maxAge(Math.max(expiresAt.getEpochSecond() - Instant.now().getEpochSecond(), 0))
			.build();
	}

	private ResponseCookie oauthCookie(String name, String value, long maxAgeSeconds) {
		return ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(true)
			.path("/api/v1/auth")
			.sameSite("Lax")
			.maxAge(Math.max(maxAgeSeconds, 0))
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
