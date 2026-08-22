package com.example.musinsaPointSystem.users.service;

import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest;
import com.example.musinsaPointSystem.users.response.MemberResponse;

public interface UserService {
	MemberResponse memberJoin(JoinMemberRequest request);

	TokenDto searchLogin(JoinMemberRequest request);

	MemberResponse searchJoin(Long memberId);

	TokenDto resolveRefreshToken(String refreshToken);
}
