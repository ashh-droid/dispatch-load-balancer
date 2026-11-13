package com.freightfox.dispatch.dto;

import com.freightfox.dispatch.domain.Priority;
import java.math.BigDecimal;

/** Read-only API representation, independent of request validation. */
public record OrderView(String orderId, double latitude, double longitude,
                        String address, BigDecimal packageWeight, Priority priority) {}
