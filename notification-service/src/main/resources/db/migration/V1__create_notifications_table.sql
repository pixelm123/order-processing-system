CREATE TABLE IF NOT EXISTS notifications (
    id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id       UUID        NOT NULL,
    customer_id    VARCHAR(255) NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    message        TEXT         NOT NULL,
    correlation_id VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT notifications_type_check
        CHECK (type IN ('PAYMENT_PROCESSED','PAYMENT_FAILED','ORDER_COMPLETED'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_order_id ON notifications (order_id);
