package io.github.ashhdroid.dispatch.strategy;

import java.util.List;

public interface DispatchStrategy {
    String key();
    DispatchPlan plan(List<DispatchOrder> orders, List<DispatchVehicle> vehicles);
}
