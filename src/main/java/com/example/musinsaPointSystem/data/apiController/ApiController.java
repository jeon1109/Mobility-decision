package com.example.musinsaPointSystem.data.apiController;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * RAG 등 부가 AI API (JSON 전용).
 * 이동 추천은 {@link MobilityRestController} 를 사용한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

	private final VectorStore vectorStore;
	private final ChatClient.Builder chatClientBuilder;

	@PostMapping("/vector")
	public ResponseEntity<Map<String, String>> vector() {
		List<Document> documents = List.of(
			new Document("Redis는 Refresh Token 저장에 사용할 수 있다."),
			new Document("Redis는 로그아웃 블랙리스트와 중복 로그인 방지에 사용할 수 있다."),
			new Document("RabbitMQ는 주문, 결제, 포인트 적립 같은 비동기 메시지 처리에 사용한다."),
			new Document("Webhook은 GitHub, 결제사, Slack 같은 외부 서비스가 내 서버로 이벤트를 알려줄 때 사용한다.")
		);

		vectorStore.add(documents);

		return ResponseEntity.ok(Map.of("message", "RAG 문서 인덱싱 완료"));
	}

	@GetMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> ask(@RequestParam(name = "question") String question) {
		ChatClient chatClient = chatClientBuilder
			.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
			.build();

		String answer = chatClient.prompt()
			.user(question)
			.call()
			.content();

		return ResponseEntity.ok(Map.of("answer", answer));
	}
}
