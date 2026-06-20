package com.example.musinsaPointSystem.common.jwt;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.musinsaPointSystem.redis.config.TokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	private final TokenProvider tokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String jwt = resolveToken(request);
		String path = request.getRequestURI();
		boolean hasJwt = StringUtils.hasText(jwt);
		boolean valid = hasJwt && tokenProvider.validateToken(jwt);

		if (hasJwt) {
			log.info(
				"[JWT-REDIS] JwtFilter — path={}, Authorization JWT 있음, 길이={}자, validateToken={} (Redis 조회 없음, JWT 서명/만료만 검사)",
				path, jwt.length(), valid);
		} else {
			log.debug("[JWT-REDIS] JwtFilter — path={}, JWT 없음", path);
		}

		if (valid && SecurityContextHolder.getContext().getAuthentication() == null
			&& tokenProvider.validateToken(jwt)) {

			Authentication authentication = tokenProvider.getAuthentication(jwt);

			// SecurityContextHolder에 추가하기
			SecurityContextHolder.getContext().setAuthentication(authentication);
			log.info("[JWT-REDIS] JwtFilter — SecurityContext 에 Authentication 설정 완료 principal={}",
				authentication.getName());
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}
