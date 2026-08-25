package com.chaean.manta.common.web.error;

public record FieldErrorResponse(String field, String message) {

    public static FieldErrorResponse of(String field, String message) {
        return new FieldErrorResponse(field, message);
    }
}
