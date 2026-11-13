package com.freightfox.dispatch.distance;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {
    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test void identicalPointsHaveZeroDistance() {
        var point = new Coordinate(28.6139, 77.2090);
        assertThat(calculator.distanceKm(point, point)).isZero();
    }
    @Test void knownDelhiCoordinates() {
        assertThat(calculator.distanceKm(new Coordinate(28.6139,77.2090), new Coordinate(28.7041,77.1025)))
                .isCloseTo(14.44226, org.assertj.core.data.Offset.offset(0.005));
    }
    @Test void knownLongDistance() {
        assertThat(calculator.distanceKm(new Coordinate(12.9716,77.5946), new Coordinate(13.0827,80.2707)))
                .isCloseTo(290.17202, org.assertj.core.data.Offset.offset(0.005));
    }
    @Test void symmetricAcrossEquatorAndDateLine() {
        var a = new Coordinate(-1.4, 179.4);
        var b = new Coordinate(2.1, -179.7);
        assertThat(calculator.distanceKm(a,b)).isCloseTo(calculator.distanceKm(b,a),
                org.assertj.core.data.Offset.offset(1e-9));
        assertThat(calculator.distanceKm(a,b)).isGreaterThan(0).isLessThan(500);
    }
    @Test void antipodalPointsAreFinite() {
        assertThat(calculator.distanceKm(new Coordinate(0,0), new Coordinate(0,180)))
                .isCloseTo(Math.PI * 6371.0, org.assertj.core.data.Offset.offset(1e-6));
    }
}
