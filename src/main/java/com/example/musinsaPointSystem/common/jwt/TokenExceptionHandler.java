package com.example.musinsaPointSystem.common.jwt;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class TokenExceptionHandler {
	@ExceptionHandler(TokenException.class)
	public ResponseEntity<TokenErrorResponse> handleToken(TokenException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(TokenErrorResponse.of(exception.getCode()));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<TokenErrorResponse> handleRedis(DataAccessException exception) {
		log.error("Redis authentication store operation failed: {}",
			exception.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(TokenErrorResponse.of(TokenErrorCode.REDIS_UNAVAILABLE));
	}
}
