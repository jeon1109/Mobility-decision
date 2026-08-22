package com.example.musinsaPointSystem.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.common.error.PublicDataException;
import com.example.musinsaPointSystem.data.apiService.MobilityV2RecommendationService.AreaNotFoundException;
import com.example.musinsaPointSystem.data.apiService.MobilityV2RecommendationService.InvalidMobilityV2RequestException;

import lombok.extern.slf4j.Slf4j;

/**
 * React SPA용 JSON 예외 응답.
 */
@Slf4j
@RestControllerAdvice
public class RestApiExceptionHandler {
	@ExceptionHandler(InvalidMobilityV2RequestException.class)
	public ResponseEntity<Map<String, Object>> handleV2BadRequest(InvalidMobilityV2RequestException e) {
		return ResponseEntity.badRequest()
			.body(v2ErrorBody("VALIDATION_ERROR", e.getMessage(), Map.of("availableModes", e.getMessage())));
	}

	@ExceptionHandler(AreaNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleAreaNotFound(AreaNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(v2ErrorBody("AREA_NOT_FOUND", e.getMessage(), Map.of()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
		Map<String, String> fields = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(error ->
			fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest()
			.body(v2ErrorBody("VALIDATION_ERROR", "요청 값을 확인해주세요.", fields));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(v2ErrorBody(
			"INVALID_REQUEST_BODY", "요청 JSON 또는 enum 값이 올바르지 않습니다.", Map.of()));
	}

	@ExceptionHandler(AiCommunicationException.class)
	public ResponseEntity<Map<String, String>> handleAiException(AiCommunicationException e) {
		log.warn("AI 통신 오류", e);
		return ResponseEntity.internalServerError().body(errorBody(
			"AI 응답 생성 실패",
			"AI 안내를 생성하는 중 문제가 발생했습니다.",
			e.getMessage()
		));
	}

	@ExceptionHandler(PublicDataException.class)
	public ResponseEntity<Map<String, String>> handlePublicDataException(PublicDataException e) {
		log.warn("공공데이터 조회 오류", e);
		return ResponseEntity.internalServerError().body(errorBody(
			"도시 데이터 조회 실패",
			"서울시 실시간 데이터를 불러오지 못했습니다.",
			e.getMessage()
		));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
		log.warn("잘못된 요청", e);
		return ResponseEntity.badRequest().body(errorBody(
			"잘못된 요청",
			e.getMessage(),
			null
		));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleException(Exception e) {
		log.error("예상치 못한 서버 오류", e);
		return ResponseEntity.internalServerError().body(errorBody(
			"오류가 발생했습니다",
			"잠시 후 다시 시도해주세요.",
			null
		));
	}

	private Map<String, String> errorBody(String title, String message, String detail) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("errorTitle", title);
		body.put("errorMessage", message);
		if (detail != null) {
			body.put("detailMessage", detail);
		}
		return body;
	}

	private Map<String, Object> v2ErrorBody(String code, String message, Map<String, String> fieldErrors) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("message", message);
		body.put("fieldErrors", fieldErrors);
		body.put("correlationId", MDC.get("correlationId"));
		return body;
	}
}
