package com.example.musinsaPointSystem.data.apiService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.config.SeoulCityDataTool;
import com.example.musinsaPointSystem.dto.MobilityContext;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilityAgentService {
	private final ChatClient chatClient;
	private final SeoulCityDataTool seoulCityDataTool;

	private final ObservationRegistry registry;

	String result = "";
	long start = System.currentTimeMillis();

	public MobilityAgentService(
		ChatClient.Builder chatClientBuilder,
		SeoulCityDataTool seoulCityDataTool,
		ObservationRegistry registry) {
		this.chatClient = chatClientBuilder.build();
		this.seoulCityDataTool = seoulCityDataTool;
		this.registry = registry;
	}

	public String recommend(MobilityContext context) {

		String chatParam = """
			당신은 상황 기반 이동 의사결정 AI입니다.

			사용자의 상태와 이동 목적을 분석하고,
			필요한 경우 Tool을 사용하여
			도시 환경 데이터를 조회하세요.

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

		String userChatParm = """
			사용자 상태: %s
			이동 목적: %s
			현재 지역: %s

			가장 적절한 이동 전략을 추천하세요.
			""";

		try {
			result = Observation.createNotStarted("call.recommend", registry)
				.observe(() -> chatClient.prompt()
					.system(chatParam)
					.user(userChatParm.formatted(
						context.condition(),
						context.purpose(),
						context.areaName(),
						context.areaCode()
					))
					.tools(seoulCityDataTool)
					.call()
					.content());

			log.info("AI 추천 성공, 실행시간={}ms", System.currentTimeMillis() - start);

		} catch (Exception e) {
			log.error("AI 추천 실패, 실행시간={}ms", System.currentTimeMillis() - start, e);
			throw e;
		}

		return result;
	}
}
