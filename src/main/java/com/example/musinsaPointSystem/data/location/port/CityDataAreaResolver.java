package com.example.musinsaPointSystem.data.location.port;

import java.util.Optional;

import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.dto.SeoulArea;

public interface CityDataAreaResolver {
	Optional<SeoulArea> resolve(Location location);
}
