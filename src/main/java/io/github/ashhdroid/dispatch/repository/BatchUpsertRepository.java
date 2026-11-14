package io.github.ashhdroid.dispatch.repository;

import io.github.ashhdroid.dispatch.dto.OrderInput;
import io.github.ashhdroid.dispatch.dto.VehicleInput;
import java.util.List;

public interface BatchUpsertRepository {
    void upsertOrders(List<OrderInput> orders);
    void upsertVehicles(List<VehicleInput> vehicles);
}
