package com.example.musinsaPointSystem.data.apiController;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.common.IdempotencyUtil;
import com.example.musinsaPointSystem.data.apiService.RecommendationService;
import com.example.musinsaPointSystem.data.apiService.SeoulAreaService;
import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.dto.MobilityDecision;
import com.example.musinsaPointSystem.dto.SeoulArea;

import lombok.RequiredArgsConstructor;

/**
 * React SPA용 REST API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MobilityRestController {

	private final SeoulAreaService seoulAreaService;
	private final RecommendationService recommendationService;
	private final IdempotencyUtil idempotencyUtil;

	@GetMapping(value = "/areas", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<SeoulArea> getAreas() {
		return seoulAreaService.getAreas();
	}

	@PostMapping(
		value = "/mobility-decisions",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<?> createMobilityDecision(@RequestBody MobilityContext context) {
		try {
			String requestKey = idempotencyUtil.createRequestKey(
				context.condition(),
				context.purpose(),
				context.areaCode()
			);

			MobilityDecision decision = recommendationService.recommend(requestKey, context);
			return ResponseEntity.ok(decision);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(
				Map.of(
					"message", "AI 안내를 생성하는 중 문제가 발생했습니다.",
					"error", e.getMessage()
				)
			);
		}
	}
}
