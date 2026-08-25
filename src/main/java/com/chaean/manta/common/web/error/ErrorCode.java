package com.chaean.manta.common.web.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode implements ApiErrorCode {
    VALIDATION_FAILED("M001", "Validation failed", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_REQUIRED("M002", "Authentication required", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ADMIN_ACCESS_DENIED("M003", "Access denied", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("M004", "Resource not found", "요청한 리소스를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("M005", "Internal server error",
            "서버에서 처리할 수 없는 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

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
