package com.fulfillmesh.common.events;

public final class Topics {
    public static final String ORDER_CREATED = "orders.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";
    public static final String PICKING_COMPLETED = "picking.completed";
    public static final String SHIPMENT_CREATED = "shipping.created";

    private Topics() {
    }
}
