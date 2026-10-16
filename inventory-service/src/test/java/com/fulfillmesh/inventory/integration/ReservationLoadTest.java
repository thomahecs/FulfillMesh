package com.fulfillmesh.inventory.integration;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.inventory.entity.InventoryItem;
import com.fulfillmesh.inventory.repository.InventoryRepository;
import com.fulfillmesh.inventory.service.InventoryReservationService;
import com.fulfillmesh.inventory.service.ReservationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** A real PostgreSQL row-lock load test, not an in-memory inventory simulation. */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.datasource.hikari.maximum-pool-size=32"
})
@Testcontainers
class ReservationLoadTest {
    private static final int REQUESTS = 500;
    private static final int AVAILABLE = 100;
    private static final int CLIENT_THREADS = 48;
    private static final String SKU = "LOAD-TEST-SKU";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillmesh")
            .withUsername("fulfillmesh")
            .withPassword("fulfillmesh");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> {
            String jdbcUrl = POSTGRES.getJdbcUrl();
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            return jdbcUrl + separator + "currentSchema=inventory";
        });
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-that-is-long-enough-123456");
    }

    @Autowired InventoryRepository inventory;
    @Autowired InventoryReservationService reservations;

    @Test
    @Timeout(120)
    void fiveHundredReservationsAgainstOneHundredUnitsNeverOversell() throws Exception {
        inventory.deleteAll();
        inventory.saveAndFlush(new InventoryItem(SKU, "Load test", AVAILABLE, 2, 2));

        ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Sample>> futures = new ArrayList<>();
        long submissionStart = System.nanoTime();
        try {
            for (int i = 0; i < REQUESTS; i++) {
                UUID id = UUID.randomUUID();
                Callable<Sample> job = () -> {
                    start.await();
                    long begin = System.nanoTime();
                    ReservationResult result = reservations.reserve(new OrderCreatedEvent(
                            UUID.randomUUID(), id, "load@example.com",
                            List.of(new OrderLinePayload(SKU, 1)), Instant.now()));
                    long end = System.nanoTime();
                    return new Sample(result.success(), (end - begin) / 1_000_000.0,
                            (end - submissionStart) / 1_000_000.0);
                };
                futures.add(pool.submit(job));
            }
            start.countDown();
            List<Sample> samples = new ArrayList<>();
            for (Future<Sample> future : futures) samples.add(future.get(90, TimeUnit.SECONDS));

            long successes = samples.stream().filter(Sample::success).count();
            InventoryItem actual = inventory.findById(SKU).orElseThrow();
            assertEquals(AVAILABLE, successes);
            assertEquals(0, actual.getAvailableQuantity());
            assertEquals(AVAILABLE, actual.getReservedQuantity());
            assertEquals(REQUESTS - AVAILABLE, samples.size() - successes);

            List<Double> service = samples.stream().map(Sample::serviceMs).sorted().toList();
            List<Double> endToEnd = samples.stream().map(Sample::wallClockMs).sorted().toList();
            double p95Service = p95(service);
            double p95EndToEnd = p95(endToEnd);
            String json = String.format(Locale.ROOT,
                    "{\n  \"type\": \"PostgreSQL row-lock integration test\",\n" +
                            "  \"requests\": %d,\n  \"initial_stock\": %d,\n" +
                            "  \"client_threads\": %d,\n  \"db_pool_size\": 32,\n" +
                            "  \"successful_reservations\": %d,\n  \"rejected_reservations\": %d,\n" +
                            "  \"remaining_stock\": %d,\n  \"oversells\": %d,\n" +
                            "  \"service_p95_ms\": %.3f,\n  \"end_to_end_p95_ms\": %.3f\n}\n",
                    REQUESTS, AVAILABLE, CLIENT_THREADS, successes, REQUESTS - successes,
                    actual.getAvailableQuantity(), Math.max(0, successes - AVAILABLE),
                    p95Service, p95EndToEnd);
            Path report = Path.of("target", "benchmark", "concurrency-500.json");
            Files.createDirectories(report.getParent());
            Files.writeString(report, json);
            System.out.println("LOAD_REPORT " + json);
            if (Boolean.getBoolean("fulfillmesh.require.p95")) {
                assertTrue(p95EndToEnd <= 150.0,
                        "End-to-end p95 exceeded 150 ms (see target/benchmark/concurrency-500.json)");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static double p95(List<Double> sorted) {
        return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
    }

    private record Sample(boolean success, double serviceMs, double wallClockMs) {}
}
