package com.example.musinsaPointSystem.data.decision.port;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.NearbyStation;
import com.example.musinsaPointSystem.data.location.model.Location;

public interface NearbyStationPort {
	List<NearbyStation> findNearby(Location origin);
}
