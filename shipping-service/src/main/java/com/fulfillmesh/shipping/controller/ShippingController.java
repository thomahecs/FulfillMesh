package com.fulfillmesh.shipping.controller;

import com.fulfillmesh.shipping.dto.ShipmentView;
import com.fulfillmesh.shipping.service.ShippingQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {
    private final ShippingQueryService service;

    public ShippingController(ShippingQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ShipmentView> all() {
        return service.all();
    }

    @GetMapping("/orders/{orderId}")
    public ShipmentView byOrder(@PathVariable UUID orderId) {
        return service.byOrderId(orderId);
    }
}
