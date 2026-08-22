package com.example.musinsaPointSystem.performance;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class MobilityPerformanceWebConfig implements WebMvcConfigurer {

	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;

	public MobilityPerformanceWebConfig(
		ObjectMapper objectMapper,
		MobilityPerformanceMetrics metrics
	) {
		this.objectMapper = objectMapper;
		this.metrics = metrics;
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(0, new TimedMobilityDecisionHttpMessageConverter(objectMapper, metrics));
	}
}
