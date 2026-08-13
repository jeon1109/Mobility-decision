package com.example.musinsaPointSystem.common.jwt;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.musinsaPointSystem.redis.config.TokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	private final TokenProvider tokenProvider;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Claims claims = tokenProvider.parseAccessToken(token);
			tokenProvider.assertNotBlacklisted(token, claims);
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				Authentication authentication = tokenProvider.getAuthentication(claims);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(request, response);
		} catch (TokenException e) {
			writeError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getCode());
		} catch (DataAccessException e) {
			writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, TokenErrorCode.REDIS_UNAVAILABLE);
		}
	}

	private void writeError(HttpServletResponse response, int status, TokenErrorCode code) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), TokenErrorResponse.of(code));
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		return StringUtils.hasText(header) && header.startsWith("Bearer ") ? header.substring(7) : null;
	}
}
