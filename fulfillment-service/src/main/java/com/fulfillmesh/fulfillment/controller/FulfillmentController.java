package com.fulfillmesh.fulfillment.controller;

import com.fulfillmesh.fulfillment.dto.FulfillmentView;
import com.fulfillmesh.fulfillment.service.FulfillmentQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fulfillments")
public class FulfillmentController {
    private final FulfillmentQueryService service;

    public FulfillmentController(FulfillmentQueryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<FulfillmentView> all() {
        return service.all();
    }

    @GetMapping("/orders/{orderId}")
    public FulfillmentView byOrder(@PathVariable UUID orderId) {
        return service.byOrderId(orderId);
    }
}
