CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    tracking_number VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);
