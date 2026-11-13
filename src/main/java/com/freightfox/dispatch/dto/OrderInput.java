package com.freightfox.dispatch.dto;

import com.freightfox.dispatch.domain.Priority;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderInput(
        @NotBlank @Size(max = 64) @Pattern(regexp = "\\S(?:.*\\S)?", message = "must not have surrounding whitespace") String orderId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank @Size(max = 512) String address,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 12, fraction = 3) BigDecimal packageWeight,
        @NotNull Priority priority) {
}
