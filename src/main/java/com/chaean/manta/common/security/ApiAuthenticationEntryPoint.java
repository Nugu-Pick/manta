package com.chaean.manta.common.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.common.web.error.FieldErrorResponse;
import com.chaean.manta.common.web.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import tools.jackson.databind.ObjectMapper;

public final class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws java.io.IOException {
		ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
		ProblemDetail problemDetail = createProblemDetail(errorCode, request);

		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
	}

	private ProblemDetail createProblemDetail(ErrorCode errorCode, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
			errorCode.status(), errorCode.defaultDetail());
		problemDetail.setTitle(errorCode.title());
		problemDetail.setType(errorCode.type());
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", errorCode.code());
		problemDetail.setProperty("traceId", traceId());
		problemDetail.setProperty("fieldErrors", List.<FieldErrorResponse>of());
		return problemDetail;
	}

	private String traceId() {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		return traceId == null ? UUID.randomUUID().toString() : traceId;
	}
}
