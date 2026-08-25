package com.example.musinsaPointSystem.data.apiController;

import java.util.List;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

	@PostMapping(value = "/mobility-decisions", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MobilityDecision> createMobilityDecision(@Valid @RequestBody MobilityContext context) {
		if (context.areaCode() == null || context.areaCode().isBlank()) {
			throw new IllegalArgumentException("areaCode는 필수입니다.");
		}
		String requestKey = idempotencyUtil.createRequestKey(
			context.condition(), context.purpose(), context.areaCode());
		return ResponseEntity.ok(recommendationService.recommend(requestKey, context));
	}
}
