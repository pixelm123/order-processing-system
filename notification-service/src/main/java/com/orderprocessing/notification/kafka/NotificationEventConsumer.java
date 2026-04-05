package com.orderprocessing.notification.kafka;

import com.orderprocessing.notification.event.OrderCompletedEvent;
import com.orderprocessing.notification.event.PaymentFailedEvent;
import com.orderprocessing.notification.event.PaymentProcessedEvent;
import com.orderprocessing.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.payment-processed}",
        groupId = "notification-service-group",
        containerFactory = "paymentProcessedListenerFactory"
    )
    public void onPaymentProcessed(ConsumerRecord<String, PaymentProcessedEvent> record) {
        PaymentProcessedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            notificationService.handlePaymentProcessed(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.payment-failed}",
        groupId = "notification-service-group",
        containerFactory = "paymentFailedListenerFactory"
    )
    public void onPaymentFailed(ConsumerRecord<String, PaymentFailedEvent> record) {
        PaymentFailedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            notificationService.handlePaymentFailed(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.order-completed}",
        groupId = "notification-service-group",
        containerFactory = "orderCompletedListenerFactory"
    )
    public void onOrderCompleted(ConsumerRecord<String, OrderCompletedEvent> record) {
        OrderCompletedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            notificationService.handleOrderCompleted(event);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
