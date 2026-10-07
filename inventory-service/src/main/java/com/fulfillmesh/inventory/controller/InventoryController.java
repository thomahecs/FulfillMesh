package com.fulfillmesh.inventory.controller;

import com.fulfillmesh.inventory.dto.InventoryView;
import com.fulfillmesh.inventory.dto.UpsertInventoryRequest;
import com.fulfillmesh.inventory.service.InventoryAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryAdminService service;

    public InventoryController(InventoryAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryView> all() {
        return service.all();
    }

    @PutMapping("/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryView upsert(@PathVariable String sku, @Valid @RequestBody UpsertInventoryRequest request) {
        return service.upsert(sku, request);
    }
}
