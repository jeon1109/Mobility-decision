package com.example.musinsaPointSystem.data.apiService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilityAgentService {
	private final ChatClient chatClient;
	private final MobilityPerformanceMetrics metrics;

	public MobilityAgentService(
		ChatClient.Builder chatClientBuilder,
		MobilityPerformanceMetrics metrics) {
		this.chatClient = chatClientBuilder.build();
		this.metrics = metrics;
	}

	public String recommend(MobilityContext context, String cityDataSummary) {
		PromptParts prompt = metrics.record("prompt.build", () -> buildPrompt(context, cityDataSummary));

		try {
			ChatResponse response = metrics.record("spring-ai.total", () -> chatClient.prompt()
				.system(prompt.system())
				.user(prompt.user())
				.call()
				.chatResponse());

			logUsage(response);
			return response.getResult().getOutput().getText();
		} catch (Exception e) {
			log.error("AI 추천 실패", e);
			throw e;
		}
	}

	private PromptParts buildPrompt(MobilityContext context, String cityDataSummary) {
		String system = """
			당신은 상황 기반 이동 의사결정 AI입니다.

			사용자의 상태와 이동 목적을 분석하고,
			제공된 도시 환경 데이터를 근거로 사용하세요.

			단순 정보 조회가 아니라
			이동 전략을 결정해야 합니다.

			반드시 JSON만 출력하세요.

			{
			  "recommendation": "...",
			  "reason": "...",
			  "usedData": ["..."],
			  "priority": ["..."],
			  "warning": "..."
			}
			""";

		String user = """
			사용자 상태: %s
			이동 목적: %s
			현재 지역: %s
			서울시 실시간 도시데이터:
			%s

			가장 적절한 이동 전략을 추천하세요.
			""".formatted(
			context.condition(),
			context.purpose(),
			context.areaName(),
			cityDataSummary
		);
		return new PromptParts(system, user);
	}

	private void logUsage(ChatResponse response) {
		Usage usage = response.getMetadata().getUsage();
		if (usage == null) {
			log.info("AI 추천 성공, model={}, tokenUsage=unavailable", response.getMetadata().getModel());
			return;
		}
		log.info(
			"AI 추천 성공, model={}, inputTokens={}, outputTokens={}, totalTokens={}",
			response.getMetadata().getModel(),
			usage.getPromptTokens(),
			usage.getCompletionTokens(),
			usage.getTotalTokens()
		);
	}

	private record PromptParts(String system, String user) {
	}
}
