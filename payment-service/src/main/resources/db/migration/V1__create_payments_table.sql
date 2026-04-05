CREATE TABLE IF NOT EXISTS payments (
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    order_id       UUID           NOT NULL,
    customer_id    VARCHAR(255)   NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    status         VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    correlation_id VARCHAR(255),
    retry_count    INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT payments_status_check CHECK (status IN ('PENDING','COMPLETED','FAILED')),
    CONSTRAINT payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);
