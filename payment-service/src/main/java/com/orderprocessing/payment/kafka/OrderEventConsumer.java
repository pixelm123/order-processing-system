package com.orderprocessing.payment.kafka;

import com.orderprocessing.payment.event.OrderCreatedEvent;
import com.orderprocessing.payment.service.PaymentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.order-created}",
        groupId = "payment-service-group",
        containerFactory = "orderCreatedListenerFactory"
    )
    public void onOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        OrderCreatedEvent event = record.value();
        MDC.put("correlationId", event.correlationId());
        try {
            log.info("Received order.created for orderId={}", event.orderId());
            paymentService.processPayment(event);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
