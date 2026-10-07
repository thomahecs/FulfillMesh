package com.fulfillmesh.order.dto;

import com.fulfillmesh.order.entity.OrderState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
        UUID id,
        String customerEmail,
        OrderState state,
        long version,
        Instant createdAt,
        List<OrderLineView> items
) {
}
