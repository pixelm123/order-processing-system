package com.orderprocessing.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
    UUID paymentId,
    UUID orderId,
    String customerId,
    BigDecimal amount,
    String correlationId,
    Instant occurredAt
) {}
