package com.example.musinsaPointSystem.data.location;
import java.math.BigDecimal;
import java.util.List;
import com.example.musinsaPointSystem.dto.SeoulArea;
/** Project representative points, not boundaries, centroids or destinations. */
public final class SeoulObservationCatalog {
    private SeoulObservationCatalog(){}
    public record Point(SeoulArea area,BigDecimal latitude,BigDecimal longitude,boolean coordinateVerified,
        String coordinateType,String coordinateSource,String geoStatus){}
    private static Point point(String code,String name,String lat,String lon){
        boolean verified=!"POI119".equals(code);
        return new Point(new SeoulArea(code,name),new BigDecimal(lat),new BigDecimal(lon),verified,
            "REPRESENTATIVE_POINT",verified?"PROJECT_CATALOG_VALIDATED_WITH_SEOUL_OFFICIAL_AREA":"PROJECT_CATALOG",
            verified?"VERIFIED":"UNVERIFIED");
    }
    public static final List<Point> POINTS=List.of(
        point("POI009","광화문·덕수궁","37.5759","126.9768"),
        point("POI014","강남역","37.4979","127.0276"),
        point("POI033","서울역","37.5547","126.9707"),
        point("POI055","홍대입구역(2호선)","37.5572","126.9254"),
        point("POI119","잠실역","37.5133","127.1001"),
        point("POI017","고속터미널역","37.5048","127.0049"),
        point("POI003","명동 관광특구","37.5636","126.9860"),
        point("POI004","이태원 관광특구","37.5345","126.9946"),
        point("POI072","여의도","37.5219","126.9245"),
        point("POI015","건대입구역","37.5404","127.0692")
    );
    public static Point find(String code){
        return POINTS.stream().filter(p->p.area().areaCode().equals(code)).findFirst().orElse(null);
    }
}
