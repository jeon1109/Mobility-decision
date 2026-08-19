package com.example.musinsaPointSystem.common.jwt;

public enum TokenErrorCode {
	TOKEN_BLACKLISTED("로그아웃된 토큰입니다."),
	TOKEN_EXPIRED("만료된 토큰입니다."),
	INVALID_TOKEN("유효하지 않은 토큰입니다."),
	INVALID_REFRESH_TOKEN("유효하지 않은 Refresh Token입니다."),
	REDIS_UNAVAILABLE("인증 저장소를 사용할 수 없습니다."),
	UNAUTHORIZED("인증이 필요합니다.");

	private final String message;

	TokenErrorCode(String message) {
		this.message = message;
	}

	public String message() {
		return message;
	}
}
