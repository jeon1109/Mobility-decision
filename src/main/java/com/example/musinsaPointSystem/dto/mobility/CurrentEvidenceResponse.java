package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;
import com.example.musinsaPointSystem.data.evidence.mobility.*;

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
	List<String> unavailableEvidence,
	EvidenceAvailability trafficAvailability,
	List<TrafficEvidence> traffic,
	EvidenceAvailability incidentAvailability,
	List<IncidentEvidence> incidents,
	com.example.musinsaPointSystem.data.location.SeoulObservationCatalog.Point observationArea
) {
	public CurrentEvidenceResponse(String schemaVersion, String requestedAt, Location location, SeoulArea cityDataArea,
		CitySituation city, List<StationEvidence> nearbyStations, List<String> unavailableEvidence,
		EvidenceAvailability trafficAvailability, List<TrafficEvidence> traffic,
		EvidenceAvailability incidentAvailability, List<IncidentEvidence> incidents) {
		this(schemaVersion, requestedAt, location, cityDataArea, city, nearbyStations, unavailableEvidence,
			trafficAvailability, traffic, incidentAvailability, incidents, cityDataArea == null ? null
				: com.example.musinsaPointSystem.data.location.SeoulObservationCatalog.find(cityDataArea.areaCode()));
	}
	public CurrentEvidenceResponse(String schemaVersion, String requestedAt, Location location,
		SeoulArea cityDataArea, CitySituation city, List<StationEvidence> nearbyStations, List<String> unavailableEvidence) {
		this(schemaVersion, requestedAt, location, cityDataArea, city, nearbyStations, unavailableEvidence,
			EvidenceAvailability.UNAVAILABLE, List.of(), EvidenceAvailability.UNAVAILABLE, List.of());
	}
	public CurrentEvidenceResponse {
		traffic = traffic == null ? List.of() : List.copyOf(traffic);
		incidents = incidents == null ? List.of() : List.copyOf(incidents);
		trafficAvailability = trafficAvailability == null ? EvidenceAvailability.UNAVAILABLE : trafficAvailability;
		incidentAvailability = incidentAvailability == null ? EvidenceAvailability.UNAVAILABLE : incidentAvailability;
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
