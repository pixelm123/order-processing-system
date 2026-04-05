package com.orderprocessing.payment.service;

import com.orderprocessing.payment.domain.Payment;
import com.orderprocessing.payment.domain.PaymentStatus;
import com.orderprocessing.payment.event.OrderCreatedEvent;
import com.orderprocessing.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("processPayment: idempotency guard — duplicate event is skipped entirely")
    void processPayment_duplicateEvent_skipped() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.FALSE);

        paymentService.processPayment(buildEvent(UUID.randomUUID()));

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("processPayment: 100% success rate → saves COMPLETED payment and publishes payment.processed")
    void processPayment_success_publishesPaymentProcessed() {
        setSuccessRate(1.0);
        UUID orderId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.TRUE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(buildSavedPayment(orderId, PaymentStatus.COMPLETED));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        paymentService.processPayment(buildEvent(orderId));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(kafkaTemplate).send(eq("payment.processed"), eq(orderId.toString()), any());
    }

    @Test
    @DisplayName("processPayment: 0% success rate → saves FAILED payment and publishes payment.failed")
    void processPayment_failure_publishesPaymentFailed() {
        setSuccessRate(0.0);
        UUID orderId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.TRUE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(buildSavedPayment(orderId, PaymentStatus.FAILED));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        paymentService.processPayment(buildEvent(orderId));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(kafkaTemplate).send(eq("payment.failed"), eq(orderId.toString()), any());
    }

    @Test
    @DisplayName("processPayment: first event sets idempotency key in Redis")
    void processPayment_firstEvent_setsRedisKey() {
        setSuccessRate(1.0);
        UUID orderId = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.TRUE);
        when(paymentRepository.save(any())).thenReturn(buildSavedPayment(orderId, PaymentStatus.COMPLETED));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        paymentService.processPayment(buildEvent(orderId));

        verify(valueOps).setIfAbsent(eq("payment:idempotency:" + orderId), eq("processed"), any());
    }

    private void setSuccessRate(double rate) {
        ReflectionTestUtils.setField(paymentService, "successRate", rate);
        ReflectionTestUtils.setField(paymentService, "paymentProcessedTopic", "payment.processed");
        ReflectionTestUtils.setField(paymentService, "paymentFailedTopic", "payment.failed");
        ReflectionTestUtils.setField(paymentService, "idempotencyTtlSeconds", 3600L);
    }

    private OrderCreatedEvent buildEvent(UUID orderId) {
        return new OrderCreatedEvent(orderId, "cust-1", new BigDecimal("99.00"), "corr-1", Instant.now());
    }

    private Payment buildSavedPayment(UUID orderId, PaymentStatus status) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setCustomerId("cust-1");
        p.setAmount(new BigDecimal("99.00"));
        p.setStatus(status);
        if (status == PaymentStatus.FAILED) p.setFailureReason("Simulated payment decline");
        try {
            var idField = Payment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }
}
