package com.example.musinsaPointSystem.users.service.impl;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
	private RedisTemplate<String, String> redisTemplate;
	private final RabbitTemplate rabbitTemplate;

	// 큐 저장소
	@Value("${message.queue.user}")
	private String queueUsers;

	@Value("${message.queue.join}")
	private String queueJoins;

	public UserServiceImpl(UserRepository jpaUserRepsitory,
		PasswordEncoder passwordEncoder, TokenProvider tokenProvider,
		RedisUtil redisUtil, AuthenticationManager authenticationManager,
		RabbitTemplate rabbitTemplate) {
		this.jpaUserRepsitory = jpaUserRepsitory;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.redisUtil = redisUtil;
		this.authenticationManager = authenticationManager;
		this.redisTemplate = redisTemplate;
		this.rabbitTemplate = rabbitTemplate;
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
			throw new DuplicateEmailException(email);
		}

		UserRole role = UserRole.ADMIN;

		Users entity = Users.builder()
			.email(email) // 정규화 반영
			.name(name)
			.password(password)
			.role(role)
			.delYn(false)
			.build();

		Users saved = jpaUserRepsitory.save(entity);

		if (saved.getUid() != null) {
			rabbitTemplate.convertAndSend(queueUsers, saved.getUid());
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

		String inputEmail = request.getEmail();
		String inputPasword = request.getPassword();
		memberEmail = jpaUserRepsitory.findByEmail(inputEmail);

		TokenDto tokenDto;

		if (memberEmail.isEmpty()) {
			throw new IllegalAccessError("이메일이 일치하지 않습니다.");
		} else {
			// 비밀번호 확인
			if (!passwordEncoder.matches(inputPasword, memberEmail.get().getPassword())) {
				throw new IllegalAccessError("비밀번호가 일치하지 않습니다.");
			} else {
				log.info("[JWT-REDIS] 로그인 흐름 ① — 이메일/비밀번호 일치, 인증 토큰 생성 단계 진입 email={}", inputEmail);
				memberInfo = jpaUserRepsitory.findByEmailAndPassword(inputEmail, memberEmail.get().getPassword());

				UsernamePasswordAuthenticationToken authenticationToken =
					new UsernamePasswordAuthenticationToken(memberInfo.get().getEmail(), inputPasword);

				Authentication authentication = authenticationManager.authenticate(authenticationToken);
				log.info("[JWT-REDIS] 로그인 흐름 ② — AuthenticationManager 인증 완료 principal={}", authentication.getName());

				tokenDto = tokenProvider.generateTokenDto(authentication);

				// Redis에 리프레시 토큰 저장
				log.info("[JWT-REDIS] 로그인 흐름 ③ — Access 는 응답으로만 전달, Refresh 만 Redis 에 email 키로 저장");
				redisUtil.save(memberInfo.get().getEmail(), tokenDto.getRefreshToken());
				log.info("[JWT-REDIS] 로그인 흐름 ④ — 로그인 처리 완료 (클라이언트는 access 를 헤더에, refresh 는 별도 보관)");
				log.info("Redis save key={}, value={}", memberInfo.get().getEmail(), tokenDto.getRefreshToken());

				if (tokenDto.getAccessToken().length() > 0) {
					rabbitTemplate.convertAndSend(queueJoins, tokenDto.getAccessToken());
				}

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
		} else {
			return MemberResponse.builder()
				.id(memberId)
				.name("test")
				.email("gird644@gmail.com")
				.build();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public TokenDto resolveRefreshToken(String refreshToken) {
		// log.info("[JWT-REDIS] reissue 요청 — Authorization 헤더로 refresh 전달 여부 확인 (현재 구현은 Redis 조회/검증 없이 Bearer 제거만 수행)");
		// if (!tokenProvider.validateTokenWithoutBlacklist(refreshToken)) {
		// 	throw new RuntimeException("유효하지 않은 Refresh Token 입니다.");
		// }
		//
		// if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
		// 	throw new InvalidRefreshTokenException("리프레시 토큰이 누락되었거나 올바르지 않습니다.");
		// }
		//
		// String raw = refreshToken.substring(7);
		// log.info("[JWT-REDIS] reissue — Bearer 제거 후 토큰 문자열 길이={}자 반환", raw.length());
		// return raw;

		if (!tokenProvider.validateTokenWithoutBlacklist(refreshToken)) {
			throw new RuntimeException("유효하지 않은 Refresh Token 입니다.");
		}

		String userId = tokenProvider.getSubject(refreshToken);

		String savedRefreshToken = redisUtil.getValue(RedisKeyPrefix.REFRESH_TOKEN + userId);

		if (savedRefreshToken == null) {
			throw new RuntimeException("로그아웃 되었거나 만료된 Refresh Token 입니다.");
		}

		if (!savedRefreshToken.equals(refreshToken)) {
			throw new RuntimeException("저장된 Refresh Token 과 일치하지 않습니다.");
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

	@Transactional
	public void logout(String accessToken) {
		if (!tokenProvider.validateTokenWithoutBlacklist(accessToken)) {
			throw new ApiException(BasicResponseMessage.UNAUTHORIZED);
		}

		Authentication authentication = tokenProvider.getAuthentication(accessToken);
		String userId = authentication.getName();

		// Refresh 삭제 (저장소 하나로 통일 권장)
		//redisUtil.delete("refresh:" + userId);

		redisTemplate.opsForValue().get("JWT_TOKEN:" + userId);
		redisTemplate.delete("JWT_TOKEN:" + userId); //Token 삭제
	}
}
