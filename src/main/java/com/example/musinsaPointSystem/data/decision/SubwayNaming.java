package com.example.musinsaPointSystem.data.decision;

import java.util.Locale;

import org.springframework.util.StringUtils;

public final class SubwayNaming {
	private static final String LINE_SUFFIX =
		"\\s+(?:(?:수도권)?\\d+호선|공항철도|GTX-[A-Za-z]|경의중앙선|경춘선|수인분당선|신분당선|우이신설선|서해선)$";

	private SubwayNaming() {}

	public static String stationQueryName(String stationName) {
		if (!StringUtils.hasText(stationName)) return "";
		String normalized = stationName.trim().replaceFirst(LINE_SUFFIX, "").trim();
		return normalized.endsWith("역")
			? normalized.substring(0, normalized.length() - 1).trim()
			: normalized;
	}

	public static String normalizeLine(String line) {
		if (!StringUtils.hasText(line)) return "";
		return line.trim()
			.replaceFirst("^수도권", "")
			.replace(" ", "")
			.toUpperCase(Locale.ROOT);
	}

	public static boolean sameLine(String expected, String actual) {
		String expectedLine = normalizeLine(expected);
		return expectedLine.isEmpty() || expectedLine.equals(normalizeLine(actual));
	}
}
