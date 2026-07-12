package com.example.musinsaPointSystem.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.common.error.PublicDataException;

import lombok.extern.slf4j.Slf4j;

/**
 * React SPA용 JSON 예외 응답.
 */
@Slf4j
@RestControllerAdvice
public class RestApiExceptionHandler {

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
}
