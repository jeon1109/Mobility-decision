package com.example.musinsaPointSystem.data.traffic;

import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.apiService.ApiService;
import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PublicApiTrafficProvider implements TrafficDataProvider {
	private final ApiService apiService;

	@Override
	public SeoulCityData getCityData(String areaCode, String areaName) {
		return apiService.getCityData(areaCode);
	}

	@Override
	public CitySituation getCitySituation(String areaCode, String areaName) {
		return apiService.getCitySituation(areaCode, areaName);
	}
}
