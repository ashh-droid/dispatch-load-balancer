package com.freightfox.dispatch.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity(name = "DispatchOrder")
@Table(name = "dispatch_orders")
public class Order {
    @Id @Column(name = "order_id", length = 64)
    private String orderId;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Column(nullable = false, length = 512) private String address;
    @Column(name = "package_weight", nullable = false, precision = 15, scale = 3)
    private BigDecimal packageWeight;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 6)
    private Priority priority;

    protected Order() {}

    public String getOrderId() { return orderId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public BigDecimal getPackageWeight() { return packageWeight; }
    public Priority getPriority() { return priority; }
}
