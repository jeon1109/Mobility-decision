package com.example.musinsaPointSystem.data.location.port;

import java.util.List;

import com.example.musinsaPointSystem.data.location.model.PlaceCandidate;

public interface PlaceSearchPort {
	List<PlaceCandidate> search(String keyword);
}
