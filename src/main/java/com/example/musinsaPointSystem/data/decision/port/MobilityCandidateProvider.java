package com.example.musinsaPointSystem.data.decision.port;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate;
import com.example.musinsaPointSystem.data.location.model.Location;

public interface MobilityCandidateProvider {
	List<MobilityCandidate> findCandidates(Location origin, Location destination);
}
