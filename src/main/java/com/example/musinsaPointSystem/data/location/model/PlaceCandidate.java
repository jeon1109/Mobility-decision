package com.example.musinsaPointSystem.data.location.model;

import java.math.BigDecimal;

public record PlaceCandidate(
	String providerPlaceId,
	String name,
	String address,
	String roadAddress,
	BigDecimal latitude,
	BigDecimal longitude,
	String category,
	String provider,
	String city,
	String district
) {
	public PlaceCandidate(String providerPlaceId, String name, String address, String roadAddress,
		BigDecimal latitude, BigDecimal longitude, String category, String provider) {
		this(providerPlaceId, name, address, roadAddress, latitude, longitude, category, provider, null, null);
	}
}
