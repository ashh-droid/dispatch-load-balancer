package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrdersRequest(@NotEmpty @Size(max = 10000) List<@NotNull @Valid OrderInput> orders) {}
