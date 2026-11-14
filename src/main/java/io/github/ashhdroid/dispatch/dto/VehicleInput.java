package io.github.ashhdroid.dispatch.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VehicleInput(
        @NotBlank @Size(max = 64) @Pattern(regexp = "\\S(?:.*\\S)?", message = "must not have surrounding whitespace") String vehicleId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 12, fraction = 3) BigDecimal capacity,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double currentLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double currentLongitude,
        @NotBlank @Size(max = 512) String currentAddress) {
}
