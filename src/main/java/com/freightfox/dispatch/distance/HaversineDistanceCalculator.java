package com.freightfox.dispatch.distance;

import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(Coordinate origin, Coordinate destination) {
        double deltaLat = Math.toRadians(destination.latitude() - origin.latitude());
        double deltaLon = Math.toRadians(destination.longitude() - origin.longitude());
        double sinLat = Math.sin(deltaLat / 2.0);
        double sinLon = Math.sin(deltaLon / 2.0);
        double a = sinLat * sinLat + Math.cos(Math.toRadians(origin.latitude()))
                * Math.cos(Math.toRadians(destination.latitude())) * sinLon * sinLon;
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(Math.min(1.0, Math.max(0.0, a))));
    }
}
