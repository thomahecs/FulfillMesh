package com.fulfillmesh.order.entity;

public enum OrderState {
    CREATED,
    INVENTORY_RESERVED,
    INVENTORY_REJECTED,
    PICKING,
    SHIPPED
}
