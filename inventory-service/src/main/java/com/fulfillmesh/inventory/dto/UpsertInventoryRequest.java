package com.fulfillmesh.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpsertInventoryRequest(
        @NotBlank String name,
        @Min(0) int availableQuantity,
        @Min(0) int warehouseX,
        @Min(0) int warehouseY
) {
}
