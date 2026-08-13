package com.example.musinsaPointSystem.redis.config;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.common.jwt.TokenErrorCode;
import com.example.musinsaPointSystem.common.jwt.TokenException;
import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.redis.repository.TokenStore;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenProvider {
	private static final String AUTHORITIES_KEY = "auth";
	private static final String REFRESH_MARKER = "isRefreshToken";
	private final Key key;
	private final TokenStore tokenStore;
	private final Duration accessTtl;
	private final Duration refreshTtl;

	public TokenProvider(@Value("${jwt.secret}") String secretKey,
		@Value("${jwt.access-token-minutes}") long accessTokenMinutes,
		@Value("${jwt.refresh-token-days}") long refreshTokenDays,
		TokenStore tokenStore) {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
		}
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.tokenStore = tokenStore;
		this.accessTtl = Duration.ofMinutes(accessTokenMinutes);
		this.refreshTtl = Duration.ofDays(refreshTokenDays);
	}

	public TokenDto generateTokenDto(Authentication authentication) {
		String authorities = authentication.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));
		Instant now = Instant.now();
		Instant accessExpiration = now.plus(accessTtl);
		Instant refreshExpiration = now.plus(refreshTtl);

		String accessToken = Jwts.builder()
			.setId(UUID.randomUUID().toString())
			.setSubject(authentication.getName())
			.claim(AUTHORITIES_KEY, authorities)
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(accessExpiration))
			.signWith(key)
			.compact();
		String refreshToken = Jwts.builder()
			.setId(UUID.randomUUID().toString())
			.setSubject(authentication.getName())
			.claim(AUTHORITIES_KEY, authorities)
			.claim(REFRESH_MARKER, true)
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(refreshExpiration))
			.signWith(key)
			.compact();

		return TokenDto.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.accessTokenExpiresIn(accessExpiration.toEpochMilli())
			.refreshToken(refreshToken)
			.refreshTokenExpiresIn(refreshTtl.toMillis())
			.build();
	}

	public Claims parseAccessToken(String token) {
		try {
			Claims claims = parseClaims(token);
			if (Boolean.TRUE.equals(claims.get(REFRESH_MARKER, Boolean.class))) {
				throw new TokenException(TokenErrorCode.INVALID_TOKEN);
			}
			return claims;
		} catch (ExpiredJwtException e) {
			throw new TokenException(TokenErrorCode.TOKEN_EXPIRED);
		} catch (TokenException e) {
			throw e;
		} catch (JwtException | IllegalArgumentException e) {
			throw new TokenException(TokenErrorCode.INVALID_TOKEN);
		}
	}

	public Claims parseRefreshToken(String token) {
		try {
			Claims claims = parseClaims(token);
			if (!Boolean.TRUE.equals(claims.get(REFRESH_MARKER, Boolean.class))) {
				throw new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN);
			}
			return claims;
		} catch (TokenException e) {
			throw e;
		} catch (JwtException | IllegalArgumentException e) {
			throw new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	public void assertNotBlacklisted(String token, Claims claims) {
		if (tokenStore.isBlacklisted(tokenIdentifier(token, claims))) {
			throw new TokenException(TokenErrorCode.TOKEN_BLACKLISTED);
		}
	}

	public Authentication getAuthentication(Claims claims) {
		String authoritiesClaim = claims.get(AUTHORITIES_KEY, String.class);
		if (authoritiesClaim == null || authoritiesClaim.isBlank()) {
			throw new TokenException(TokenErrorCode.INVALID_TOKEN);
		}
		Collection<? extends GrantedAuthority> authorities = Arrays.stream(authoritiesClaim.split(","))
			.map(SimpleGrantedAuthority::new)
			.toList();
		User principal = new User(claims.getSubject(), "", authorities);
		return new UsernamePasswordAuthenticationToken(principal, "", authorities);
	}

	public Authentication getAuthenticationFromRefreshToken(String refreshToken) {
		return getAuthentication(parseRefreshToken(refreshToken));
	}

	public boolean validateTokenWithoutBlacklist(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	public long getExpiration(String token) {
		return parseAccessToken(token).getExpiration().getTime() - System.currentTimeMillis();
	}

	public String getSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public String tokenIdentifier(String token, Claims claims) {
		return claims.getId() != null ? claims.getId() : sha256(token);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
	}

	private static String sha256(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}
}
