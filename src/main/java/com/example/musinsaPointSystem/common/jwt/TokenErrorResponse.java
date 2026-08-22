package com.example.musinsaPointSystem.common.jwt;

import java.util.Map;

import org.slf4j.MDC;

public record TokenErrorResponse(String code, String message, Map<String, String> fieldErrors,
	String correlationId) {
	public static TokenErrorResponse of(TokenErrorCode code) {
		return new TokenErrorResponse(code.name(), code.message(), Map.of(), MDC.get("correlationId"));
	}
}
