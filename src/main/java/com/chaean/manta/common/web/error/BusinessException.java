package com.chaean.manta.common.web.error;

import java.util.Objects;

public final class BusinessException extends RuntimeException {

	private final ApiErrorCode errorCode;

	private BusinessException(ApiErrorCode errorCode, String detail) {
		super(detail);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
	}

	public static BusinessException of(ApiErrorCode errorCode) {
		return new BusinessException(errorCode, errorCode.defaultDetail());
	}

	public static BusinessException of(ApiErrorCode errorCode, String detail) {
		return new BusinessException(errorCode, detail);
	}

	public ApiErrorCode errorCode() {
		return errorCode;
	}
}
