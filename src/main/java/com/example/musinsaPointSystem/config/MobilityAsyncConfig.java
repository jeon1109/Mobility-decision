package com.example.musinsaPointSystem.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MobilityAsyncConfig {
	@Bean(destroyMethod = "close")
	ExecutorService mobilityIoExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}
