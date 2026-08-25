package com.example.musinsaPointSystem.data.apiController;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.data.apiService.MobilityV2RecommendationService;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Request;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class MobilityV2RestController {
	private final MobilityV2RecommendationService recommendationService;

	@PostMapping(value = "/mobility-decisions", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MobilityV2Response> create(@Valid @RequestBody MobilityV2Request request) {
		return ResponseEntity.ok(recommendationService.recommend(request));
	}
}
