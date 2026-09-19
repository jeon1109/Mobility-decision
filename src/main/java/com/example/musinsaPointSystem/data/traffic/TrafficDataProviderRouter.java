package com.example.musinsaPointSystem.data.traffic;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;

@Component
@Primary
public class TrafficDataProviderRouter implements TrafficDataProvider {
	private final PublicApiTrafficProvider publicApi;

	public TrafficDataProviderRouter(PublicApiTrafficProvider publicApi) {
		this.publicApi = publicApi;
	}

	@Override
	public SeoulCityData getCityData(String areaCode, String areaName) {
		return publicApi.getCityData(areaCode, areaName);
	}

	@Override
	public CitySituation getCitySituation(String areaCode, String areaName) {
		return publicApi.getCitySituation(areaCode, areaName);
	}
}
