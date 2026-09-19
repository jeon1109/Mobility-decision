package com.example.musinsaPointSystem.data.apiController;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.data.location.PlaceSearchUseCase;
import com.example.musinsaPointSystem.data.location.model.PlaceCandidate;
import com.example.musinsaPointSystem.data.location.model.ResolvedLocation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceSearchRestController {
	private final PlaceSearchUseCase placeSearchUseCase;

	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<PlaceCandidate> search(@RequestParam(name = "q") String query) {
		return placeSearchUseCase.search(query);
	}

	@PostMapping(value = "/resolve", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResolvedLocation resolve(@RequestBody PlaceCandidate candidate) {
		return placeSearchUseCase.resolve(candidate);
	}

	@PostMapping(value = "/reverse", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResolvedLocation reverse(@Valid @RequestBody CoordinatesRequest request) {
		return placeSearchUseCase.resolveCoordinates(request.latitude(), request.longitude());
	}

	public record CoordinatesRequest(
		@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude
	) {
	}
}
