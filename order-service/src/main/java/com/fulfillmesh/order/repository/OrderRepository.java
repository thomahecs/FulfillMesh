package com.fulfillmesh.order.repository;

import com.fulfillmesh.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByIdAndCustomerEmail(UUID id, String customerEmail);
    List<OrderEntity> findAllByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    List<OrderEntity> findAllByOrderByCreatedAtDesc();
}
