package com.example.musinsaPointSystem.redis.repository;

import java.time.Duration;

import org.springframework.stereotype.Repository;

import com.example.musinsaPointSystem.redis.config.RedisKeyPrefix;
import com.example.musinsaPointSystem.redis.utils.RedisUtil;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {
	private final RedisUtil redis;

	@Override
	public void saveRefreshToken(String userId, String refreshToken, Duration ttl) {
		redis.setValue(RedisKeyPrefix.REFRESH_TOKEN + userId, refreshToken, ttl.toMillis(),
			java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	@Override
	public String findRefreshToken(String userId) {
		return redis.getValue(RedisKeyPrefix.REFRESH_TOKEN + userId);
	}

	@Override
	public void deleteRefreshToken(String userId) {
		redis.delete(RedisKeyPrefix.REFRESH_TOKEN + userId);
	}

	@Override
	public void blacklist(String tokenId, Duration ttl) {
		redis.setValue(RedisKeyPrefix.BLACKLIST_ACCESS_TOKEN + tokenId, "logout",
			ttl.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean isBlacklisted(String tokenId) {
		return redis.hasKey(RedisKeyPrefix.BLACKLIST_ACCESS_TOKEN + tokenId);
	}
}
