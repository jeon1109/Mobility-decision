package com.example.musinsaPointSystem.redis.utils;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {
	private final RedisTemplate<String, String> redisTemplate;

	// 저장하기 키와 값을
	public void save(String key, String value) {
		log.info("[JWT-REDIS] Redis SET — key(이메일)={}, 저장값=refresh JWT, 길이={}자 (전체 토큰은 로그에 남기지 않음)",
			key, value != null ? value.length() : 0);
		redisTemplate.opsForValue().set(key, value);
		log.debug("[JWT-REDIS] Redis SET 완료 — key={}", key);
	}

	public void setValue(String key, String value, long timeout, TimeUnit unit) {
		redisTemplate.opsForValue().set(key, value, timeout, unit);
	}

	public String getValue(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	public boolean delete(String key) {
		Boolean result = redisTemplate.delete(key);
		return Boolean.TRUE.equals(result);
	}

	public boolean hasKey(String key) {
		Boolean result = redisTemplate.hasKey(key);
		return Boolean.TRUE.equals(result);
	}

	public static String extractToken(String bearerToken) {
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		throw new RuntimeException("Authorization header가 올바르지 않습니다.");
	}
}
