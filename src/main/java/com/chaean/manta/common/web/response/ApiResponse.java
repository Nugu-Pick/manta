package com.chaean.manta.common.web.response;

/**
 * 단건 또는 일반 성공 응답의 공통 envelope다.
 */
public record ApiResponse<T>(T data) {

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data);
	}
}
