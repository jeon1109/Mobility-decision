package com.example.musinsaPointSystem.redis.repository;

import java.time.Duration;

public interface TokenStore {
	void saveRefreshToken(String userId, String refreshToken, Duration ttl);
	String findRefreshToken(String userId);
	void deleteRefreshToken(String userId);
	void blacklist(String tokenId, Duration ttl);
	void revokeSession(String tokenId, String userId, Duration ttl);
	boolean isBlacklisted(String tokenId);
}
