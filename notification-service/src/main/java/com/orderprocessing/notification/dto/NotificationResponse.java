package com.orderprocessing.notification.dto;

import com.orderprocessing.notification.domain.Notification;
import com.orderprocessing.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID orderId,
    String customerId,
    NotificationType type,
    String message,
    String correlationId,
    Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getOrderId(),
            notification.getCustomerId(),
            notification.getType(),
            notification.getMessage(),
            notification.getCorrelationId(),
            notification.getCreatedAt()
        );
    }
}
