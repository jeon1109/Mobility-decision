package com.example.musinsaPointSystem.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.redis.utils.RedisUtil;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest;
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
	public MemberResponse join(@RequestBody JoinMemberRequest request) {

		return userService.memberJoin(request);
	}

	// 회원 가입 데이터 처리 메소드
	@PostMapping(value = "/v1/member/login", produces = "application/json")
	public TokenDto login(@RequestBody JoinMemberRequest request) {

		return userService.searchLogin(request);
	}

	// 회원 가입 데이터 처리 메소드
	@PostMapping(value = "/v1/member/reissue", produces = "application/json")
	public TokenDto reissue(HttpServletRequest httpServletRequest) {
		String tokens = httpServletRequest.getHeader("Authorization");

		return userService.resolveRefreshToken(tokens);
	}

	// 회원 가입 데이터 처리 메소드
	@GetMapping(value = "/v1/member/logout", produces = "application/json")
	public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
		String accessToken = RedisUtil.extractToken(authorizationHeader);
		userService.logout(accessToken);
		return ResponseEntity.ok().build();
	}
}
