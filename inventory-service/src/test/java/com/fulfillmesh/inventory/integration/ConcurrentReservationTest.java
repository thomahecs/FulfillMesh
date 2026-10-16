package com.fulfillmesh.inventory.integration;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import com.fulfillmesh.inventory.service.InventoryReservationService;
import com.fulfillmesh.inventory.service.ReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class ConcurrentReservationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("fulfillmesh")
                    .withUsername("fulfillmesh")
                    .withPassword("fulfillmesh");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> {
            String jdbcUrl = POSTGRES.getJdbcUrl();
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            return jdbcUrl + separator + "currentSchema=inventory";
        });
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-that-is-long-enough-123456");
    }

    @Autowired
    InventoryRepository inventory;

    @Autowired
    InventoryReservationService reservations;

    @BeforeEach
    void resetSku() {
        inventory.deleteAll();
        inventory.saveAndFlush(new InventoryItem("SKU-X", "Concurrency Test", 1, 2, 2));
    }

    @Test
    void twoConcurrentOrdersCannotOversellOneUnit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<ReservationResult> job = () -> {
            start.await(5, TimeUnit.SECONDS);
            return reservations.reserve(event());
        };

        Future<ReservationResult> first = pool.submit(job);
        Future<ReservationResult> second = pool.submit(job);
        start.countDown();

        ReservationResult a = first.get(10, TimeUnit.SECONDS);
        ReservationResult b = second.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        long successCount = List.of(a, b).stream().filter(ReservationResult::success).count();
        InventoryItem finalItem = inventory.findById("SKU-X").orElseThrow();

        assertEquals(1, successCount);
        assertEquals(0, finalItem.getAvailableQuantity());
        assertEquals(1, finalItem.getReservedQuantity());
    }

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test@example.com",
                List.of(new OrderLinePayload("SKU-X", 1)),
                Instant.now()
        );
    }
}
