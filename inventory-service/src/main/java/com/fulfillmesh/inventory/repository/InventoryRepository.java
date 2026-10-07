package com.fulfillmesh.inventory.repository;

import com.fulfillmesh.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {

    // Reservations use a database row lock so two orders cannot decrement the
    // same SKU at the same time and oversell it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.sku = :sku")
    Optional<InventoryItem> findBySkuForUpdate(@Param("sku") String sku);
}
