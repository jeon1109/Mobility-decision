package com.example.musinsaPointSystem.performance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CorrelationIdFilterTest {

	@Test
	void preservesSafeCorrelationIdAndMeasuresWholeRequest() throws Exception {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		CorrelationIdFilter filter = new CorrelationIdFilter(new MobilityPerformanceMetrics(registry));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/mobility-decisions");
		request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-123");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) -> {
		});

		assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("request-123");
		assertThat(registry.get(MobilityPerformanceMetrics.STAGE_TIMER)
			.tag("stage", "request.total")
			.tag("outcome", "success")
			.timer().count()).isEqualTo(1);
	}

	@Test
	void replacesUnsafeCorrelationIdToPreventLogInjection() throws Exception {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		CorrelationIdFilter filter = new CorrelationIdFilter(new MobilityPerformanceMetrics(registry));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/areas");
		request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad\nvalue");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) -> {
		});

		assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
			.matches("[0-9a-f-]{36}")
			.doesNotContain("\n");
	}

	@Test
	void recordsServerFailureAsError() throws Exception {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		CorrelationIdFilter filter = new CorrelationIdFilter(new MobilityPerformanceMetrics(registry));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/mobility-decisions");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) ->
			((MockHttpServletResponse) servletResponse).setStatus(503)
		);

		assertThat(registry.get(MobilityPerformanceMetrics.STAGE_TIMER)
			.tag("stage", "request.total")
			.tag("outcome", "error")
			.timer().count()).isEqualTo(1);
	}

	@Test
	void measuresV2MobilityRequest() throws Exception {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		CorrelationIdFilter filter = new CorrelationIdFilter(new MobilityPerformanceMetrics(registry));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/mobility-decisions");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (servletRequest, servletResponse) -> {
		});

		assertThat(registry.get(MobilityPerformanceMetrics.STAGE_TIMER)
			.tag("stage", "request.total")
			.tag("outcome", "success")
			.timer().count()).isEqualTo(1);
	}
}
