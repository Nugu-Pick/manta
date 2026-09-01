package com.chaean.manta.member.internal.security;

import java.io.IOException;
import java.time.Instant;

import com.chaean.manta.member.adapter.OAuth2ProfileUser;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.OAuthCommandService;
import com.chaean.manta.member.internal.application.SignupContextCodec;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.OAuthLoginResult;
import com.chaean.manta.member.internal.application.model.SignupContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
public final class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final OAuthCommandService oauthCommandService;
	private final SignupContextCodec signupContextCodec;
	private final AuthProperties properties;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		if (!(authentication.getPrincipal() instanceof OAuth2User user)
			|| !(user instanceof OAuth2ProfileUser profileUser)) {
			throw new ServletException("Manta OAuth principal이 없습니다.");
		}

		try {
			OAuthLoginResult result = oauthCommandService.completeOAuthLogin(profileUser.profile(), Instant.now());
			if (result.requiresSignup()) {
				SignupContext context = result.signupContext();
				addCookie(response, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME, signupContextCodec.encode(context),
					secondsUntil(context.expiresAt()));
				clearCookie(response, AuthProperties.REFRESH_COOKIE_NAME);
				redirect(response, properties.frontendSignupCallbackUri());
				return;
			}

			AuthTokenPair tokens = result.tokens();
			addCookie(response, AuthProperties.REFRESH_COOKIE_NAME, tokens.refreshToken(),
				secondsUntil(tokens.refreshTokenExpiresAt()));
			clearCookie(response, AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME);
			redirect(response, properties.frontendLoginCallbackUri());
		} finally {
			invalidateSession(request);
		}
	}

	private void redirect(HttpServletResponse response, String uri) throws IOException {
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.sendRedirect(uri);
	}

	private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
		response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(true)
			.sameSite("Lax")
			.path("/api/v1/auth")
			.maxAge(Math.max(maxAgeSeconds, 0))
			.build()
			.toString());
	}

	private void clearCookie(HttpServletResponse response, String name) {
		addCookie(response, name, "", 0);
	}

	private void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	private long secondsUntil(Instant expiresAt) {
		return Math.max(expiresAt.getEpochSecond() - Instant.now().getEpochSecond(), 0);
	}
}
