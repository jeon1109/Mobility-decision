package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.util.*;
import org.springframework.stereotype.Component;
import com.example.musinsaPointSystem.data.decision.port.TrafficEvidencePort;
import com.example.musinsaPointSystem.data.evidence.mobility.*;
@Component
public class TopisTrafficAdapter implements TrafficEvidencePort {
    private final TopisClient client;private final TopisMapper mapper;
    public TopisTrafficAdapter(TopisClient client,TopisMapper mapper){this.client=client;this.mapper=mapper;}
    public EvidenceBatch<TrafficEvidence> findByLinks(List<String> linkIds){
        if(linkIds==null||linkIds.isEmpty())return EvidenceBatch.unavailable();
        List<TrafficEvidence> rows=new ArrayList<>();boolean failed=false;
        for(String link:linkIds.stream().distinct().limit(5).toList())try{
            var result=client.fetch("TrafficInfo",link);String road=null;
            try{road=client.fetch("LinkInfo",link).rows().stream().filter(r->link.equals(r.get("LINK_ID")))
                .map(r->r.get("ROAD_NAME")).filter(Objects::nonNull).findFirst().orElse(null);}catch(RuntimeException ignored){}
            for(var row:result.rows())if(link.equals(row.get("LINK_ID")))rows.add(mapper.traffic(row,result.collectedAt(),road));
            if(!result.complete())failed=true;
        }catch(RuntimeException ignored){failed=true;}
        return new EvidenceBatch<>(rows.isEmpty()?(failed?EvidenceAvailability.UNAVAILABLE:EvidenceAvailability.UNKNOWN):
            (failed?EvidenceAvailability.UNKNOWN:EvidenceAvailability.AVAILABLE),rows);
    }
}
