package com.example.musinsaPointSystem.common.jwt;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.users.entity.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final SecretKey accessKey;
	private final SecretKey refreshKey;
	private final int accessExpMinutes;
	private final int refreshExpDays;

	public JwtService(@Value("${jwt.secret}") String accessSecret,
		@Value("${jwt.refresh}") String refreshSecret,
		@Value("${jwt.access-token-minutes}") int accessExpMinutes,
		@Value("${jwt.refresh-token-days}") int refreshExpDays) {
		this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes());
		this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes());
		this.accessExpMinutes = accessExpMinutes;
		this.refreshExpDays = refreshExpDays;
	}

	public String generateAccess(Users user) {
		Instant now = Instant.now(); // 날짜 오늘자

		return Jwts.builder()
			.setSubject(String.valueOf(user.getUid()))
			.claim("email", user.getEmail())
			.setIssuedAt(Date.from(now))
			.setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000L))//토큰유효기간(10분)
			.signWith(accessKey, SignatureAlgorithm.HS256)
			.compact();
	}

	public String generateRefresh(Users user) {
		Instant now = Instant.now();
		long nowMil = System.currentTimeMillis();
		long expMillis = nowMil + 10L * 24 * 60 * 60 * 1000; // 10일

		return Jwts.builder()
			.setSubject(String.valueOf(user.getUid()))
			.claim("typ", "refresh")
			.setIssuedAt(Date.from(now))
			.setExpiration(new Date(expMillis)) //토큰유효기간(10일)
			.signWith(refreshKey, SignatureAlgorithm.HS256)
			.compact();

	}

	public Jws<Claims> parseAccess(String token) {
		return Jwts.parser().setSigningKey(accessKey).build().parseClaimsJws(token);
	}

	public Jws<Claims> parseRefresh(String token) {
		return Jwts.parser().setSigningKey(refreshKey).build().parseClaimsJws(token);
	}
}
