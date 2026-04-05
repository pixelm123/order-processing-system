package com.orderprocessing.notification.service;

import com.orderprocessing.notification.domain.Notification;
import com.orderprocessing.notification.domain.NotificationType;
import com.orderprocessing.notification.dto.NotificationResponse;
import com.orderprocessing.notification.event.OrderCompletedEvent;
import com.orderprocessing.notification.event.PaymentFailedEvent;
import com.orderprocessing.notification.event.PaymentProcessedEvent;
import com.orderprocessing.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        String message = "Payment of %s successfully processed for order %s (customer: %s)"
            .formatted(event.amount(), event.orderId(), event.customerId());
        notificationRepository.save(buildNotification(
            event.orderId(), event.customerId(), NotificationType.PAYMENT_PROCESSED, message, event.correlationId()));
        log.info("[NOTIFICATION] [correlationId={}] [type={}] orderId={} — {}",
            event.correlationId(), NotificationType.PAYMENT_PROCESSED, event.orderId(), message);
    }

    public void handlePaymentFailed(PaymentFailedEvent event) {
        String message = "Payment FAILED for order %s (customer: %s). Reason: %s"
            .formatted(event.orderId(), event.customerId(), event.reason());
        notificationRepository.save(buildNotification(
            event.orderId(), event.customerId(), NotificationType.PAYMENT_FAILED, message, event.correlationId()));
        log.warn("[NOTIFICATION] [correlationId={}] [type={}] orderId={} — {}",
            event.correlationId(), NotificationType.PAYMENT_FAILED, event.orderId(), message);
    }

    public void handleOrderCompleted(OrderCompletedEvent event) {
        String message = "Order %s has been completed for customer %s. Total: %s"
            .formatted(event.orderId(), event.customerId(), event.totalAmount());
        notificationRepository.save(buildNotification(
            event.orderId(), event.customerId(), NotificationType.ORDER_COMPLETED, message, event.correlationId()));
        log.info("[NOTIFICATION] [correlationId={}] [type={}] orderId={} — {}",
            event.correlationId(), NotificationType.ORDER_COMPLETED, event.orderId(), message);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForOrder(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    private Notification buildNotification(UUID orderId, String customerId,
                                            NotificationType type, String message,
                                            String correlationId) {
        Notification n = new Notification();
        n.setOrderId(orderId);
        n.setCustomerId(customerId);
        n.setType(type);
        n.setMessage(message);
        n.setCorrelationId(correlationId);
        return n;
    }
}
