package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.Preference;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.Purpose;
import com.example.musinsaPointSystem.data.location.model.Location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobilityFlowRequest(
	@NotBlank String originAreaCode,
	@NotBlank @Size(max = 120) String destination,
	@NotNull Purpose purpose,
	@NotNull @Size(min = 1, max = 5) List<Priority> priorities,
	Location origin,
	Location destinationLocation
) {
	public MobilityFlowRequest {
		originAreaCode = originAreaCode == null ? null : originAreaCode.trim();
		destination = destination == null ? null : destination.trim();
		priorities = priorities == null ? null : priorities.stream().distinct().sorted().toList();
	}

	public MobilityFlowRequest(String originAreaCode, String destination, Purpose purpose,
		List<Priority> priorities) {
		this(originAreaCode, destination, purpose, priorities, null, null);
	}

	public List<Preference> toPreferences() {
		return priorities.stream().map(Priority::preference).toList();
	}

	public enum Priority {
		FAST("빠르게", Preference.FAST),
		LOW_COST("저렴하게", Preference.LOW_COST),
		LESS_WALKING("덜 걷기", Preference.LESS_WALKING),
		AVOID_CROWD("혼잡 피하기", Preference.AVOID_CROWD),
		FEWER_TRANSFERS("환승 적게", Preference.FEWER_TRANSFERS);

		private final String label;
		private final Preference preference;

		Priority(String label, Preference preference) {
			this.label = label;
			this.preference = preference;
		}

		public String label() { return label; }
		public Preference preference() { return preference; }
	}
}
