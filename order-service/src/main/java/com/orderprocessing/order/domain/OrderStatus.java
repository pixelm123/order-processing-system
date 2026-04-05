package com.orderprocessing.order.domain;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == SHIPPED;
            case SHIPPED    -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
