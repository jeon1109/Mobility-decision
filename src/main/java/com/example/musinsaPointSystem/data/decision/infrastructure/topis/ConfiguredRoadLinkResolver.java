package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.musinsaPointSystem.data.decision.port.RoadLinkResolver;
import com.example.musinsaPointSystem.data.location.model.Location;
@Component
public class ConfiguredRoadLinkResolver implements RoadLinkResolver {
    private final TopisProperties properties;
    public ConfiguredRoadLinkResolver(TopisProperties properties){this.properties=properties;}
    public List<String> findVerifiedLinks(Location location){
        if(location == null || location.provider() == null || location.placeId() == null) return List.of();
        return properties.getLocationLinks().getOrDefault(location.provider()+":"+location.placeId(),List.of())
            .stream().filter(v -> v != null && v.matches("[0-9]+")).distinct().limit(5).toList();
    }
}
