package com.orderprocessing.order.service;

import com.orderprocessing.order.domain.Order;
import com.orderprocessing.order.domain.OrderStatus;
import com.orderprocessing.order.dto.CreateOrderRequest;
import com.orderprocessing.order.dto.OrderResponse;
import com.orderprocessing.order.exception.DuplicateOrderException;
import com.orderprocessing.order.exception.OrderNotFoundException;
import com.orderprocessing.order.kafka.OrderEventPublisher;
import com.orderprocessing.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MDC.put("correlationId", "test-correlation-id");
    }

    @Test
    @DisplayName("createOrder: happy path — saves order and publishes event")
    void createOrder_happyPath() {
        var request = new CreateOrderRequest("cust-1", new BigDecimal("49.99"), "idem-key-1");

        when(orderRepository.findByIdempotencyKey("idem-key-1")).thenReturn(Optional.empty());
        Order savedOrder = buildSavedOrder("idem-key-1");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.customerId()).isEqualTo("cust-1");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(eventPublisher).publishOrderCreated(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("idem-key-1");
    }

    @Test
    @DisplayName("createOrder: duplicate idempotency key throws DuplicateOrderException")
    void createOrder_duplicateIdempotencyKey_throwsDuplicateOrderException() {
        var request = new CreateOrderRequest("cust-1", new BigDecimal("49.99"), "idem-key-dup");

        when(orderRepository.findByIdempotencyKey("idem-key-dup"))
            .thenReturn(Optional.of(buildSavedOrder("idem-key-dup")));

        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(DuplicateOrderException.class)
            .hasMessageContaining("idem-key-dup");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("getOrder: missing order throws OrderNotFoundException")
    void getOrder_notFound_throwsOrderNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(missingId))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessageContaining(missingId.toString());
    }

    @Test
    @DisplayName("confirmOrder: transitions PENDING → CONFIRMED and publishes order.completed")
    void confirmOrder_pendingToConfirmed() {
        Order order = buildSavedOrder("idem-key-1");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.confirmOrder(order.getId(), "corr-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(eventPublisher).publishOrderCompleted(order);
    }

    @Test
    @DisplayName("confirmOrder: CANCELLED order throws IllegalStateException (state machine)")
    void confirmOrder_cancelledOrder_throwsIllegalState() {
        Order order = buildSavedOrder("idem-key-1");
        order.transitionTo(OrderStatus.CANCELLED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmOrder(order.getId(), "corr-1"))
            .isInstanceOf(IllegalStateException.class);

        verify(eventPublisher, never()).publishOrderCompleted(any());
    }

    @Test
    @DisplayName("cancelOrder: transitions PENDING → CANCELLED")
    void cancelOrder_pendingToCancelled() {
        Order order = buildSavedOrder("idem-key-1");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        orderService.cancelOrder(order.getId(), "corr-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private Order buildSavedOrder(String idempotencyKey) {
        Order order = new Order();
        order.setCustomerId("cust-1");
        order.setTotalAmount(new BigDecimal("49.99"));
        order.setIdempotencyKey(idempotencyKey);
        order.setCorrelationId("test-correlation-id");
        try {
            var idField = Order.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return order;
    }
}
