package com.fulfillmesh.order.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState state;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id asc")
    private List<OrderLineEntity> lines = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(UUID id, String customerEmail, Instant createdAt) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.createdAt = createdAt;
        this.state = OrderState.CREATED;
    }

    public void addLine(String sku, int quantity) {
        this.lines.add(new OrderLineEntity(this, sku, quantity));
    }

    public void moveTo(OrderState next) {
        this.state = next;
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderState getState() { return state; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLineEntity> getLines() { return lines; }
}
