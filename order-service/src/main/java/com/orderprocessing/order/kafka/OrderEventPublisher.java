package com.orderprocessing.order.kafka;

import com.orderprocessing.order.domain.Order;
import com.orderprocessing.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topics.order-completed}")
    private String orderCompletedTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        var event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            order.getCorrelationId(),
            Instant.now()
        );
        kafkaTemplate.send(orderCreatedTopic, order.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[correlationId={}] Failed to publish order.created for orderId={}",
                        order.getCorrelationId(), order.getId(), ex);
                } else {
                    log.info("[correlationId={}] Published order.created for orderId={} partition={} offset={}",
                        order.getCorrelationId(), order.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }

    public void publishOrderCompleted(Order order) {
        var event = new OrderCreatedEvent(
            order.getId(),
            order.getCustomerId(),
            order.getTotalAmount(),
            order.getCorrelationId(),
            Instant.now()
        );
        kafkaTemplate.send(orderCompletedTopic, order.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[correlationId={}] Failed to publish order.completed for orderId={}",
                        order.getCorrelationId(), order.getId(), ex);
                } else {
                    log.info("[correlationId={}] Published order.completed for orderId={}",
                        order.getCorrelationId(), order.getId());
                }
            });
    }
}
