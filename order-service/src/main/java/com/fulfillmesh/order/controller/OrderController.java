package com.fulfillmesh.order.controller;

import com.fulfillmesh.order.dto.CreateOrderRequest;
import com.fulfillmesh.order.dto.OrderView;
import com.fulfillmesh.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService orders;

    public OrderController(OrderApplicationService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication
    ) {
        return orders.createOrder(request, authentication.getName(), idempotencyKey);
    }

    @GetMapping("/{id}")
    public OrderView get(@PathVariable UUID id, Authentication authentication) {
        return orders.getOrder(id, authentication.getName());
    }
}
