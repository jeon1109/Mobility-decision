package com.example.musinsaPointSystem.users.request;

import com.example.musinsaPointSystem.users.entity.Users;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class JoinMemberRequest {
	@NotNull
	private String name;
	@NotNull
	private String email;
	@NotNull
	private String password;

	public Users toEntity() {
		return Users.builder()
			.email(email)
			.name(name)
			.password(password)
			.delYn(false)
			.build();
	}
}
