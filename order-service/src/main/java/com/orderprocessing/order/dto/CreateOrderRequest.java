package com.orderprocessing.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(

    @NotBlank(message = "customerId is required")
    String customerId,

    @NotNull(message = "totalAmount is required")
    @DecimalMin(value = "0.01", message = "totalAmount must be positive")
    BigDecimal totalAmount,

    /** Client-generated idempotency key — used to deduplicate order creation retries. */
    @NotBlank(message = "idempotencyKey is required")
    String idempotencyKey
) {}
