package com.example.musinsaPointSystem.data.decision.infrastructure.topis;

import java.util.Optional;
import org.locationtech.proj4j.*;

/** TOPIS official map uses EPSG:5181 for its incident point coordinates. */
public final class TopisCoordinateConverter {
    private static final CRSFactory CRS = new CRSFactory();
    private static final CoordinateReferenceSystem SOURCE = CRS.createFromParameters("EPSG:5181",
        "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");
    private static final CoordinateReferenceSystem TARGET = CRS.createFromParameters("EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs");
    private TopisCoordinateConverter() {}
    public record Point(double latitude, double longitude) {}
    public static Optional<Point> convert(String x, String y) {
        try {
            double easting=Double.parseDouble(x), northing=Double.parseDouble(y);
            if (!Double.isFinite(easting) || !Double.isFinite(northing)
                || easting < 100000 || easting > 300000 || northing < 400000 || northing > 500000) return Optional.empty();
            // A fresh transform avoids shared mutable intermediate coordinates across virtual threads.
            var output=new CoordinateTransformFactory().createTransform(SOURCE,TARGET)
                .transform(new ProjCoordinate(easting,northing),new ProjCoordinate());
            if (!Double.isFinite(output.x) || !Double.isFinite(output.y)
                || output.y < 37.4 || output.y > 37.75 || output.x < 126.7 || output.x > 127.25) return Optional.empty();
            return Optional.of(new Point(output.y,output.x));
        } catch (RuntimeException ignored) { return Optional.empty(); }
    }
}
