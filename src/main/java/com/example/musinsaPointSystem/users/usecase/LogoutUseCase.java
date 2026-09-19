package com.example.musinsaPointSystem.users.usecase;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.common.jwt.TokenErrorCode;
import com.example.musinsaPointSystem.common.jwt.TokenException;
import com.example.musinsaPointSystem.redis.config.TokenProvider;
import com.example.musinsaPointSystem.redis.repository.TokenStore;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogoutUseCase {
	private final TokenProvider tokenProvider;
	private final TokenStore tokenStore;

	public void execute(String accessToken) {
		Claims claims = tokenProvider.parseAccessToken(accessToken);
		tokenProvider.assertNotBlacklisted(accessToken, claims);
		long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
		if (remainingMillis <= 0) {
			throw new TokenException(TokenErrorCode.TOKEN_EXPIRED);
		}
		tokenStore.revokeSession(tokenProvider.tokenIdentifier(accessToken, claims), claims.getSubject(),
			Duration.ofMillis(remainingMillis));
	}
}
