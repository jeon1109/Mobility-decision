package com.example.musinsaPointSystem.data.location;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.example.musinsaPointSystem.data.location.model.AdministrativeArea;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.data.location.model.PlaceCandidate;
import com.example.musinsaPointSystem.data.location.port.ReverseGeocodingPort;

/** Search addresses are provider metadata; submitted locations must be verified by coordinates. */
@Component
public class SeoulServiceAreaPolicy {
    private final ReverseGeocodingPort geocoding;
    public SeoulServiceAreaPolicy(ReverseGeocodingPort geocoding) { this.geocoding = geocoding; }

    public boolean isSupported(AdministrativeArea area) {
        return area != null && "서울특별시".equals(area.city());
    }
    public void requireSupported(AdministrativeArea area) {
        if (!isSupported(area)) throw new UnsupportedServiceAreaException();
    }
    public boolean supportsCandidate(PlaceCandidate candidate) {
        return filterCandidate(candidate) != null;
    }
    public PlaceCandidate filterCandidate(PlaceCandidate candidate) {
        validateCoordinates(candidate.latitude(), candidate.longitude());
        String city = addressCity(candidate.address());
        String roadCity = addressCity(candidate.roadAddress());
        if (city != null && roadCity != null && !city.equals(roadCity)) return null;
        String known = city != null ? city : roadCity;
        AdministrativeArea area = known != null ? new AdministrativeArea(known, null)
            : geocoding.resolve(candidate.latitude(), candidate.longitude());
        if (!isSupported(area)) return null;
        return new PlaceCandidate(candidate.providerPlaceId(), candidate.name(), candidate.address(), candidate.roadAddress(),
            candidate.latitude(), candidate.longitude(), candidate.category(), candidate.provider(), area.city(), area.district());
    }
    public static String addressCity(String address) {
        if (address == null || address.isBlank()) return null;
        String first = address.trim().split("\\s+", 2)[0];
        return switch (first) {
            case "서울", "서울특별시" -> "서울특별시";
            case "경기", "경기도", "인천", "인천광역시", "부산", "부산광역시",
                "대구", "대구광역시", "대전", "대전광역시", "광주", "광주광역시",
                "울산", "울산광역시", "세종", "세종특별자치시", "강원", "강원도", "강원특별자치도",
                "충북", "충청북도", "충남", "충청남도", "전북", "전라북도", "전북특별자치도",
                "전남", "전라남도", "경북", "경상북도", "경남", "경상남도", "제주", "제주특별자치도" -> "OUTSIDE_SEOUL";
            default -> null;
        };
    }
    public Location verify(Location location) {
        if (location == null) throw new IllegalArgumentException("장소 정보가 필요합니다.");
        validateCoordinates(location.latitude(), location.longitude());
        AdministrativeArea area = geocoding.resolve(location.latitude(), location.longitude());
        requireSupported(area);
        return new Location(location.placeId(), location.name(), location.address(), location.roadAddress(),
            location.latitude(), location.longitude(), area.city(), area.district(), location.category(), location.provider());
    }
    public static void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
            || latitude.compareTo(BigDecimal.valueOf(90)) > 0 || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
            || longitude.compareTo(BigDecimal.valueOf(180)) > 0)
            throw new IllegalArgumentException("장소 좌표가 올바르지 않습니다.");
    }
}
