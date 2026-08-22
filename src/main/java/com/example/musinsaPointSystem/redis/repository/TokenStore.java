package com.example.musinsaPointSystem.redis.repository;

import java.time.Duration;

public interface TokenStore {
	void saveRefreshToken(String userId, String refreshToken, Duration ttl);
	String findRefreshToken(String userId);
	void deleteRefreshToken(String userId);
	void blacklist(String tokenId, Duration ttl);
	boolean isBlacklisted(String tokenId);
}
