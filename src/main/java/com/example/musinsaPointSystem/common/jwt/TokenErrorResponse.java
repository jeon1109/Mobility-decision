package com.example.musinsaPointSystem.common.jwt;

public record TokenErrorResponse(String code, String message) {
	public static TokenErrorResponse of(TokenErrorCode code) {
		return new TokenErrorResponse(code.name(), code.message());
	}
}
