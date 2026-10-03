package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.util.Map;
import com.example.musinsaPointSystem.data.evidence.mobility.IncidentEvidence.IncidentCategory;
/** Codes are resolved through official code APIs; exact labels only, never free-text contains(). */
public class TopisIncidentTypeMapper {
    public IncidentCategory category(String type,String detail,Map<String,String> main,Map<String,String> sub){
        var detailed=label(sub.get(detail));return detailed!=IncidentCategory.OTHER?detailed:label(main.get(type));
    }
    private IncidentCategory label(String value){
        if(value==null)return IncidentCategory.OTHER;
        return switch(value.trim()){
            case "사고","교통사고","추돌사고","전복사고","전도사고","보행사고" -> IncidentCategory.ACCIDENT;
            case "통제","도로통제" -> IncidentCategory.ROAD_CONTROL;
            case "공사" -> IncidentCategory.CONSTRUCTION;
            case "집회" -> IncidentCategory.PROTEST;
            case "화재","차량화재" -> IncidentCategory.FIRE;
            case "기상" -> IncidentCategory.WEATHER;
            case "고장","차량고장" -> IncidentCategory.FAILURE;
            default -> IncidentCategory.OTHER;
        };
    }
}
