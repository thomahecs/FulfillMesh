CREATE TABLE fulfillment_tasks (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    state VARCHAR(50) NOT NULL,
    optimized_distance INTEGER,
    sequential_distance INTEGER,
    improvement_percent DOUBLE PRECISION,
    pick_route TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
