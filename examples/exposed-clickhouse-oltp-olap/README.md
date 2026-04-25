# bluetape4k-examples-exposed-clickhouse-oltp-olap

[한국어](./README.ko.md) | English

End-to-end example combining **PostgreSQL (OLTP)** and **ClickHouse (OLAP)** through Exposed,
demonstrating how to forward transactional records into an analytical store and run aggregate
queries with ClickHouse-native functions.

## Architecture

```mermaid
flowchart LR
    Client["Client / Service"] -->|insert order| PG[("PostgreSQL OLTP\nOrders")]
    PG -->|batch forward| CH[("ClickHouse OLAP\nOrderEvents")]
    CH -->|aggregate| Analytics["Analytics Result\n(count, uniqExact, quantile, argMax)"]
```

## Components

| Component             | Role                                                       |
|-----------------------|------------------------------------------------------------|
| `Orders` table        | PostgreSQL OLTP — single-row inserts in JDBC transaction   |
| `OrdersRepository`    | PostgreSQL synchronous repository                          |
| `OrderEvents` table   | ClickHouse OLAP — `MergeTree` partitioned by `region`      |
| `AnalyticsRepository` | Batch insert + aggregate query (`uniqExact`, `quantile`)   |

## Running

The integration test uses **Testcontainers** to spin up both PostgreSQL and ClickHouse:

```bash
./gradlew :bluetape4k-examples-exposed-clickhouse-oltp-olap:test
```

## Caveats

- ClickHouse is **not transactional** — failures during forwarding leave partial data.
  Implement idempotent forwarders (e.g. dedup by `order_id` with `ReplacingMergeTree`).
- Aggregate functions (`uniqExact`, `quantile`, `argMax`) are issued as raw SQL because
  the Exposed expression API does not yet model these ClickHouse-native functions.

## See Also

- [`bluetape4k-exposed-clickhouse`](../../data/exposed-clickhouse/README.md) — the underlying ClickHouse adapter
