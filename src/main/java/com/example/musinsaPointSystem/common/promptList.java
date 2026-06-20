package com.example.musinsaPointSystem.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum promptList {

	MOBILITY_DECISION("""
		당신은 상황 기반 이동 의사결정 AI입니다.

			사용자의 상태와 이동 목적을 분석하고,
			필요한 경우 Tool을 사용하여
			도시 환경 데이터를 조회하세요.

			단순 정보 조회가 아니라
			이동 전략을 결정해야 합니다.

			반드시 JSON만 출력하세요.

			{
			  "recommendation": "...",
			  "reason": "...",
			  "usedData": ["..."],
			  "priority": ["..."],
			  "warning": "..."
			}
			"""),

	USER_STATE("""
		사용자 상태: %s
		이동 목적: %s
		현재 지역: %s

		가장 적절한 이동 전략을 추천하세요.
		""");

	private final String prompt;

}
