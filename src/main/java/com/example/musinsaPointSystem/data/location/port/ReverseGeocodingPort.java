package com.example.musinsaPointSystem.data.location.port;

import java.math.BigDecimal;

import com.example.musinsaPointSystem.data.location.model.AdministrativeArea;

public interface ReverseGeocodingPort {
	AdministrativeArea resolve(BigDecimal latitude, BigDecimal longitude);
}
