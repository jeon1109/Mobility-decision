package com.example.musinsaPointSystem.common;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.common.error.PublicDataException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AiCommunicationException.class)
    public String handleAiException(
            AiCommunicationException e,
            Model model
    ) {
        log.warn("AI 통신 오류");

        model.addAttribute("errorTitle", "AI 응답 생성 실패");
        model.addAttribute("errorMessage", "AI 안내를 생성하는 중 문제가 발생했습니다.");
        model.addAttribute("detailMessage", e.getMessage());

        return "auth/doro/error";
    }

    @ExceptionHandler(PublicDataException.class)
    public String handlePublicDataException(
            PublicDataException e,
            Model model
    ) {
        log.warn("공공데이터 조회 오류");

        model.addAttribute("errorTitle", "도시 데이터 조회 실패");
        model.addAttribute("errorMessage", "서울시 실시간 데이터를 불러오지 못했습니다.");
        model.addAttribute("detailMessage", e.getMessage());

        return "auth/doro/error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(
            IllegalArgumentException e,
            Model model
    ) {
        log.warn("잘못된 요청", e);

        model.addAttribute("errorTitle", "잘못된 요청");
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("detailMessage", null);

        return "auth/doro/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception e,
            Model model
    ) {
        log.error("예상치 못한 서버 오류", e);

        model.addAttribute("errorTitle", "오류가 발생했습니다");
        model.addAttribute("errorMessage", "잠시 후 다시 시도해주세요.");
        model.addAttribute("detailMessage", null);

        return "auth/doro/error";
    }
}
