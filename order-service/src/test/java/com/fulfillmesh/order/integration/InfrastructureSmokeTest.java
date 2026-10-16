package com.fulfillmesh.order.integration;

import com.fulfillmesh.common.events.OrderCreatedEvent;
import com.fulfillmesh.common.events.OrderLinePayload;
import com.fulfillmesh.common.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class InfrastructureSmokeTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("fulfillmesh")
                    .withUsername("fulfillmesh")
                    .withPassword("fulfillmesh");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.11"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> {
            String jdbcUrl = POSTGRES.getJdbcUrl();
            String separator = jdbcUrl.contains("?") ? "&" : "?";
            return jdbcUrl + separator + "currentSchema=orders";
        });
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.jwt.secret", () -> "integration-test-secret-that-is-long-enough-123456");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    KafkaTemplate<String, Object> kafka;

    @Test
    void postgresIsReachableAndFlywayRan() {
        Integer count = jdbc.queryForObject("select count(*) from orders.app_users", Integer.class);
        assertNotNull(count);
    }

    @Test
    void redisRoundTripWorks() {
        redis.opsForValue().set("integration:test", "ok");
        assertEquals("ok", redis.opsForValue().get("integration:test"));
    }

    @Test
    void kafkaProducerCanPublishJsonEvent() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "integration@example.com",
                List.of(new OrderLinePayload("SKU-A", 1)),
                Instant.now()
        );

        var result = kafka.send(Topics.ORDER_CREATED, event.orderId().toString(), event)
                .get(15, TimeUnit.SECONDS);

        assertNotNull(result.getRecordMetadata());
        assertEquals(Topics.ORDER_CREATED, result.getRecordMetadata().topic());
    }
}
