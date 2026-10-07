package com.fulfillmesh.common.events;

public record ReservedItemPayload(String sku, int quantity, int x, int y) {
}
