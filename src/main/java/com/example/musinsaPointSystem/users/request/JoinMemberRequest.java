package com.example.musinsaPointSystem.users.request;

import com.example.musinsaPointSystem.users.entity.Users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class JoinMemberRequest {
	public interface Join {}
	public interface Login {}

	@NotBlank(groups = Join.class, message = "이름을 입력해주세요.")
	@Size(max = 50, groups = Join.class, message = "이름은 50자 이하여야 합니다.")
	private String name;
	@NotBlank(groups = {Join.class, Login.class}, message = "이메일을 입력해주세요.")
	@Email(groups = {Join.class, Login.class}, message = "올바른 이메일 형식이 아닙니다.")
	@Size(max = 254, groups = {Join.class, Login.class}, message = "이메일이 너무 깁니다.")
	private String email;
	@NotBlank(groups = {Join.class, Login.class}, message = "비밀번호를 입력해주세요.")
	@Size(max = 100, groups = {Join.class, Login.class}, message = "비밀번호가 너무 깁니다.")
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
