package com.orderprocessing.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} → {1} should be allowed")
    @CsvSource({
        "PENDING,   CONFIRMED",
        "PENDING,   CANCELLED",
        "CONFIRMED, SHIPPED",
        "SHIPPED,   DELIVERED"
    })
    @DisplayName("Valid transitions are permitted")
    void validTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @Test
    @DisplayName("PENDING → CONFIRMED transitions the Order entity")
    void pendingToConfirmed_updatesStatus() {
        Order order = buildOrder();
        order.transitionTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PENDING → CANCELLED transitions the Order entity")
    void pendingToCancelled_updatesStatus() {
        Order order = buildOrder();
        order.transitionTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest(name = "{0} → {1} should be rejected")
    @CsvSource({
        "PENDING,   SHIPPED",
        "PENDING,   DELIVERED",
        "CONFIRMED, PENDING",
        "CONFIRMED, CANCELLED",
        "CONFIRMED, DELIVERED",
        "SHIPPED,   PENDING",
        "SHIPPED,   CONFIRMED",
        "SHIPPED,   CANCELLED",
        "DELIVERED, CONFIRMED",
        "DELIVERED, SHIPPED",
        "DELIVERED, CANCELLED",
        "CANCELLED, PENDING",
        "CANCELLED, CONFIRMED"
    })
    @DisplayName("Invalid transitions are rejected")
    void invalidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("transitionTo() throws IllegalStateException for invalid transition")
    void transitionTo_throwsOnInvalidTransition() {
        Order order = buildOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DELIVERED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING")
            .hasMessageContaining("DELIVERED");
    }

    @Test
    @DisplayName("Terminal states (DELIVERED, CANCELLED) cannot transition to anything")
    void terminalStates_cannotTransitionToAnything() {
        for (OrderStatus terminal : new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.CANCELLED}) {
            for (OrderStatus any : OrderStatus.values()) {
                assertThat(terminal.canTransitionTo(any))
                    .as("%s → %s should be forbidden", terminal, any)
                    .isFalse();
            }
        }
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setCustomerId("cust-1");
        order.setTotalAmount(new java.math.BigDecimal("99.99"));
        order.setIdempotencyKey("idem-key-1");
        return order;
    }
}
