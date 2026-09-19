package com.example.musinsaPointSystem.data.decision.infrastructure;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

@Component
public class ExternalCallExecutor {
	private final CircuitBreakerRegistry circuitBreakers;
	private final RetryRegistry retries;

	public ExternalCallExecutor() {
		this.circuitBreakers = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
			.failureRateThreshold(50)
			.slidingWindowSize(10)
			.minimumNumberOfCalls(5)
			.waitDurationInOpenState(Duration.ofSeconds(30))
			.recordException(this::retryable)
			.build());
		this.retries = RetryRegistry.of(RetryConfig.custom()
			.maxAttempts(2)
			.waitDuration(Duration.ofMillis(250))
			.retryOnException(this::retryable)
			.build());
	}

	public <T> T execute(String provider, Supplier<T> call) {
		CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker(provider);
		Retry retry = retries.retry(provider);
		return Retry.decorateSupplier(retry,
			CircuitBreaker.decorateSupplier(circuitBreaker, call)).get();
	}

	private boolean retryable(Throwable error) {
		if (error instanceof WebClientResponseException response) {
			int status = response.getStatusCode().value();
			return status == 429 || status >= 500;
		}
		return true;
	}
}
