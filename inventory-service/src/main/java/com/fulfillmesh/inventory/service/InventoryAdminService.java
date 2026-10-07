package com.fulfillmesh.inventory.service;

import com.fulfillmesh.inventory.dto.InventoryView;
import com.fulfillmesh.inventory.dto.UpsertInventoryRequest;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryAdminService {
    private final InventoryRepository inventory;

    public InventoryAdminService(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public List<InventoryView> all() {
        return inventory.findAll().stream()
                .map(InventoryAdminService::toView)
                .toList();
    }

    @Transactional
    public InventoryView upsert(String rawSku, UpsertInventoryRequest request) {
        String sku = rawSku.trim().toUpperCase();
        InventoryItem item = inventory.findById(sku)
                .orElseGet(() -> new InventoryItem(
                        sku,
                        request.name(),
                        request.availableQuantity(),
                        request.warehouseX(),
                        request.warehouseY()
                ));

        if (inventory.existsById(sku)) {
            item.replaceStock(
                    request.name(),
                    request.availableQuantity(),
                    request.warehouseX(),
                    request.warehouseY()
            );
        }

        return toView(inventory.saveAndFlush(item));
    }

    public static InventoryView toView(InventoryItem item) {
        return new InventoryView(
                item.getSku(),
                item.getName(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getWarehouseX(),
                item.getWarehouseY(),
                item.getVersion()
        );
    }
}
