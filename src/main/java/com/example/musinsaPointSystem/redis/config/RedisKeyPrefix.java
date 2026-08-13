package com.example.musinsaPointSystem.redis.config;

public final class RedisKeyPrefix {
	public static final String REFRESH_TOKEN = "refresh:";
	public static final String BLACKLIST_ACCESS_TOKEN = "blacklist:";
	public static final String IDEMPOTENCY = "idempotency:";

	private RedisKeyPrefix() {
	}
}
