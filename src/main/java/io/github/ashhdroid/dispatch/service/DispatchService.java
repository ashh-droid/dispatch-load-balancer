package io.github.ashhdroid.dispatch.service;

import io.github.ashhdroid.dispatch.distance.Coordinate;
import io.github.ashhdroid.dispatch.dto.DispatchPlanResponse;
import io.github.ashhdroid.dispatch.exception.InvalidStrategyException;
import io.github.ashhdroid.dispatch.mapper.DispatchPlanMapper;
import io.github.ashhdroid.dispatch.repository.OrderRepository;
import io.github.ashhdroid.dispatch.repository.VehicleRepository;
import io.github.ashhdroid.dispatch.strategy.DispatchOrder;
import io.github.ashhdroid.dispatch.strategy.DispatchStrategy;
import io.github.ashhdroid.dispatch.strategy.DispatchVehicle;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchService {
    private final OrderRepository orders;
    private final VehicleRepository vehicles;
    private final Map<String, DispatchStrategy> strategies;
    private final DispatchPlanMapper mapper;
    public DispatchService(OrderRepository orders, VehicleRepository vehicles,
                           List<DispatchStrategy> strategies, DispatchPlanMapper mapper) {
        this.orders = orders;
        this.vehicles = vehicles;
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(DispatchStrategy::key,
                Function.identity()));
        this.mapper = mapper;
    }
    @Transactional(readOnly = true)
    public DispatchPlanResponse plan() { return plan("greedy"); }
    @Transactional(readOnly = true)
    public DispatchPlanResponse plan(String name) {
        DispatchStrategy chosen = strategies.get(name);
        if (chosen == null) throw new InvalidStrategyException("strategy must be one of " + String.join(", ", new java.util.TreeSet<>(strategies.keySet())));
        var orderInputs = orders.findAll().stream().map(order -> new DispatchOrder(order.getOrderId(),
                new Coordinate(order.getLatitude(), order.getLongitude()), order.getAddress(),
                order.getPackageWeight(), order.getPriority())).toList();
        var fleet = vehicles.findAll().stream().map(vehicle -> new DispatchVehicle(
                vehicle.getVehicleId(), vehicle.getCapacity(),
                new Coordinate(vehicle.getCurrentLatitude(), vehicle.getCurrentLongitude()),
                vehicle.getCurrentAddress())).toList();
        return mapper.toResponse(chosen.plan(orderInputs, fleet), name);
    }
}
