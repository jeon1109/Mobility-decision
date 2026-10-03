package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.time.Instant;
import java.util.List;
import java.util.Map;
/** Infrastructure only; never serialized into UI or AI contracts. */
public record TopisResponseDto(int totalCount, List<Map<String,String>> rows, Instant collectedAt, boolean complete) {}
