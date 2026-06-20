package com.example.musinsaPointSystem.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.observation.ObservationRegistry;

@Configuration
public class ObservationConfig {
	@Bean
	public ObservationRegistry observationRegistry() {
		return ObservationRegistry.create();
	}
}
