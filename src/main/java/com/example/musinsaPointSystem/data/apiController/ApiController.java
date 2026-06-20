package com.example.musinsaPointSystem.data.apiController;

import com.example.musinsaPointSystem.common.IdempotencyUtil;
import com.example.musinsaPointSystem.data.apiService.RecommendationService;
import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.dto.MobilityDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {
    private final RecommendationService recommendationService;
    private final IdempotencyUtil idempotencyUtil;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    @PostMapping("/recommend")
    public String recommend(
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "purpose", required = false) String purpose,
            @RequestParam(value = "areaName", required = false) String areaName,
            @RequestParam(value = "areaCode", required = false) String areaCode,
            Model model
    ) {

        MobilityContext context =
                new MobilityContext(
                        condition,
                        purpose,
                        areaName,
                        areaCode
                );

        String requestKey =
                idempotencyUtil.createRequestKey(
                        condition,
                        purpose,
                        areaCode
                );

        MobilityDecision decision =
                recommendationService.recommend(
                        requestKey,
                        context
                );

        model.addAttribute("decision", decision);
        model.addAttribute("areaName", areaName);

        return "auth/doro/result";
    }

    @PostMapping("/vector")
    public String vector() {
        List<Document> documents = List.of(
                new Document("Redis는 Refresh Token 저장에 사용할 수 있다."),
                new Document("Redis는 로그아웃 블랙리스트와 중복 로그인 방지에 사용할 수 있다."),
                new Document("RabbitMQ는 주문, 결제, 포인트 적립 같은 비동기 메시지 처리에 사용한다."),
                new Document("Webhook은 GitHub, 결제사, Slack 같은 외부 서비스가 내 서버로 이벤트를 알려줄 때 사용한다.")
        );

        vectorStore.add(documents);

        return "RAG 문서 인덱싱 완료";
    }

    @GetMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public String ask(@RequestParam(name = "question") String question) {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();

        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

}
