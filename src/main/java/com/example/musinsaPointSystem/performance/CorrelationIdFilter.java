package com.example.musinsaPointSystem.performance;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-Id";
	private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

	private final MobilityPerformanceMetrics metrics;

	public CorrelationIdFilter(MobilityPerformanceMetrics metrics) {
		this.metrics = metrics;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
		String previousCorrelationId = MDC.get("correlationId");
		long startedAt = System.nanoTime();
		boolean completed = false;

		MDC.put("correlationId", correlationId);
		response.setHeader(HEADER_NAME, correlationId);
		try {
			filterChain.doFilter(request, response);
			completed = true;
		} finally {
			if (isMobilityDecisionRequest(request)) {
				metrics.recordDuration(
					"request.total",
					requestOutcome(completed, response.getStatus()),
					System.nanoTime() - startedAt
				);
			}
			if (previousCorrelationId == null) {
				MDC.remove("correlationId");
			} else {
				MDC.put("correlationId", previousCorrelationId);
			}
		}
	}

	private String requestOutcome(boolean completed, int status) {
		if (!completed || status >= 500) {
			return "error";
		}
		if (status >= 400) {
			return "client_error";
		}
		return "success";
	}

	private String resolveCorrelationId(String requestedId) {
		if (requestedId != null && SAFE_CORRELATION_ID.matcher(requestedId).matches()) {
			return requestedId;
		}
		return UUID.randomUUID().toString();
	}

	private boolean isMobilityDecisionRequest(HttpServletRequest request) {
		return "POST".equals(request.getMethod())
			&& ("/api/v1/mobility-decisions".equals(request.getRequestURI())
				|| "/api/v2/mobility-decisions".equals(request.getRequestURI()));
	}
}
