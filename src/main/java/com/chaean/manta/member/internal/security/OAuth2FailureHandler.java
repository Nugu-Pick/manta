package com.chaean.manta.member.internal.security;

import java.io.IOException;
import java.util.UUID;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.filter.TraceIdFilter;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.web.AuthCookies;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Slf4j
public final class OAuth2FailureHandler implements AuthenticationFailureHandler {

	private final AuthProperties properties;

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception) throws IOException {
		try {
			handleFailureResponse(response, exception);
		} finally {
			invalidateSession(request);
		}
	}

	public void handleFailure(HttpServletResponse response, RuntimeException exception) throws IOException {
		handleFailureResponse(response, exception);
	}

	private void handleFailureResponse(HttpServletResponse response, RuntimeException exception) throws IOException {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		if (traceId == null) {
			traceId = UUID.randomUUID().toString();
		}
		String category = exception instanceof BusinessException ? "business"
			: exception instanceof AuthenticationException ? "authentication" : "internal";
		// 예외 원문과 provider 응답에는 토큰·개인정보가 포함될 수 있어 출력하지 않는다.
		log.warn("OAuth 처리 실패: category={}, exception={}, traceId={}",
			category, exception.getClass().getSimpleName(), traceId);
		if (response.isCommitted()) {
			return;
		}
		response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
		response.addHeader(HttpHeaders.SET_COOKIE,
			AuthCookies.clear(AuthProperties.SIGNUP_CONTEXT_COOKIE_NAME).toString());
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		response.setHeader(HttpHeaders.PRAGMA, "no-cache");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.sendRedirect(UriComponentsBuilder.fromUriString(properties.frontendErrorCallbackUri())
			.queryParam("error", "oauth_failed")
			.build()
			.toUriString());
	}

	private void invalidateSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}
}
