package com.example.musinsaPointSystem.data.decision.port;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.SubwayArrivalEvidence;

public interface SubwayArrivalPort {
	List<SubwayArrivalEvidence> findArrivals(String stationName);
}
