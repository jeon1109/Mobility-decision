package com.example.musinsaPointSystem.data.location.model;

import java.math.BigDecimal;

public record Location(
	String placeId,
	String name,
	String address,
	String roadAddress,
	BigDecimal latitude,
	BigDecimal longitude,
	String city,
	String district,
	String category,
	String provider
) {}
