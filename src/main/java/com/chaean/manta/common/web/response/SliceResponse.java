package com.chaean.manta.common.web.response;

import java.util.List;
import java.util.Objects;

/**
 * 사용자 목록의 cursor pagination 응답이다.
 */
public record SliceResponse<T>(List<T> items, String nextCursor, boolean hasNext) {

    public SliceResponse {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }

    public static <T> SliceResponse<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new SliceResponse<>(items, nextCursor, hasNext);
    }
}
