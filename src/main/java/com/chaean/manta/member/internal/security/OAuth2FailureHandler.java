package com.chaean.manta.member.internal.security;

import java.io.IOException;

import com.chaean.manta.member.config.AuthProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
public final class OAuth2FailureHandler implements AuthenticationFailureHandler {

	private final AuthProperties properties;

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception) throws IOException {
		try {
			clearCookie(response, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME);
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
			response.setHeader(HttpHeaders.PRAGMA, "no-cache");
			response.setHeader("Referrer-Policy", "no-referrer");
			response.sendRedirect(UriComponentsBuilder.fromUriString(properties.frontendErrorCallbackUri())
				.queryParam("error", "oauth_failed")
				.build()
				.toUriString());
		} finally {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
		}
	}

	private void clearCookie(HttpServletResponse response, String name) {
		response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/api/v1/auth")
			.maxAge(0)
			.build()
			.toString());
	}
}
