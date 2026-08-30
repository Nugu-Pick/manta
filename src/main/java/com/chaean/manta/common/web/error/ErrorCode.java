package com.chaean.manta.common.web.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements ApiErrorCode {
	// Common
	VALIDATION_FAILED("M001", "Validation failed", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	AUTHENTICATION_REQUIRED("M002", "Authentication required", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
	ADMIN_ACCESS_DENIED("M003", "Access denied", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
	RESOURCE_NOT_FOUND("M004", "Resource not found", "요청한 리소스를 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND),
	INTERNAL_SERVER_ERROR("M005", "Internal server error",
		"서버에서 처리할 수 없는 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

	// Member
	EMAIL_REQUIRED("M100", "Email required", "이메일이 필요합니다.", HttpStatus.UNAUTHORIZED),
	MEMBER_NOT_FOUND("M101", "Member not found", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
	NICKNAME_GENERATION_FAILED("M102", "Nickname generation failed", "닉네임을 생성할 수 없습니다.",
		HttpStatus.CONFLICT),
	NICKNAME_INVALID("M103", "Invalid nickname", "닉네임이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
	NICKNAME_ALREADY_TAKEN("M104", "Nickname already taken", "이미 사용 중인 닉네임입니다.",
		HttpStatus.CONFLICT),
	LEGAL_DOCUMENT_NOT_FOUND("M105", "Legal document not found", "약관 문서를 찾을 수 없습니다.",
		HttpStatus.NOT_FOUND),
	LEGAL_DOCUMENT_NOT_AVAILABLE("M106", "Legal document not available", "현재 동의할 수 없는 약관입니다.",
		HttpStatus.CONFLICT),
	AUTH_PROVIDER_NOT_SUPPORTED("M108", "OAuth provider not supported", "지원하지 않는 OAuth provider입니다.",
		HttpStatus.BAD_REQUEST),
	AUTH_PROVIDER_REQUEST_FAILED("M109", "OAuth provider request failed", "OAuth provider 요청에 실패했습니다.",
		HttpStatus.BAD_GATEWAY),
	AUTH_STATE_INVALID("M110", "Invalid OAuth state", "OAuth 인증 상태가 유효하지 않습니다.",
		HttpStatus.BAD_REQUEST),
	AUTH_PROFILE_INVALID("M111", "Invalid OAuth profile", "OAuth provider 회원 정보를 확인할 수 없습니다.",
		HttpStatus.BAD_GATEWAY),
	REFRESH_TOKEN_INVALID("M113", "Invalid refresh token",
		"Refresh Token이 유효하지 않거나 만료되었습니다.",
		HttpStatus.UNAUTHORIZED),
	ONBOARDING_REQUIRED_AGREEMENT_MISSING("M114", "Onboarding agreement missing",
		"온보딩을 완료하려면 필수 약관에 동의해야 합니다.", HttpStatus.FORBIDDEN),
	ONBOARDING_ALREADY_COMPLETED("M115", "Onboarding already completed", "이미 온보딩을 완료한 회원입니다.",
		HttpStatus.CONFLICT),
	MEMBER_STATUS_NOT_ACTIVE("M116", "Member is not active", "활성화된 회원만 이용할 수 있습니다.",
		HttpStatus.FORBIDDEN);

	private final String code;
	private final String title;
	private final String defaultDetail;
	private final HttpStatus status;

	ErrorCode(String code, String title, String defaultDetail, HttpStatus status) {
		this.code = code;
		this.title = title;
		this.defaultDetail = defaultDetail;
		this.status = status;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public String defaultDetail() {
		return defaultDetail;
	}

	@Override
	public HttpStatus status() {
		return status;
	}
}
