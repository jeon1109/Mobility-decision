package com.example.musinsaPointSystem.data.decision.infrastructure.topis;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.decision.port.IncidentEvidencePort;
import com.example.musinsaPointSystem.data.evidence.mobility.EvidenceAvailability;
import com.example.musinsaPointSystem.data.evidence.mobility.EvidenceBatch;
import com.example.musinsaPointSystem.data.evidence.mobility.IncidentEvidence;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class TopisIncidentAdapter implements IncidentEvidencePort {
	private final TopisClient client;
	private final TopisMapper mapper;
	private final Clock clock;
	private final MeterRegistry meters;

	public TopisIncidentAdapter(TopisClient client, TopisMapper mapper, Clock clock, MeterRegistry meters) {
		this.client = client;
		this.mapper = mapper;
		this.clock = clock;
		this.meters = meters;
	}

	public EvidenceBatch<IncidentEvidence> collectIncidents() {
		try {
			var data = client.fetch("AccInfo", null);
			Map<String, String> main = codes("AccMainCode", "ACC_TYPE", "ACC_TYPE_NM"),
				sub = codes("AccSubCode", "ACC_DTYPE", "ACC_DTYPE_NM");
			var items = data.rows().stream().filter(r -> r.get("ACC_ID") != null && !r.get("ACC_ID").isBlank())
				.map(r -> mapper.incident(r, data.collectedAt(), clock.instant(), main, sub)).toList();
			meters.summary("topis.incident.count").record(items.size());
			meters.summary("topis.incident.active.count")
				.record(items.stream().filter(i -> i.status() == IncidentEvidence.IncidentStatus.ACTIVE).count());
			meters.summary("topis.incident.scheduled.count")
				.record(items.stream().filter(i -> i.status() == IncidentEvidence.IncidentStatus.SCHEDULED).count());
			return new EvidenceBatch<>(
				data.complete() && items.size() == data.rows().size() ? EvidenceAvailability.AVAILABLE :
					EvidenceAvailability.UNKNOWN, items);
		} catch (RuntimeException ignored) {
			return EvidenceBatch.unavailable();
		}
	}

	private Map<String, String> codes(String service, String code, String name) {
		try {
			Map<String, String> values = new HashMap<>();
			client.fetch(service, null).rows()
				.forEach(r -> {
					if (r.get(code) != null && r.get(name) != null)
						values.put(r.get(code), r.get(name));
				});
			return values;
		} catch (RuntimeException ignored) {
			return Map.of();
		}
	}
}
