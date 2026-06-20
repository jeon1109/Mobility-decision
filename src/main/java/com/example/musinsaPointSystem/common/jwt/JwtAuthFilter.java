package com.example.musinsaPointSystem.common.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {
	private final JwtService jwtService;

	public JwtAuthFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain)
		throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
			// 실제 JWT만 남깁니다.
			String token = header.substring(7);

			try {
				Claims claims = jwtService.parseAccess(token).getBody();
				String userId = claims.getSubject(); // userId

				if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
					== null) {
					// 권한 체크
					List<GrantedAuthority> authorities =
						List.of(new SimpleGrantedAuthority("USER"));
					// “이 사용자가 인증됐다”는 스프링 시큐리티 표준 객체
					var auth = new UsernamePasswordAuthenticationToken(
						userId, null, authorities);

					// 이 요청은 “인증됨(authenticated)” 상태로 취급됩니다.
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			} catch (ExpiredJwtException e) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} catch (Exception e) {
				// 위조/서명 오류
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}
