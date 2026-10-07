package com.fulfillmesh.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(@NotEmpty List<@Valid CreateOrderLineRequest> items) {
}
