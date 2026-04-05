package com.orderprocessing.order.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
    UUID paymentId,
    UUID orderId,
    String customerId,
    String reason,
    String correlationId,
    Instant occurredAt
) {}
