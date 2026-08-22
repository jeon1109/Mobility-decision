package com.example.musinsaPointSystem.common;

import java.util.Map;

import org.slf4j.MDC;

public record ApiErrorResponse(
	String code,
	String message,
	Map<String, String> fieldErrors,
	String correlationId
) {
	public static ApiErrorResponse of(String code, String message) {
		return new ApiErrorResponse(code, message, Map.of(), MDC.get("correlationId"));
	}

	public static ApiErrorResponse of(String code, String message, Map<String, String> fieldErrors) {
		return new ApiErrorResponse(code, message, fieldErrors, MDC.get("correlationId"));
	}
}
