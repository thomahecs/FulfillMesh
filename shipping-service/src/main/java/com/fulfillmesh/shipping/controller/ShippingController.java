package com.fulfillmesh.shipping.controller;

import com.fulfillmesh.shipping.dto.ShipmentView;
import com.fulfillmesh.shipping.service.ShippingQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {
    private final ShippingQueryService service;

    public ShippingController(ShippingQueryService service) {
        this.service = service;
    }

    @GetMapping("/orders/{orderId}")
    public ShipmentView byOrder(@PathVariable UUID orderId) {
        return service.byOrderId(orderId);
    }
}
