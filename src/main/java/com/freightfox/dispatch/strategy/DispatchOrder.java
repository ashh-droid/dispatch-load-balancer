package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.domain.Priority;
import com.freightfox.dispatch.distance.Coordinate;
import java.math.BigDecimal;

public record DispatchOrder(String orderId, Coordinate location, String address,
                            BigDecimal packageWeight, Priority priority) {}
