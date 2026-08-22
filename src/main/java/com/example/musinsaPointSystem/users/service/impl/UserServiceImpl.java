package com.example.musinsaPointSystem.users.service.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.musinsaPointSystem.common.error.ApiException;
import com.example.musinsaPointSystem.common.error.DuplicateEmailException;
import com.example.musinsaPointSystem.common.error.InvalidCredentialsException;
import com.example.musinsaPointSystem.common.error.MemberNotFoundException;
import com.example.musinsaPointSystem.common.jwt.TokenErrorCode;
import com.example.musinsaPointSystem.common.jwt.TokenException;
import com.example.musinsaPointSystem.redis.config.RedisKeyPrefix;
import com.example.musinsaPointSystem.redis.config.TokenProvider;
import com.example.musinsaPointSystem.redis.dto.TokenDto;
import com.example.musinsaPointSystem.redis.utils.RedisUtil;
import com.example.musinsaPointSystem.users.enm.BasicResponseMessage;
import com.example.musinsaPointSystem.users.enm.UserRole;
import com.example.musinsaPointSystem.users.entity.Users;
import com.example.musinsaPointSystem.users.repository.UserRepository;
import com.example.musinsaPointSystem.users.request.JoinMemberRequest;
import com.example.musinsaPointSystem.users.response.MemberResponse;
import com.example.musinsaPointSystem.users.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
	private final UserRepository jpaUserRepsitory;
	private final PasswordEncoder passwordEncoder;
	private final TokenProvider tokenProvider;
	private final RedisUtil redisUtil;
	private final AuthenticationManager authenticationManager;

	// 큐 저장소
	public UserServiceImpl(UserRepository jpaUserRepsitory,
		PasswordEncoder passwordEncoder, TokenProvider tokenProvider,
		RedisUtil redisUtil, AuthenticationManager authenticationManager) {
		this.jpaUserRepsitory = jpaUserRepsitory;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.redisUtil = redisUtil;
		this.authenticationManager = authenticationManager;
	}

	@Override
	@Transactional // 2. 쓰기 트랜잭션 추가
	public MemberResponse memberJoin(JoinMemberRequest request) {

		// 1) 정규화(필수는 아니지만 실무에선 거의 함)
		String email = request.getEmail().trim().toLowerCase();
		String name = request.getName();
		String password = passwordEncoder.encode(request.getPassword());

		// 2) 중복 사전 체크(UX 개선용) - 최종 방어는 DB UNIQUE
		if (jpaUserRepsitory.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}

		UserRole role = UserRole.ADMIN;

		Users entity = Users.builder()
			.email(email) // 정규화 반영
			.name(name)
			.password(password)
			.role(role)
			.delYn(false)
			.build();

		Users saved;
		try {
			saved = jpaUserRepsitory.save(entity);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateEmailException();
		}

		return MemberResponse.builder()
			.id(saved.getUid())
			.email(saved.getEmail())
			.name(saved.getName())
			.build();
	}

	@Override
	public TokenDto searchLogin(JoinMemberRequest request) {
		Optional<Users> memberInfo;
		Optional<Users> memberEmail;

		String inputEmail = request.getEmail().trim().toLowerCase();
		String inputPasword = request.getPassword();
		memberEmail = jpaUserRepsitory.findByEmail(inputEmail);

		TokenDto tokenDto;

		if (memberEmail.isEmpty()) {
			throw new InvalidCredentialsException();
		} else {
			// 비밀번호 확인
			if (!passwordEncoder.matches(inputPasword, memberEmail.get().getPassword())) {
				throw new InvalidCredentialsException();
			} else {
				log.info("[JWT-REDIS] 로그인 흐름 ① — 이메일/비밀번호 확인 완료, 인증 토큰 생성 단계 진입");
				memberInfo = jpaUserRepsitory.findByEmailAndPassword(inputEmail, memberEmail.get().getPassword());

				UsernamePasswordAuthenticationToken authenticationToken =
					new UsernamePasswordAuthenticationToken(memberInfo.get().getEmail(), inputPasword);

				Authentication authentication = authenticationManager.authenticate(authenticationToken);
				log.info("[JWT-REDIS] 로그인 흐름 ② — AuthenticationManager 인증 완료");

				tokenDto = tokenProvider.generateTokenDto(authentication);

				// Redis에 리프레시 토큰 저장
				log.info("[JWT-REDIS] 로그인 흐름 ③ — Access 는 응답으로만 전달, Refresh 만 Redis 에 email 키로 저장");
				redisUtil.setValue(
					RedisKeyPrefix.REFRESH_TOKEN + memberInfo.get().getEmail(),
					tokenDto.getRefreshToken(),
					tokenDto.getRefreshTokenExpiresIn(),
					TimeUnit.MILLISECONDS);
				log.info("[JWT-REDIS] 로그인 흐름 ④ — 로그인 처리 완료 (클라이언트는 access 를 헤더에, refresh 는 별도 보관)");
				log.info("Redis refresh token saved");

				return tokenDto;
			}
		}
	}

	@Override
	public MemberResponse searchJoin(Long memberId) {
		SecurityContext context = SecurityContextHolder.getContext();
		Authentication authentication = context.getAuthentication();

		if (authentication == null) {
			return MemberResponse.builder()
				.id(memberId)
				.name("anonymous")
				.email("anonymous")
				.build();
		}
		Users member = jpaUserRepsitory.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);
		return MemberResponse.builder()
			.id(member.getUid())
			.name(member.getName())
			.email(member.getEmail())
			.build();
	}

	@Override
	@Transactional(readOnly = true)
	public TokenDto resolveRefreshToken(String refreshToken) {
		if (refreshToken == null || !refreshToken.startsWith("Bearer ")
			|| refreshToken.length() == "Bearer ".length()) {
			throw new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN);
		}
		refreshToken = refreshToken.substring(7);
		var claims = tokenProvider.parseRefreshToken(refreshToken);
		String userId = claims.getSubject();

		String savedRefreshToken = redisUtil.getValue(RedisKeyPrefix.REFRESH_TOKEN + userId);

		if (savedRefreshToken == null) {
			throw new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN);
		}

		if (!savedRefreshToken.equals(refreshToken)) {
			throw new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN);
		}

		Authentication authentication = tokenProvider.getAuthenticationFromRefreshToken(refreshToken);
		TokenDto newTokenDto = tokenProvider.generateTokenDto(authentication);

		redisUtil.setValue(
			RedisKeyPrefix.REFRESH_TOKEN + userId,
			newTokenDto.getRefreshToken(),
			newTokenDto.getRefreshTokenExpiresIn(),
			TimeUnit.MILLISECONDS
		);

		return newTokenDto;
	}
}
