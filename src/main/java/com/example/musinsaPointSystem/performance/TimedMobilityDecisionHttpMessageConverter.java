package com.example.musinsaPointSystem.performance;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;

import com.example.musinsaPointSystem.dto.MobilityDecision;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TimedMobilityDecisionHttpMessageConverter extends MappingJackson2HttpMessageConverter {

	private final MobilityPerformanceMetrics metrics;

	public TimedMobilityDecisionHttpMessageConverter(
		ObjectMapper objectMapper,
		MobilityPerformanceMetrics metrics
	) {
		super(objectMapper);
		this.metrics = metrics;
	}

	@Override
	protected void writeInternal(
		Object object,
		@Nullable Type type,
		HttpOutputMessage outputMessage
	) throws IOException, HttpMessageNotWritableException {
		if (!(object instanceof MobilityDecision)) {
			super.writeInternal(object, type, outputMessage);
			return;
		}

		long startedAt = System.nanoTime();
		boolean success = false;
		try {
			super.writeInternal(object, type, outputMessage);
			success = true;
		} finally {
			metrics.recordDuration(
				"response.serialize",
				success ? "success" : "error",
				System.nanoTime() - startedAt
			);
		}
	}
}
