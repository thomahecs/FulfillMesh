package com.fulfillmesh.order.service;

import com.fulfillmesh.order.dto.CreateOrderLineRequest;
import com.fulfillmesh.order.dto.CreateOrderRequest;
import com.fulfillmesh.order.entity.OrderEntity;
import com.fulfillmesh.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderApplicationServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final OrderApplicationService service = new OrderApplicationService(orders, kafka, redis);

    @Test
    void createsOrderAndStoresIdempotencyMapping() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("PENDING"), eq(Duration.ofMinutes(10)))).thenReturn(true);
        when(orders.saveAndFlush(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateOrderRequest(List.of(
                new CreateOrderLineRequest("sku-a", 2),
                new CreateOrderLineRequest("sku-b", 1)
        ));

        var created = service.createOrder(request, "student@example.com", "req-1");

        assertEquals("student@example.com", created.customerEmail());
        assertEquals("SKU-A", created.items().getFirst().sku());
        assertEquals(2, created.items().size());
        verify(kafka).send(eq("orders.created"), eq(created.id().toString()), any());
        verify(values).set(
                eq("idem:order:student@example.com:req-1"),
                eq(created.id().toString()),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void returnsExistingOrderForCompletedIdempotencyKey() {
        UUID id = UUID.randomUUID();
        OrderEntity existing = new OrderEntity(id, "student@example.com", java.time.Instant.now());
        existing.addLine("SKU-A", 1);

        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("PENDING"), any(Duration.class))).thenReturn(false);
        when(values.get(anyString())).thenReturn(id.toString());
        when(orders.findByIdAndCustomerEmail(id, "student@example.com")).thenReturn(Optional.of(existing));

        var result = service.createOrder(
                new CreateOrderRequest(List.of(new CreateOrderLineRequest("SKU-A", 1))),
                "student@example.com",
                "req-1"
        );

        assertEquals(id, result.id());
        verify(kafka, never()).send(anyString(), anyString(), any());
    }

    @Test
    void rejectsDuplicateSkuInsideOneOrder() {
        var request = new CreateOrderRequest(List.of(
                new CreateOrderLineRequest("sku-a", 1),
                new CreateOrderLineRequest("SKU-A", 1)
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOrder(request, "student@example.com", "req-1")
        );
    }

    @Test
    void clearsPendingKeyWhenCreateFails() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("PENDING"), any(Duration.class))).thenReturn(true);
        when(orders.saveAndFlush(any(OrderEntity.class))).thenThrow(new RuntimeException("db down"));

        var request = new CreateOrderRequest(List.of(new CreateOrderLineRequest("SKU-A", 1)));

        assertThrows(
                RuntimeException.class,
                () -> service.createOrder(request, "student@example.com", "req-2")
        );
        verify(redis).delete("idem:order:student@example.com:req-2");
    }
    @Test
    void listsOnlyCustomerOrdersForRegularUser() {
        OrderEntity mine = new OrderEntity(UUID.randomUUID(), "student@example.com", java.time.Instant.now());
        mine.addLine("SKU-A", 1);
        when(orders.findAllByCustomerEmailOrderByCreatedAtDesc("student@example.com")).thenReturn(List.of(mine));

        var result = service.listOrders("student@example.com", false);

        assertEquals(1, result.size());
        assertEquals("student@example.com", result.getFirst().customerEmail());
        verify(orders, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void listsAllOrdersForAdmin() {
        OrderEntity first = new OrderEntity(UUID.randomUUID(), "one@example.com", java.time.Instant.now());
        OrderEntity second = new OrderEntity(UUID.randomUUID(), "two@example.com", java.time.Instant.now());
        when(orders.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(first, second));

        var result = service.listOrders("admin@example.com", true);

        assertEquals(2, result.size());
    }

    @Test
    void adminCanReadAnyOrderById() {
        UUID id = UUID.randomUUID();
        OrderEntity order = new OrderEntity(id, "customer@example.com", java.time.Instant.now());
        when(orders.findById(id)).thenReturn(Optional.of(order));

        var result = service.getOrder(id, "admin@example.com", true);

        assertEquals("customer@example.com", result.customerEmail());
    }

}
