package com.orderprocessing.order.exception;

public class DuplicateOrderException extends RuntimeException {
    public DuplicateOrderException(String idempotencyKey) {
        super("Order already exists for idempotency key: " + idempotencyKey);
    }
}
