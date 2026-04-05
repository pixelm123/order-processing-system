CREATE TABLE IF NOT EXISTS orders (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id      VARCHAR(255) NOT NULL,
    total_amount     NUMERIC(19, 2) NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    idempotency_key  VARCHAR(255) NOT NULL,
    correlation_id   VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT orders_status_check CHECK (status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')),
    CONSTRAINT orders_amount_positive CHECK (total_amount > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key ON orders (idempotency_key);
CREATE        INDEX IF NOT EXISTS idx_orders_customer_id     ON orders (customer_id);
