package com.example.musinsaPointSystem.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.common.error.DuplicateEmailException;
import com.example.musinsaPointSystem.common.error.InvalidCredentialsException;
import com.example.musinsaPointSystem.common.error.MemberNotFoundException;
import com.example.musinsaPointSystem.common.error.PublicDataException;
import com.example.musinsaPointSystem.data.apiService.SeoulAreaService.AreaNotFoundException;
import com.example.musinsaPointSystem.data.location.PlaceProviderException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class RestApiExceptionHandler {
	@ExceptionHandler(AreaNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleAreaNotFound(AreaNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse.of("AREA_NOT_FOUND", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(error ->
			fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(
			ApiErrorResponse.of("VALIDATION_ERROR", "요청 값을 확인해주세요.", fields));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(ApiErrorResponse.of(
			"INVALID_REQUEST_BODY", "요청 JSON 또는 enum 값이 올바르지 않습니다."));
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(
			"DUPLICATE_EMAIL", e.getMessage(), Map.of("email", "이미 사용 중인 이메일입니다.")));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiErrorResponse.of("INVALID_CREDENTIALS", e.getMessage()));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException e) {
		log.warn("로그인 인증 실패: {}", e.getClass().getSimpleName());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiErrorResponse.of("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."));
	}

	@ExceptionHandler(MemberNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse.of("MEMBER_NOT_FOUND", e.getMessage()));
	}

	@ExceptionHandler(AiCommunicationException.class)
	public ResponseEntity<ApiErrorResponse> handleAiException(AiCommunicationException e) {
		log.warn("AI 통신 또는 응답 검증 실패", e);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiErrorResponse.of(
			"AI_UNAVAILABLE", "AI 안내를 생성하는 중 문제가 발생했습니다."));
	}

	@ExceptionHandler(PublicDataException.class)
	public ResponseEntity<ApiErrorResponse> handlePublicDataException(PublicDataException e) {
		log.warn("공공데이터 조회 오류", e);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.of(
			"PUBLIC_DATA_UNAVAILABLE", "서울시 실시간 데이터를 불러오지 못했습니다."));
	}

	@ExceptionHandler(PlaceProviderException.class)
	public ResponseEntity<ApiErrorResponse> handlePlaceProvider(PlaceProviderException e) {
		log.warn("장소 검색 서비스 오류: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiErrorResponse.of(
			"PLACE_PROVIDER_UNAVAILABLE", e.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException e) {
		log.warn("잘못된 요청: {}", e.getMessage());
		return ResponseEntity.badRequest().body(ApiErrorResponse.of("BAD_REQUEST", e.getMessage()));
	}

	@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
	public ResponseEntity<ApiErrorResponse> handleNotFound(Exception e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiErrorResponse.of("NOT_FOUND", "요청한 API를 찾을 수 없습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleException(Exception e) {
		log.error("예상치 못한 서버 오류", e);
		return ResponseEntity.internalServerError().body(ApiErrorResponse.of(
			"INTERNAL_SERVER_ERROR", "잠시 후 다시 시도해주세요."));
	}
}
