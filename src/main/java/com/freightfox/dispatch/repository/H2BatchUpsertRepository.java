package com.freightfox.dispatch.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class H2BatchUpsertRepository extends JdbcUpsertBase {
    public H2BatchUpsertRepository(JdbcTemplate jdbc) { super(jdbc); }
    @Override protected String orderSql() {
        return "MERGE INTO dispatch_orders (order_id, latitude, longitude, address, package_weight, priority, updated_at) "
             + "KEY (order_id) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
    }
    @Override protected String vehicleSql() {
        return "MERGE INTO dispatch_vehicles (vehicle_id, capacity, current_latitude, current_longitude, current_address, updated_at) "
             + "KEY (vehicle_id) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
    }
}
