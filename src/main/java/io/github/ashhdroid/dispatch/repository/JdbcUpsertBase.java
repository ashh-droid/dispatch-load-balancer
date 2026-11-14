package io.github.ashhdroid.dispatch.repository;

import io.github.ashhdroid.dispatch.dto.OrderInput;
import io.github.ashhdroid.dispatch.dto.VehicleInput;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class JdbcUpsertBase implements BatchUpsertRepository {
    protected final JdbcTemplate jdbc;
    protected JdbcUpsertBase(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    protected abstract String orderSql();
    protected abstract String vehicleSql();

    @Override
    public void upsertOrders(List<OrderInput> orders) {
        jdbc.batchUpdate(orderSql(), orders, 500, (statement, order) -> {
            statement.setString(1, order.orderId());
            statement.setDouble(2, order.latitude());
            statement.setDouble(3, order.longitude());
            statement.setString(4, order.address());
            statement.setBigDecimal(5, order.packageWeight());
            statement.setString(6, order.priority().name());
        });
    }
    @Override
    public void upsertVehicles(List<VehicleInput> vehicles) {
        jdbc.batchUpdate(vehicleSql(), vehicles, 500, (statement, vehicle) -> {
            statement.setString(1, vehicle.vehicleId());
            statement.setBigDecimal(2, vehicle.capacity());
            statement.setDouble(3, vehicle.currentLatitude());
            statement.setDouble(4, vehicle.currentLongitude());
            statement.setString(5, vehicle.currentAddress());
        });
    }
}
