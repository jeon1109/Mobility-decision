package com.example.musinsaPointSystem.dto.mobilityv2;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobilityV2Request(
	@NotBlank String areaCode,
	@NotNull Purpose purpose,
	@NotNull @Size(max = 5) List<UserState> userStates,
	@NotNull @Size(max = 6) List<Preference> preferences,
	List<TransportMode> availableModes
) {
	public MobilityV2Request {
		userStates = userStates == null ? null : userStates.stream().distinct().sorted().toList();
		preferences = preferences == null ? null : preferences.stream().distinct().sorted().toList();
		availableModes = availableModes == null
			? null
			: availableModes.stream().distinct().sorted().toList();
	}

	public enum Purpose { COMMUTE_TO_WORK, GO_HOME, OUTING, TOURISM, OTHER }
	public enum UserState { TIRED, CARRYING_LUGGAGE, LIMITED_WALKING, WITH_CHILD, LATE_NIGHT }
	public enum Preference {
		AVOID_CROWD, MINIMIZE_WEATHER_EXPOSURE, AVOID_ROAD_DELAY,
		PREFER_PUBLIC_TRANSIT, PREFER_BIKE, PREFER_CAR
	}
	public enum TransportMode { WALK, PUBLIC_TRANSIT, BIKE, CAR, TAXI, EV }
}
