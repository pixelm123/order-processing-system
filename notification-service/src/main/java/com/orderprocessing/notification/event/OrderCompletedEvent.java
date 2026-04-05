package com.orderprocessing.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCompletedEvent(
    UUID orderId,
    String customerId,
    BigDecimal totalAmount,
    String correlationId,
    Instant occurredAt
) {}
