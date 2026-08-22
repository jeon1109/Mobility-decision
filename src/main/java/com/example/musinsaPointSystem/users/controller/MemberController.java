package com.example.musinsaPointSystem.users.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest.Join;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest.Login;
import com.example.musinsaPointSystem.users.response.MemberResponse;
import com.example.musinsaPointSystem.users.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class MemberController {
	private final UserService userService;

	public MemberController(UserService userService) {
		this.userService = userService;
	}

	// 회원 가입 데이터 처리 메소드
	@PostMapping(value = "/v1/member/join", produces = "application/json")
	public MemberResponse join(@Validated(Join.class) @RequestBody JoinMemberRequest request) {

		return userService.memberJoin(request);
	}

	// 회원 가입 데이터 처리 메소드
	@PostMapping(value = "/v1/member/login", produces = "application/json")
	public TokenDto login(@Validated(Login.class) @RequestBody JoinMemberRequest request) {

		return userService.searchLogin(request);
	}

	// 회원 가입 데이터 처리 메소드
	@PostMapping(value = "/v1/member/reissue", produces = "application/json")
	public TokenDto reissue(HttpServletRequest httpServletRequest) {
		String tokens = httpServletRequest.getHeader("Authorization");

		return userService.resolveRefreshToken(tokens);
	}

}
