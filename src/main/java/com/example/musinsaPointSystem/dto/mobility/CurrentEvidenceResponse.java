package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.CatchableTrain;
import com.example.musinsaPointSystem.data.decision.model.NearbyStation;
import com.example.musinsaPointSystem.data.decision.model.SubwayArrivalEvidence;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.dto.SeoulArea;

public record CurrentEvidenceResponse(
	String schemaVersion,
	String requestedAt,
	Location location,
	SeoulArea cityDataArea,
	CitySituation city,
	List<StationEvidence> nearbyStations,
	List<String> unavailableEvidence
) {
	public CurrentEvidenceResponse {
		nearbyStations = nearbyStations == null ? List.of() : List.copyOf(nearbyStations);
		unavailableEvidence = unavailableEvidence == null ? List.of() : List.copyOf(unavailableEvidence);
	}

	public record StationEvidence(NearbyStation station, List<SubwayArrivalEvidence> arrivals,
		CatchableTrain catchableTrain) {
		public StationEvidence {
			arrivals = arrivals == null ? List.of() : List.copyOf(arrivals);
		}
	}
}
