package io.github.ashhdroid.dispatch.dto;

import io.github.ashhdroid.dispatch.domain.Priority;
import java.math.BigDecimal;

/** Read-only API representation, independent of request validation. */
public record OrderView(String orderId, double latitude, double longitude,
                        String address, BigDecimal packageWeight, Priority priority) {}
