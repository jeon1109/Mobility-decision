package com.example.musinsaPointSystem.data.decision.guest;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseRequest;
import com.example.musinsaPointSystem.data.location.model.Location;
public final class GuestRequestPolicy {
    // Preserve Kakao/browser coordinate precision while bounding oversized inputs.
    private static final int MAX_COORDINATE_PRECISION = 32;
    private static final int MAX_COORDINATE_SCALE = 24;
    private GuestRequestPolicy(){}
    public static void validate(MobilityDecisionCaseRequest request){
        if(request==null||request.purpose()==null||request.preferences()==null
            ||request.preferences().size()>2||request.preferences().stream().anyMatch(java.util.Objects::isNull))
            throw new IllegalArgumentException("비회원 상세 우선순위는 최대 2개까지 지정할 수 있습니다.");
        location(request.origin());location(request.destination());
    }
    private static void location(Location value){
        if(value==null)throw new IllegalArgumentException("출발지와 목적지가 필요합니다.");
        for(String text:new String[]{value.placeId(),value.name(),value.address(),value.roadAddress(),value.city(),value.district(),value.category(),value.provider()})
            if(text!=null&&text.length()>256)throw new IllegalArgumentException("장소 정보가 너무 깁니다.");
        for(var coordinate:new java.math.BigDecimal[]{value.latitude(),value.longitude()})
            if(coordinate!=null&&(coordinate.precision()>MAX_COORDINATE_PRECISION
                ||Math.abs((long)coordinate.scale())>MAX_COORDINATE_SCALE))
                throw new IllegalArgumentException("장소 좌표 정밀도가 올바르지 않습니다.");
    }
}
