package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobilityRequest(
	@NotBlank String areaCode,
	@NotNull Purpose purpose,
	@NotNull @Size(min = 1, max = 1, message = "사용자 상태를 한 개 선택해야 합니다.") List<UserState> userStates,
	@NotNull @Size(max = 6) List<Preference> preferences
) {
	public MobilityRequest {
		userStates = userStates == null ? null : userStates.stream().distinct().sorted().toList();
		preferences = preferences == null ? null : preferences.stream().distinct().sorted().toList();
	}

	public enum Purpose {
		COMMUTE_TO_WORK("출근"), GO_HOME("귀가"), APPOINTMENT("약속"),
		TRAVEL("여행"), OTHER("기타"), OUTING("외출");

		private final String label;
		Purpose(String label) { this.label = label; }
		public String label() { return label; }
	}

	public enum UserState {
		TIRED("피곤함"), CARRYING_LUGGAGE("짐 소지"), LIMITED_WALKING("보행 제한"),
		WITH_CHILD("아이 동반"), LATE_NIGHT("심야 이동");

		private final String label;
		UserState(String label) { this.label = label; }
		public String label() { return label; }
	}

	public enum Preference {
		AVOID_CROWD("혼잡 회피"),
		MINIMIZE_WEATHER_EXPOSURE("날씨 노출 최소화"),
		AVOID_ROAD_DELAY("도로 지연 회피"),
		FAST("빠르게"), LOW_COST("저렴하게"), LESS_WALKING("덜 걷기"), FEWER_TRANSFERS("환승 적게");

		private final String label;
		Preference(String label) { this.label = label; }
		public String label() { return label; }
	}

	public enum TransportMode {
		WALK("도보"), PUBLIC_TRANSIT("대중교통"), BIKE("자전거"), CAR("자동차"),
		TAXI("택시"), EV("전기차");

		private final String label;
		TransportMode(String label) { this.label = label; }
		public String label() { return label; }
	}
}
