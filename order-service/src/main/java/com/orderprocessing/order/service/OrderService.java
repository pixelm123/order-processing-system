package com.orderprocessing.order.service;

import com.orderprocessing.order.config.CorrelationIdFilter;
import com.orderprocessing.order.domain.Order;
import com.orderprocessing.order.domain.OrderStatus;
import com.orderprocessing.order.dto.CreateOrderRequest;
import com.orderprocessing.order.dto.OrderResponse;
import com.orderprocessing.order.exception.DuplicateOrderException;
import com.orderprocessing.order.exception.OrderNotFoundException;
import com.orderprocessing.order.kafka.OrderEventPublisher;
import com.orderprocessing.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_CACHE = "orders";

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        orderRepository.findByIdempotencyKey(request.idempotencyKey())
            .ifPresent(existing -> { throw new DuplicateOrderException(request.idempotencyKey()); });

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setTotalAmount(request.totalAmount());
        order.setIdempotencyKey(request.idempotencyKey());
        order.setCorrelationId(correlationId);

        Order saved = orderRepository.save(order);
        log.info("Created order id={} customerId={}", saved.getId(), saved.getCustomerId());

        eventPublisher.publishOrderCreated(saved);

        return OrderResponse.from(saved);
    }

    @Cacheable(value = ORDER_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return orderRepository.findById(id)
            .map(OrderResponse::from)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public void confirmOrder(UUID orderId, String correlationId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.transitionTo(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("[correlationId={}] Order {} transitioned to CONFIRMED", correlationId, orderId);

        eventPublisher.publishOrderCompleted(order);
    }

    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public void cancelOrder(UUID orderId, String correlationId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.transitionTo(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.warn("[correlationId={}] Order {} transitioned to CANCELLED", correlationId, orderId);
    }
}
