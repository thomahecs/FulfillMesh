package com.fulfillmesh.order.service;

import com.fulfillmesh.order.entity.OrderEntity;
import com.fulfillmesh.order.entity.OrderState;
import com.fulfillmesh.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderStateService {
    private static final Map<OrderState, Integer> PROGRESS = Map.of(
            OrderState.CREATED, 0,
            OrderState.INVENTORY_RESERVED, 1,
            OrderState.PICKING, 2,
            OrderState.SHIPPED, 3
    );

    private final OrderRepository orders;

    public OrderStateService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public void move(UUID orderId, OrderState next) {
        OrderEntity order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!shouldAdvance(order.getState(), next)) {
            return;
        }

        // OrderEntity has @Version. saveAndFlush forces the version check to
        // happen inside this transaction instead of being delayed.
        order.moveTo(next);
        orders.saveAndFlush(order);
    }

    boolean shouldAdvance(OrderState current, OrderState next) {
        if (current == OrderState.INVENTORY_REJECTED) {
            return false;
        }

        if (next == OrderState.INVENTORY_REJECTED) {
            return current == OrderState.CREATED;
        }

        Integer currentRank = PROGRESS.get(current);
        Integer nextRank = PROGRESS.get(next);
        return currentRank != null && nextRank != null && nextRank > currentRank;
    }
}
