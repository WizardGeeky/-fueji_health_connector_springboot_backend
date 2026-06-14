package com.healthconnector.app.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

/**
 * Generic success response wrapper for all API responses.
 *
 * @param <T> the response data type
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	@Builder.Default
	private boolean success = true;

	@Builder.Default
	private Instant timestamp = Instant.now();

	private String message;

	private T data;

	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder().data(data).build();
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return ApiResponse.<T>builder().message(message).data(data).build();
	}

	public static <T> ApiResponse<T> success(String message) {
		return ApiResponse.<T>builder().message(message).build();
	}
}
