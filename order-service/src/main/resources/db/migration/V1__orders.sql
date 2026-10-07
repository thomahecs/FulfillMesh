CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_email VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE order_lines (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
