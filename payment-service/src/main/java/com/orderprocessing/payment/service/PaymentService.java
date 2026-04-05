package com.orderprocessing.payment.service;

import com.orderprocessing.payment.domain.Payment;
import com.orderprocessing.payment.domain.PaymentStatus;
import com.orderprocessing.payment.dto.PaymentResponse;
import com.orderprocessing.payment.event.OrderCreatedEvent;
import com.orderprocessing.payment.event.PaymentFailedEvent;
import com.orderprocessing.payment.event.PaymentProcessedEvent;
import com.orderprocessing.payment.exception.PaymentNotFoundException;
import com.orderprocessing.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final Random RANDOM = new Random();

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    @Value("${app.kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    @Value("${app.payment.success-rate:0.80}")
    private double successRate;

    @Value("${app.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds;

    public PaymentService(PaymentRepository paymentRepository,
                          KafkaTemplate<String, Object> kafkaTemplate,
                          RedisTemplate<String, String> redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.orderId();

        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(idempotencyKey, "processed", Duration.ofSeconds(idempotencyTtlSeconds));

        if (Boolean.FALSE.equals(isNew)) {
            log.warn("[correlationId={}] Duplicate payment event received for orderId={} — skipping",
                event.correlationId(), event.orderId());
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setCustomerId(event.customerId());
        payment.setAmount(event.totalAmount());
        payment.setCorrelationId(event.correlationId());

        boolean success = RANDOM.nextDouble() < successRate;

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            Payment saved = paymentRepository.save(payment);

            var processedEvent = new PaymentProcessedEvent(
                saved.getId(), event.orderId(), event.customerId(),
                event.totalAmount(), event.correlationId(), Instant.now()
            );
            kafkaTemplate.send(paymentProcessedTopic, event.orderId().toString(), processedEvent);
            log.info("[correlationId={}] Payment COMPLETED for orderId={}", event.correlationId(), event.orderId());

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Simulated payment decline");
            Payment saved = paymentRepository.save(payment);

            var failedEvent = new PaymentFailedEvent(
                saved.getId(), event.orderId(), event.customerId(),
                "Simulated payment decline", event.correlationId(), Instant.now()
            );
            kafkaTemplate.send(paymentFailedTopic, event.orderId().toString(), failedEvent);
            log.warn("[correlationId={}] Payment FAILED for orderId={}", event.correlationId(), event.orderId());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
            .map(PaymentResponse::from)
            .orElseThrow(() -> new PaymentNotFoundException(orderId));
    }
}
