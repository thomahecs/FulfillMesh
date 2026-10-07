package com.fulfillmesh.order.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_lines")
public class OrderLineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    protected OrderLineEntity() {
    }

    public OrderLineEntity(OrderEntity order, String sku, int quantity) {
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
