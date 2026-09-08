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
import com.chaean.manta.member.web.AuthCookies;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@RequiredArgsConstructor
public final class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final OAuthCommandService oauthCommandService;
	private final SignupContextCodec signupContextCodec;
	private final AuthProperties properties;
	private final OAuth2FailureHandler failureHandler;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {
		try {
			OAuthLoginResult result;
			ResponseCookie cookie;
			try {
				if (!(authentication.getPrincipal() instanceof OAuth2ProfileUser profileUser)) {
					throw new BadCredentialsException("OAuth principal이 없습니다.");
				}
				result = oauthCommandService.completeOAuthLogin(profileUser.profile(), Instant.now());
				if (result.requiresSignup()) {
					SignupContext context = result.signupContext();
					cookie = AuthCookies.create(AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME,
						signupContextCodec.encode(context), context.expiresAt(), Instant.now());
				} else {
					AuthTokenPair tokens = result.tokens();
					cookie = AuthCookies.create(AuthProperties.REFRESH_COOKIE_NAME, tokens.refreshToken(),
						tokens.refreshTokenExpiresAt(), Instant.now());
				}
			} catch (RuntimeException exception) {
				failureHandler.handleFailure(response, exception);
				return;
			}

			// 업무 결과를 준비한 뒤 응답을 기록한다. 응답 전송 오류는 재시도하지 않는다.
			response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			response.addHeader(HttpHeaders.SET_COOKIE, AuthCookies.clear(result.requiresSignup()
				? AuthProperties.REFRESH_COOKIE_NAME : AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString());
			redirect(response, result.requiresSignup()
				? properties.frontendSignupCallbackUri() : properties.frontendLoginCallbackUri());
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

	private void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
