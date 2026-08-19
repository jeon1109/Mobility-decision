package com.example.musinsaPointSystem.common.jwt;

public class TokenException extends RuntimeException {
	private final TokenErrorCode code;

	public TokenException(TokenErrorCode code) {
		super(code.message());
		this.code = code;
	}

	public TokenErrorCode getCode() {
		return code;
	}
}
