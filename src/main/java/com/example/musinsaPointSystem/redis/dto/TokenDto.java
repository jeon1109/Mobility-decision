package com.example.musinsaPointSystem.redis.dto;

import lombok.Builder;

@Builder
public class TokenDto {
	private String grantType;     // Bearer
	private String accessToken;   // 액세스 토큰
	private String refreshToken;  // 리프레시 토큰
	private Long accessTokenExpiresIn; // 만료 시간(타임스탬프 또는 초)
	private Long refreshTokenExpiresIn; // 만료 시간(타임스탬프 또는 초)

	public TokenDto(String grantType, String accessToken, String refreshToken, Long accessTokenExpiresIn,
		Long refreshTokenExpiresIn) {
		this.grantType = grantType;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.accessTokenExpiresIn = accessTokenExpiresIn;
		this.refreshTokenExpiresIn = refreshTokenExpiresIn;
	}

	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Long getAccessTokenExpiresIn() {
		return accessTokenExpiresIn;
	}

	public void setAccessTokenExpiresIn(Long accessTokenExpiresIn) {
		this.accessTokenExpiresIn = accessTokenExpiresIn;
	}

	public Long getRefreshTokenExpiresIn() {
		return refreshTokenExpiresIn;
	}

	public void setRefreshTokenExpiresIn(Long refreshTokenExpiresIn) {
		this.refreshTokenExpiresIn = refreshTokenExpiresIn;
	}
}
