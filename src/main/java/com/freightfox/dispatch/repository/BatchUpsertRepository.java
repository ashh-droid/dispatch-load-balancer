package com.freightfox.dispatch.repository;

import com.freightfox.dispatch.dto.OrderInput;
import com.freightfox.dispatch.dto.VehicleInput;
import java.util.List;

public interface BatchUpsertRepository {
    void upsertOrders(List<OrderInput> orders);
    void upsertVehicles(List<VehicleInput> vehicles);
}
