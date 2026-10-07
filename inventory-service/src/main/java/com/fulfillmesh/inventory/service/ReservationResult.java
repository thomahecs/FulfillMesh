package com.fulfillmesh.inventory.service;

import com.fulfillmesh.common.events.ReservedItemPayload;

import java.util.List;

public record ReservationResult(boolean success, List<ReservedItemPayload> reservedItems, String reason) {
    public static ReservationResult accepted(List<ReservedItemPayload> items) {
        return new ReservationResult(true, items, null);
    }

    public static ReservationResult rejected(String reason) {
        return new ReservationResult(false, List.of(), reason);
    }
}
