package com.example.musinsaPointSystem.data.location;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "location.kakao")
public class LocationProperties {
	private String apiKey = "";
	private String baseUrl = "https://dapi.kakao.com";
	private int searchLimit = 10;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		String normalized = apiKey == null ? "" : apiKey.trim();
		if (isQuoted(normalized)) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		if (normalized.regionMatches(true, 0, "KakaoAK ", 0, "KakaoAK ".length())) {
			normalized = normalized.substring("KakaoAK ".length()).trim();
		}
		this.apiKey = normalized;
	}

	public String getAuthorizationHeader() {
		return apiKey.isBlank() ? "" : "KakaoAK " + apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public int getSearchLimit() {
		return searchLimit;
	}

	public void setSearchLimit(int searchLimit) {
		this.searchLimit = searchLimit;
	}

	private boolean isQuoted(String value) {
		return value.length() >= 2
			&& ((value.startsWith("\"") && value.endsWith("\""))
			|| (value.startsWith("'") && value.endsWith("'")));
	}
}
