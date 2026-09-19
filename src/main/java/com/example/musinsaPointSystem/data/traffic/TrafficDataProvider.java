package com.example.musinsaPointSystem.data.traffic;

import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;

public interface TrafficDataProvider {
	default SeoulCityData getCityData(String areaCode) {
		return getCityData(areaCode, areaCode);
	}

	SeoulCityData getCityData(String areaCode, String areaName);

	CitySituation getCitySituation(String areaCode, String areaName);
}
