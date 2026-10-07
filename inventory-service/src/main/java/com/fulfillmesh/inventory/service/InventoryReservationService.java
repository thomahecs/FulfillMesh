package com.fulfillmesh.inventory.service;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.common.events.ReservedItemPayload;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryReservationService {
    private final InventoryRepository inventory;

    public InventoryReservationService(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Transactional
    public ReservationResult reserve(OrderCreatedEvent event) {
        // Lock SKUs in a stable order. This reduces the chance of deadlocks when
        // two multi-item orders touch the same rows in a different request order.
        List<OrderLinePayload> requested = event.items().stream()
                .sorted(Comparator.comparing(OrderLinePayload::sku))
                .toList();

        List<InventoryItem> locked = new ArrayList<>();
        for (OrderLinePayload line : requested) {
            InventoryItem item = inventory.findBySkuForUpdate(line.sku())
                    .orElse(null);
            if (item == null) {
                return ReservationResult.rejected("Unknown SKU: " + line.sku());
            }
            if (item.getAvailableQuantity() < line.quantity()) {
                return ReservationResult.rejected(
                        "Insufficient stock for " + line.sku()
                                + " (requested=" + line.quantity()
                                + ", available=" + item.getAvailableQuantity() + ")"
                );
            }
            locked.add(item);
        }

        List<ReservedItemPayload> reserved = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            OrderLinePayload line = requested.get(i);
            InventoryItem item = locked.get(i);
            item.reserve(line.quantity());
            reserved.add(new ReservedItemPayload(
                    item.getSku(),
                    line.quantity(),
                    item.getWarehouseX(),
                    item.getWarehouseY()
            ));
        }

        inventory.saveAll(locked);
        inventory.flush();
        return ReservationResult.accepted(reserved);
    }
}
