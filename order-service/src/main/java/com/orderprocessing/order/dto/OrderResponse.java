package com.orderprocessing.order.dto;

import com.orderprocessing.order.domain.Order;
import com.orderprocessing.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String customerId,
    BigDecimal totalAmount,
    OrderStatus status,
    String correlationId,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCorrelationId(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
