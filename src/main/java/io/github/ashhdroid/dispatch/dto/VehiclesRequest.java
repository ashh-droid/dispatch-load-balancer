package io.github.ashhdroid.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VehiclesRequest(@NotEmpty @Size(max = 10000) List<@NotNull @Valid VehicleInput> vehicles) {}
