package com.example.musinsaPointSystem.redis.config;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.redis.utils.RedisUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenProvider {
	private static final String AUTHORITIES_KEY = "auth";
	private static final String BEARER_TYPE = "Bearer";
	private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30;            // 30분
	private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;  // 7일
	private final Key key;
	private final RedisUtil redisUtil;

	public TokenProvider(@Value("${jwt.secret}") String secretKey, RedisUtil redisUtil) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
		this.redisUtil = redisUtil;
	}

	public TokenDto generateTokenDto(Authentication authentication) {
		// 권한들 가져오기
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		String subject = authentication.getName();
		log.info("[JWT-REDIS] TokenProvider — JWT 발급 시작 subject(이메일)={}, authorities={}", subject, authorities);
		log.info(
			"[JWT-REDIS] TokenProvider — Access/Refresh 는 모두 JWT 문자열이며, Redis 저장은 호출부(UserService)에서 refresh 만 수행");

		String accessToken = generateAccessToken(subject, authorities);
		String refreshToken = generateRefreshToken(subject, authorities);

		long now = (new Date()).getTime();

		log.info(
			"[JWT-REDIS] TokenProvider — 발급 완료 access 길이={}자, refresh 길이={}자 (Redis 미사용, redisUtil 은 로그인 후 서비스에서 save 호출)",
			accessToken.length(), refreshToken.length());

		return TokenDto.builder()
			.grantType(BEARER_TYPE)
			.accessToken(accessToken)
			.accessTokenExpiresIn(new Date(now + ACCESS_TOKEN_EXPIRE_TIME).getTime())
			.refreshToken(refreshToken)
			.build();
	}

	private String generateAccessToken(String email, String authorities) {
		long now = (new Date()).getTime();
		Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
		return Jwts.builder()
			.setSubject(email)
			.claim(AUTHORITIES_KEY, authorities)
			.setExpiration(accessTokenExpiresIn)
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

	private String generateRefreshToken(String email, String authorities) {
		long now = (new Date()).getTime();
		return Jwts.builder()
			.setSubject(email)
			.claim(AUTHORITIES_KEY, authorities)
			.setExpiration(new Date(now + REFRESH_TOKEN_EXPIRE_TIME))
			.claim("isRefreshToken", true) // refreshToken 임을 나타내는 클레임 추가
			.signWith(key, SignatureAlgorithm.HS512)
			.compact();
	}

	public Authentication getAuthentication(String accessToken) {
		// 토큰 복호화
		Claims claims = parseClaims(accessToken);

		if (claims.get(AUTHORITIES_KEY) == null) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		// 클레임에서 권한 정보 가져오기
		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		// UserDetails 객체를 만들어서 Authentication 리턴
		UserDetails principal = new User(claims.getSubject(), "", authorities);

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	public boolean validateTokenWithoutBlacklist(String token) {
		try {
			Jwts.parser()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			return false;
		} catch (ExpiredJwtException e) {
			return false;
		} catch (UnsupportedJwtException e) {
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);

			if (isBlacklisted(token)) {
				return false;
			}

			return true;
		} catch (SecurityException | MalformedJwtException e) {
			return false;
		} catch (ExpiredJwtException e) {
			return false;
		} catch (UnsupportedJwtException e) {
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public long getExpiration(String token) {
		Date expiration = parseClaims(token).getExpiration();
		return expiration.getTime() - System.currentTimeMillis();
	}

	public String getSubject(String token) {
		return parseClaims(token).getSubject();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	private boolean isBlacklisted(String token) {
		return redisUtil.hasKey(RedisKeyPrefix.BLACKLIST_ACCESS_TOKEN + token);
	}

	public Authentication getAuthenticationFromRefreshToken(String refreshToken) {
		Claims claims = parseClaims(refreshToken);

		Boolean isRefreshToken = claims.get("isRefreshToken", Boolean.class);
		if (isRefreshToken == null || !isRefreshToken) {
			throw new RuntimeException("Refresh Token이 아닙니다.");
		}

		String userId = claims.getSubject();
		String auth = claims.get("auth", String.class);

		if (auth == null || auth.isBlank()) {
			throw new RuntimeException("권한 정보가 없는 토큰입니다.");
		}

		Collection<? extends GrantedAuthority> authorities =
			Arrays.stream(auth.split(","))
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());

		User principal = new User(userId, "", authorities);

		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}
}
