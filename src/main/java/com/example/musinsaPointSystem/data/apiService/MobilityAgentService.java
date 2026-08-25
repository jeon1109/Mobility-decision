package com.example.musinsaPointSystem.data.apiService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilityAgentService {
	private final ChatClient chatClient;
	private final MobilityPerformanceMetrics metrics;

	public MobilityAgentService(ChatClient.Builder builder, MobilityPerformanceMetrics metrics) {
		this.chatClient = builder.build();
		this.metrics = metrics;
	}

	public String recommend(MobilityContext context, String cityDataSummary) {
		String system = """
			당신은 상황 기반 이동 의사결정 AI입니다. 제공된 관측 데이터 외의 날씨, 경로,
			거리, 소요시간을 추정하지 마세요. 반드시 recommendation, reason, usedData,
			priority, warning 필드를 갖는 JSON 객체만 출력하세요.
			""";
		String user = "사용자 상태: %s\n이동 목적: %s\n현재 지역: %s\n관측 데이터:\n%s"
			.formatted(context.condition(), context.purpose(), context.areaName(), cityDataSummary);
		try {
			ChatResponse response = metrics.record("spring-ai.total", () -> chatClient.prompt()
				.system(system).user(user).call().chatResponse());
			Usage usage = response.getMetadata().getUsage();
			log.info("AI 추천 성공, model={}, totalTokens={}", response.getMetadata().getModel(),
				usage == null ? "unavailable" : usage.getTotalTokens());
			return response.getResult().getOutput().getText();
		} catch (RuntimeException e) {
			throw new AiCommunicationException("AI 안내 생성에 실패했습니다.", e);
		}
	}
}
