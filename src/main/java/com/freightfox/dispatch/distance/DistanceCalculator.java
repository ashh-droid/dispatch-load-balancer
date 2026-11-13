package com.freightfox.dispatch.distance;

public interface DistanceCalculator {
    double distanceKm(Coordinate origin, Coordinate destination);
}
