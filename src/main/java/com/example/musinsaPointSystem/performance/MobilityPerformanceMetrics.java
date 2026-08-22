package com.example.musinsaPointSystem.performance;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MobilityPerformanceMetrics {

	public static final String STAGE_TIMER = "mobility.decision.stage.duration";
	public static final String CACHE_REQUESTS = "mobility.decision.cache.requests";
	public static final String TOOL_CALLS = "mobility.decision.tool.calls";
	public static final String PUBLIC_API_REQUESTS = "mobility.decision.public-api.requests";

	private static final Logger log = LoggerFactory.getLogger("mobility.performance");

	private final MeterRegistry meterRegistry;

	public MobilityPerformanceMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public <T> T record(String stage, Supplier<T> action) {
		long startedAt = System.nanoTime();
		boolean success = false;
		try {
			T result = action.get();
			success = true;
			return result;
		} finally {
			record(stage, success ? "success" : "error", System.nanoTime() - startedAt);
		}
	}

	public void record(String stage, Runnable action) {
		record(stage, () -> {
			action.run();
			return null;
		});
	}

	public void recordDuration(String stage, String outcome, long durationNanos) {
		record(stage, outcome, durationNanos);
	}

	public void recordCacheRequest(boolean hit) {
		Counter.builder(CACHE_REQUESTS)
			.description("Mobility decision cache lookup count")
			.tag("cache", "in-memory")
			.tag("result", hit ? "hit" : "miss")
			.register(meterRegistry)
			.increment();
	}

	public void recordToolCall(String tool, String outcome) {
		Counter.builder(TOOL_CALLS)
			.description("Spring AI tool call count")
			.tag("tool", tool)
			.tag("outcome", outcome)
			.register(meterRegistry)
			.increment();
	}

	public void recordPublicApiRequest(String outcome, boolean fallback) {
		Counter.builder(PUBLIC_API_REQUESTS)
			.description("Mobility public API request count")
			.tag("provider", "seoul-city-data")
			.tag("outcome", outcome)
			.tag("cache_hit", "false")
			.tag("fallback", Boolean.toString(fallback))
			.tag("retry_count", "0")
			.register(meterRegistry)
			.increment();
	}

	private void record(String stage, String outcome, long durationNanos) {
		Timer.builder(STAGE_TIMER)
			.description("Mobility decision processing duration by stage")
			.tag("stage", stage)
			.tag("outcome", outcome)
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry)
			.record(durationNanos, TimeUnit.NANOSECONDS);

		log.info(
			"correlationId={} stage={} outcome={} durationMs={}",
			MDC.get("correlationId"),
			stage,
			outcome,
			TimeUnit.NANOSECONDS.toMillis(durationNanos)
		);
	}
}
