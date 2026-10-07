# Architecture notes

FulfillMesh is split into four Spring Boot services plus one shared Java module.

```text
Client
  |
  v
order-service  -- orders.created -->  inventory-service
     ^                               |
     |                               | inventory.reserved / rejected
     |                               v
     +------------------------- fulfillment-service
     |                               |
     |                               | picking.completed
     |                               v
     +-------------------------- shipping-service
                                     |
                                     | shipping.created
                                     +-----------------------> order-service
```

## Service ownership

- `order-service`: authentication, order write model, order state, HTTP idempotency.
- `inventory-service`: inventory rows and stock reservation.
- `fulfillment-service`: picking task state plus the A* route planner.
- `shipping-service`: shipment creation and tracking number generation.
- `platform-common`: event payloads, topic names, and JWT token parsing.

Every service owns a separate PostgreSQL schema. They share one PostgreSQL server locally only to keep the setup small.

## Concurrency choices

### Inventory

The inventory repository reads reservation rows with `PESSIMISTIC_WRITE`. A multi-SKU order locks rows in SKU order before changing quantities. That prevents two workers from both seeing the same stock count and overselling it.

### Order and fulfillment state

Order, inventory, fulfillment, and shipment entities have `@Version` fields. Hibernate therefore rejects stale updates instead of letting a later write silently overwrite an earlier one.

### Idempotency

Redis is used in two places:

1. `Idempotency-Key` on order creation maps one client request to one order.
2. Kafka consumers store processed event ids with TTLs. A duplicate delivery is ignored.

The code deliberately deletes a consumer idempotency key when processing throws, so Kafka retries are still possible.

## Event delivery note

This project uses normal database transactions and Kafka sends, not a transactional outbox. That keeps the code understandable for a portfolio project. A production system with stricter delivery guarantees would add an outbox table or Kafka transactions.
