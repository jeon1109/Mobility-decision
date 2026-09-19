package com.example.musinsaPointSystem.data.evidence;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FreshnessPolicy {
	private final EvidenceFreshnessProperties properties;
	private final Clock clock;

	public FreshnessPolicy(EvidenceFreshnessProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public FreshnessStatus evaluate(String observedAt, EvidenceType type) {
		Instant observed = parseObservedAt(observedAt);
		if (observed == null) {
			return FreshnessStatus.UNKNOWN;
		}

		Instant now = clock.instant();
		if (observed.isAfter(now.plus(properties.getFutureTolerance()))) {
			return FreshnessStatus.UNKNOWN;
		}
		return observed.isBefore(now.minus(properties.allowedAge(type)))
			? FreshnessStatus.STALE
			: FreshnessStatus.FRESH;
	}

	Instant parseObservedAt(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value).toInstant();
		} catch (DateTimeParseException ignored) {
			try {
				return Instant.parse(value);
			} catch (DateTimeParseException invalid) {
				return null;
			}
		}
	}
}
