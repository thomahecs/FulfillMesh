CREATE TABLE inventory_items (
    sku VARCHAR(80) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    warehouse_x INTEGER NOT NULL CHECK (warehouse_x >= 0),
    warehouse_y INTEGER NOT NULL CHECK (warehouse_y >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

-- Coordinates are intentionally spread around the warehouse so the route
-- benchmark has a real multi-item path to optimize.
INSERT INTO inventory_items
    (sku, name, available_quantity, reserved_quantity, warehouse_x, warehouse_y, version)
VALUES
    ('SKU-A', 'USB-C Dock',          100, 0,  2, 12, 0),
    ('SKU-B', 'Mechanical Keyboard', 80, 0,  5, 12, 0),
    ('SKU-C', 'Wireless Mouse',      90, 0,  5,  2, 0),
    ('SKU-D', 'Laptop Stand',        70, 0,  8,  7, 0),
    ('SKU-E', 'Webcam',              75, 0, 14, 12, 0),
    ('SKU-F', 'USB-C Cable',        150, 0, 11, 12, 0),
    ('SKU-G', 'Portable SSD',        60, 0, 17, 17, 0),
    ('SKU-H', 'Headset',             95, 0, 11,  7, 0),
    ('SKU-I', 'Desk Mat',            50, 0,  2,  2, 0),
    ('SKU-J', 'Monitor Arm',         45, 0, 17,  2, 0),
    ('SKU-K', 'HDMI Cable',         140, 0,  2, 17, 0),
    ('SKU-L', 'Power Strip',        120, 0, 14,  7, 0);
