package com.example.musinsaPointSystem.data.location.model;

import com.example.musinsaPointSystem.dto.SeoulArea;

public record ResolvedLocation(
	Location location,
	SeoulArea cityDataArea,
	boolean realtimeDataAvailable,
	String message
) {}
