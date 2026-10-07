package com.fulfillmesh.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderLineRequest(@NotBlank String sku, @Min(1) int quantity) {
}
