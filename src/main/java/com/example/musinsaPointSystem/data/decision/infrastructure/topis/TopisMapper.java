package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.springframework.stereotype.Component;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
import com.example.musinsaPointSystem.data.evidence.mobility.*;
import com.example.musinsaPointSystem.data.evidence.mobility.IncidentEvidence.*;
@Component
public class TopisMapper {
    public TrafficEvidence traffic(Map<String,String> row,Instant collectedAt,String roadName){
        return new TrafficEvidence(text(row,"LINK_ID"),roadName,null,null,number(row,"PRCS_SPD"),number(row,"PRCS_TRV_TIME"),
            "UNKNOWN","UNKNOWN",TrafficEvidence.TrafficStatus.UNKNOWN,null,collectedAt,"TOPIS",FreshnessStatus.UNKNOWN);
    }
    public IncidentEvidence incident(Map<String,String> row,Instant collectedAt,Instant now,Map<String,String> main,Map<String,String> sub){
        var category=new TopisIncidentTypeMapper().category(row.get("ACC_TYPE"),row.get("ACC_DTYPE"),main,sub);
        Instant start=time(row.get("OCCR_DATE"),row.get("OCCR_TIME")),end=time(row.get("EXP_CLR_DATE"),row.get("EXP_CLR_TIME"));
        var status=start!=null&&start.isAfter(now)?IncidentStatus.SCHEDULED:IncidentStatus.UNKNOWN;
        // Expected clearance is not actual clearance. No ACTIVE/RESOLVED inference from the clock.
        var impact=category==IncidentCategory.ROAD_CONTROL?MobilityImpactType.ROAD_CONTROL:MobilityImpactType.UNKNOWN;
        return new IncidentEvidence(text(row,"ACC_ID"),category,status,text(row,"ACC_INFO"),text(row,"LINK_ID"),null,null,
            null,null,start,end,null,collectedAt,"TOPIS",FreshnessStatus.UNKNOWN,List.of(impact),"UNKNOWN");
    }
    private Instant time(String date,String time){
        if(date==null||time==null)return null;
        if(time.length()==4)time+="00";
        try{return LocalDateTime.parse(date+time,DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
            .withResolverStyle(ResolverStyle.STRICT)).atZone(ZoneId.of("Asia/Seoul")).toInstant();}
        catch(RuntimeException e){return null;}
    }
    private Double number(Map<String,String> row,String name){
        try{double value=Double.parseDouble(row.get(name));return Double.isFinite(value)&&value>=0?value:null;}
        catch(RuntimeException e){return null;}
    }
    private String text(Map<String,String> row,String name){String v=row.get(name);return v==null||v.isBlank()?null:v;}
}
