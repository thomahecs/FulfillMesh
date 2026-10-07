package com.fulfillmesh.inventory.service;

import com.fulfillmesh.inventory.dto.UpsertInventoryRequest;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryAdminServiceTest {
    private final InventoryRepository repository = mock(InventoryRepository.class);
    private final InventoryAdminService service = new InventoryAdminService(repository);

    @Test
    void listsInventory() {
        when(repository.findAll()).thenReturn(List.of(new InventoryItem("SKU-A", "Dock", 3, 2, 12)));

        var result = service.all();

        assertEquals(1, result.size());
        assertEquals("SKU-A", result.getFirst().sku());
    }

    @Test
    void updatesExistingSku() {
        InventoryItem item = new InventoryItem("SKU-A", "Old", 3, 2, 12);
        when(repository.findById("SKU-A")).thenReturn(Optional.of(item));
        when(repository.existsById("SKU-A")).thenReturn(true);
        when(repository.saveAndFlush(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.upsert("sku-a", new UpsertInventoryRequest("New", 8, 4, 5));

        assertEquals("New", result.name());
        assertEquals(8, result.availableQuantity());
    }
}
