package com.example.musinsaPointSystem.redis.entity;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@RedisHash(value = "MemberToken", timeToLive = 3600 * 24 * 14)
@AllArgsConstructor
@Getter
@Setter
public class redis {
	@Id
	private String email;
	private String refreshToken;
}
