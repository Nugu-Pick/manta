package com.chaean.manta.common.web.response;

import java.util.List;
import java.util.Objects;

/**
 * 관리자 목록의 offset pagination 응답이다.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

	public PageResponse {
		items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
		if (page < 0) {
			throw new IllegalArgumentException("page must be greater than or equal to zero");
		}
		if (size <= 0) {
			throw new IllegalArgumentException("size must be greater than zero");
		}
		if (totalElements < 0) {
			throw new IllegalArgumentException("totalElements must be greater than or equal to zero");
		}
		if (totalPages < 0) {
			throw new IllegalArgumentException("totalPages must be greater than or equal to zero");
		}
	}

	public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements, int totalPages) {
		return new PageResponse<>(items, page, size, totalElements, totalPages);
	}
}
