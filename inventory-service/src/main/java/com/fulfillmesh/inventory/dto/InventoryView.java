package com.fulfillmesh.inventory.dto;

public record InventoryView(
        String sku,
        String name,
        int availableQuantity,
        int reservedQuantity,
        int warehouseX,
        int warehouseY,
        long version
) {
}
