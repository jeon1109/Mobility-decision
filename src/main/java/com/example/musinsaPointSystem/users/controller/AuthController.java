package com.example.musinsaPointSystem.users.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.common.jwt.TokenErrorCode;
import com.example.musinsaPointSystem.common.jwt.TokenException;
import com.example.musinsaPointSystem.users.usecase.LogoutUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final LogoutUseCase logoutUseCase;

	@PostMapping("/logout")
	@Operation(summary = "로그아웃", description = "Access Token을 blacklist에 등록하고 Refresh Token을 삭제합니다.")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Logout successful")
	public ResponseEntity<Map<String, String>> logout(
		@RequestHeader(value = "Authorization", required = false) String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")
			|| authorization.length() == "Bearer ".length()) {
			throw new TokenException(TokenErrorCode.UNAUTHORIZED);
		}
		logoutUseCase.execute(authorization.substring(7));
		return ResponseEntity.ok(Map.of("message", "Logout successful"));
	}
}
