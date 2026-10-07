package com.fulfillmesh.inventory.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "warehouse_x", nullable = false)
    private int warehouseX;

    @Column(name = "warehouse_y", nullable = false)
    private int warehouseY;

    @Version
    private long version;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, String name, int availableQuantity, int warehouseX, int warehouseY) {
        this.sku = sku;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
        this.warehouseX = warehouseX;
        this.warehouseY = warehouseY;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (availableQuantity < quantity) {
            throw new IllegalStateException("Not enough stock for " + sku);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void replaceStock(String name, int availableQuantity, int warehouseX, int warehouseY) {
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.warehouseX = warehouseX;
        this.warehouseY = warehouseY;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getWarehouseX() { return warehouseX; }
    public int getWarehouseY() { return warehouseY; }
    public long getVersion() { return version; }
}
