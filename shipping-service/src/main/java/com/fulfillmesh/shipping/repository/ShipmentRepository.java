package com.fulfillmesh.shipping.repository;

import com.fulfillmesh.shipping.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID orderId);
    List<Shipment> findAllByOrderByCreatedAtDesc();
}
