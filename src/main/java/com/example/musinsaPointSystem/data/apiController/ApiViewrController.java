package com.example.musinsaPointSystem.data.apiController;

import com.example.musinsaPointSystem.data.apiService.SeoulAreaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiViewrController {
    private final SeoulAreaService seoulAreaService;

    public ApiViewrController(SeoulAreaService seoulAreaService) {
        this.seoulAreaService = seoulAreaService;
    }

    @GetMapping("/main")
    public String index(Model model) {

        model.addAttribute(
                "areas",
                seoulAreaService.getAreas()
        );

        return "auth/doro/doro";
    }

    @GetMapping("/")
    public String login(Model model) {
        model.addAttribute("message", "Hello, Thymeleaf!");
        // URL(/login)과 동일한 뷰 이름("login")을 쓰면 InternalResourceView 가 /login 으로
        // 다시 forward 하며 Circular view path 예외가 날 수 있음 → auth/login 으로 분리
        return "auth/doro/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("message", "Hello, Thymeleaf!");
        return "auth/doro/signup";
    }
}
