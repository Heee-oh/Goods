package com.goods.market.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp,
        String path
) {

    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(true, data, null, Instant.now(), path);
    }

    public static <T> ApiResponse<T> failure(
            String code,
            String message,
            List<FieldErrorItem> fieldErrors,
            String path
    ) {
        return new ApiResponse<>(
                false,
                null,
                new ApiError(code, message, fieldErrors),
                Instant.now(),
                path
        );
    }


    public static <T> ApiResponse<T> failure(
            String code,
            String message,
            String path
    ) {
        return new ApiResponse<>(
                false,
                null,
                new ApiError(code, message, null),
                Instant.now(),
                path
        );
    }
}
