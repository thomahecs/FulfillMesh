package com.fulfillmesh.order.service;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.common.events.Topics;
import com.fulfillmesh.order.dto.*;
import com.fulfillmesh.order.entity.OrderEntity;
import com.fulfillmesh.order.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orders;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;

    public OrderApplicationService(
            OrderRepository orders,
            KafkaTemplate<String, Object> kafka,
            StringRedisTemplate redis
    ) {
        this.orders = orders;
        this.kafka = kafka;
        this.redis = redis;
    }

    @Transactional
    public OrderView createOrder(CreateOrderRequest request, String customerEmail, String idempotencyKey) {
        validateNoDuplicateSkus(request);

        // The Redis key is acquired before the database write. If the same HTTP request
        // is retried, we return the original order instead of creating another one.
        String key = "idem:order:" + customerEmail + ":" + idempotencyKey;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "PENDING", Duration.ofMinutes(10));

        if (!Boolean.TRUE.equals(acquired)) {
            String existing = redis.opsForValue().get(key);
            if (existing == null || "PENDING".equals(existing)) {
                throw new IllegalStateException("An order with this idempotency key is still being created");
            }
            return getOrder(UUID.fromString(existing), customerEmail);
        }

        try {
            OrderView created = createFreshOrder(request, customerEmail);
            redis.opsForValue().set(key, created.id().toString(), Duration.ofHours(24));
            return created;
        } catch (RuntimeException ex) {
            redis.delete(key);
            throw ex;
        }
    }

    @Transactional
    protected OrderView createFreshOrder(CreateOrderRequest request, String customerEmail) {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), customerEmail, Instant.now());
        for (CreateOrderLineRequest item : request.items()) {
            order.addLine(item.sku().trim().toUpperCase(), item.quantity());
        }

        orders.saveAndFlush(order);

        List<OrderLinePayload> items = order.getLines().stream()
                .map(line -> new OrderLinePayload(line.getSku(), line.getQuantity()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                customerEmail,
                items,
                Instant.now()
        );
        kafka.send(Topics.ORDER_CREATED, order.getId().toString(), event);
        return toView(order);
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(UUID id, String customerEmail) {
        OrderEntity order = orders.findByIdAndCustomerEmail(id, customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public OrderView getOrder(UUID id, String customerEmail, boolean admin) {
        OrderEntity order = admin
                ? orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"))
                : orders.findByIdAndCustomerEmail(id, customerEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toView(order);
    }

    @Transactional(readOnly = true)
    public List<OrderView> listOrders(String customerEmail, boolean admin) {
        List<OrderEntity> result = admin
                ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findAllByCustomerEmailOrderByCreatedAtDesc(customerEmail);
        return result.stream().map(OrderApplicationService::toView).toList();
    }

    private void validateNoDuplicateSkus(CreateOrderRequest request) {
        Set<String> seen = new HashSet<>();
        for (CreateOrderLineRequest item : request.items()) {
            String sku = item.sku().trim().toUpperCase();
            if (!seen.add(sku)) {
                throw new IllegalArgumentException("Duplicate SKU in one order: " + sku);
            }
        }
    }

    public static OrderView toView(OrderEntity order) {
        List<OrderLineView> lines = order.getLines().stream()
                .map(line -> new OrderLineView(line.getSku(), line.getQuantity()))
                .toList();
        return new OrderView(
                order.getId(),
                order.getCustomerEmail(),
                order.getState(),
                order.getVersion(),
                order.getCreatedAt(),
                lines
        );
    }
}
