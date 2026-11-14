package io.github.ashhdroid.dispatch.strategy;

import io.github.ashhdroid.dispatch.domain.Priority;
import io.github.ashhdroid.dispatch.distance.Coordinate;
import java.math.BigDecimal;

public record DispatchOrder(String orderId, Coordinate location, String address,
                            BigDecimal packageWeight, Priority priority) {}
