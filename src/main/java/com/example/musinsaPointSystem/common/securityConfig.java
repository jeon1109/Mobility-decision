package com.example.musinsaPointSystem.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.musinsaPointSystem.common.jwt.JwtFilter;
import com.example.musinsaPointSystem.redis.exception.JwtAccessDeniedHandler;
import com.example.musinsaPointSystem.redis.exception.JwtAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class securityConfig {
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final String allowedOrigins;
	private final JwtFilter jwtFilter;

	public securityConfig(JwtAccessDeniedHandler jwtAccessDeniedHandler,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtFilter jwtFilter,
		@Value("${app.cors.allowed-origins}") String allowedOrigins) {
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtFilter = jwtFilter;
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
		@Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource)
		throws Exception {
		// 수많은 필터 중 CSRF 필터를 disable 시킴
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.csrf(csrf -> csrf.disable())
			.exceptionHandling(exception -> {
				exception.accessDeniedHandler(jwtAccessDeniedHandler);
				exception.authenticationEntryPoint(jwtAuthenticationEntryPoint);
			})
			// stateless로 세션을 넘겨주는걸로 생성
			.sessionManagement(sessionManagement ->
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			);
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/error").permitAll()
			// React SPA 정적 리소스 및 클라이언트 라우트
			.requestMatchers(
				"/",
				"/index.html",
				"/assets/**",
				"/favicon.svg",
				"/icons.svg",
				"/login",
				"/signup",
				"/main",
				"/loading",
				"/result",
				"/chatroom"
			).permitAll()
			.requestMatchers("/api/v1/auth/logout").authenticated()
			.requestMatchers("/api/**", "/v1/**", "/join/**",
				"/views/**", "/v3/api-docs/**", "/swagger-ui/**", "/chat/**", "/ws/**",
				"/swagger-ui.html", "/actuator/health", "/actuator/health/**", "/actuator/info")
			.permitAll()
			// .requestMatchers("/admin/**").hasAuthority("ADMIN")  // URL 보안: 관리자 페이지
			.anyRequest().authenticated()
		);

		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 패스워드 암호화처리
		return new BCryptPasswordEncoder();
	}

	@Bean(name = "corsConfigurationSource")
	CorsConfigurationSource corsConfigurationSource() {
		var config = new org.springframework.web.cors.CorsConfiguration();
		config.setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList());
		config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(java.util.List.of("*"));
		config.setExposedHeaders(java.util.List.of("X-Correlation-Id"));
		config.setAllowCredentials(true);

		var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
