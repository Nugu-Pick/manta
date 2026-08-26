package com.chaean.manta.common.security;

import java.io.IOException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import tools.jackson.databind.ObjectMapper;

public final class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        ErrorCode errorCode = ErrorCode.ADMIN_ACCESS_DENIED;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status(), errorCode.defaultDetail());
        problemDetail.setTitle(errorCode.title());
        problemDetail.setType(errorCode.type());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", errorCode.code());
        problemDetail.setProperty("traceId", traceId());
        problemDetail.setProperty("fieldErrors", List.<FieldErrorResponse>of());
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId == null ? UUID.randomUUID().toString() : traceId;
    }
}
