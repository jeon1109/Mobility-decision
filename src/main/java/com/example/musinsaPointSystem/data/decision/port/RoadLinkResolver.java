package com.example.musinsaPointSystem.data.decision.port;
import java.util.List;
import com.example.musinsaPointSystem.data.location.model.Location;
public interface RoadLinkResolver {
    List<String> findVerifiedLinks(Location location);
}
