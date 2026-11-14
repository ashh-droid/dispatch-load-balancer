package io.github.ashhdroid.dispatch.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresBatchUpsertRepository extends JdbcUpsertBase {
    public PostgresBatchUpsertRepository(JdbcTemplate jdbc) { super(jdbc); }
    @Override protected String orderSql() {
        return "INSERT INTO dispatch_orders (order_id, latitude, longitude, address, package_weight, priority, updated_at) "
             + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT (order_id) DO UPDATE SET "
             + "latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, address = EXCLUDED.address, "
             + "package_weight = EXCLUDED.package_weight, priority = EXCLUDED.priority, updated_at = CURRENT_TIMESTAMP";
    }
    @Override protected String vehicleSql() {
        return "INSERT INTO dispatch_vehicles (vehicle_id, capacity, current_latitude, current_longitude, current_address, updated_at) "
             + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT (vehicle_id) DO UPDATE SET "
             + "capacity = EXCLUDED.capacity, current_latitude = EXCLUDED.current_latitude, "
             + "current_longitude = EXCLUDED.current_longitude, current_address = EXCLUDED.current_address, "
             + "updated_at = CURRENT_TIMESTAMP";
    }
}
