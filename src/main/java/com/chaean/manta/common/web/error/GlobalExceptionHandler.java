package com.chaean.manta.common.web.error;

import com.chaean.manta.common.web.filter.TraceIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusinessException(BusinessException exception, HttpServletRequest request) {
		return createProblemDetail(exception.errorCode(), exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
		HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(error -> FieldErrorResponse.of(error.getField(), error.getDefaultMessage()))
			.toList();

		return createProblemDetail(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultDetail(),
			request, fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
			.map(error -> FieldErrorResponse.of(error.getPropertyPath().toString(), error.getMessage()))
			.toList();

		return createProblemDetail(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultDetail(),
			request, fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleMalformedRequest(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return createProblemDetail(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultDetail(),
			request, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
		return createProblemDetail(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.defaultDetail(),
			request, List.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ProblemDetail handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
		ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;

		return createProblemDetail(errorCode, errorCode.defaultDetail(), request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
		ErrorCode errorCode = ErrorCode.ADMIN_ACCESS_DENIED;

		return createProblemDetail(errorCode, errorCode.defaultDetail(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
		String path = request.getRequestURI();
		log.error("처리하지 않은 예외가 발생했습니다. traceId={}, path={}", traceId(), path, exception);

		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

		return createProblemDetail(errorCode, errorCode.defaultDetail(), request, List.of());
	}

	private ProblemDetail createProblemDetail(ApiErrorCode errorCode, String detail, HttpServletRequest request,
		List<FieldErrorResponse> fieldErrors) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		problemDetail.setTitle(errorCode.title());
		problemDetail.setType(errorCode.type());
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", errorCode.code());
		problemDetail.setProperty("traceId", traceId());
		problemDetail.setProperty("fieldErrors", fieldErrors);

		return problemDetail;
	}

	private String traceId() {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		return traceId == null ? UUID.randomUUID().toString() : traceId;
	}
}
