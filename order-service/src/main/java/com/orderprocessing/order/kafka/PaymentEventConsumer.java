package com.orderprocessing.order.kafka;

import com.orderprocessing.order.event.PaymentFailedEvent;
import com.orderprocessing.order.event.PaymentProcessedEvent;
import com.orderprocessing.order.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.payment-processed}",
        groupId = "order-service-group",
        containerFactory = "paymentProcessedListenerFactory"
    )
    public void onPaymentProcessed(ConsumerRecord<String, PaymentProcessedEvent> record) {
        PaymentProcessedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            log.info("Received payment.processed for orderId={}", event.orderId());
            orderService.confirmOrder(event.orderId(), event.correlationId());
        } finally {
            MDC.remove("correlationId");
        }
    }

    @KafkaListener(
        topics = "${app.kafka.topics.payment-failed}",
        groupId = "order-service-group",
        containerFactory = "paymentFailedListenerFactory"
    )
    public void onPaymentFailed(ConsumerRecord<String, PaymentFailedEvent> record) {
        PaymentFailedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            log.warn("Received payment.failed for orderId={} reason={}", event.orderId(), event.reason());
            orderService.cancelOrder(event.orderId(), event.correlationId());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
