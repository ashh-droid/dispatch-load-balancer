package io.github.ashhdroid.dispatch.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dispatch_vehicles")
public class Vehicle {
    @Id @Column(name = "vehicle_id", length = 64)
    private String vehicleId;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal capacity;
    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;
    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;
    @Column(name = "current_address", nullable = false, length = 512)
    private String currentAddress;

    protected Vehicle() {}

    public String getVehicleId() { return vehicleId; }
    public BigDecimal getCapacity() { return capacity; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public String getCurrentAddress() { return currentAddress; }
}
