---
title: "Failures, testing, and ecosystem paths"
description: "Set error, cancellation, and observation boundaries, then progress from lightweight checks to real Cassandra verification."
---

# Failures, testing, and ecosystem paths

## Separate three failure layers

Do not collapse every Cassandra outcome into one backend exception.

1. **Call-contract errors:** invalid CQL, bind-marker count or type, row mapping, and nullability
2. **Distributed-system failures:** unavailable, timeout, overload, schema disagreement, and connection failure
3. **Condition not applied:** an LWT request completed but `wasApplied()` is false

Coroutine adapters do not replace these with another exception hierarchy. Translate them at a repository or service boundary only when it can preserve cause and retryability in a stable domain contract.

## Cancellation and timeout

Canceling a reactive subscription or future wait does not reveal the final state of a query already sent to the cluster. A write timeout in particular can leave application status uncertain. Design driver idempotence, retry policy, LWT, and read-after-timeout checks together.

Do not record or retry `CancellationException` as a normal backend failure. Before increasing timeouts, inspect query shape, partition size, pools, and coordinator health.

## Observation responsibility

This artifact contains no health indicator, `ObservationRegistry`, meter bean, or auto-configuration. The application adds Spring Boot Actuator and Cassandra driver Micrometer integration.

Useful signals include:

- session connectivity and available nodes;
- request latency and timeout rate;
- unavailable, overloaded, authentication, and schema errors;
- connection-pool in-flight and queue counts;
- LWT latency and not-applied rate;
- item count and memory use for Flow batches.

Do not put raw CQL or bind values into metric labels. That creates high cardinality and can expose sensitive values.

## Start with the smallest tests

Mock-based unit tests verify that coroutine adapters call the right Spring Data operation and preserve values and failures. `OptionsSupportTest` and `AbstractCassandraModelTest` check statement options and model contracts without Cassandra.

```bash
./gradlew :bluetape4k-spring-boot-cassandra:test \
  --tests '*UnitTest' \
  --tests '*OptionsSupportTest' \
  --tests '*AbstractCassandraModelTest'
```

Mapping, schema, optimistic locking, and real queries require Cassandra:

```bash
./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache
```

This suite starts `CassandraServer.Launcher.cassandra4`. Run it sequentially with other Testcontainers, native, and emulator checks.

## What the fixtures teach

- `ReactiveSessionCoroutinesExamples`: minimal execute and prepare calls
- `ReactiveCassandraTemplateTest`: reactive CRUD and slices
- `AsyncCassandraTemplateTest`: async+suspend CRUD and options
- `AsyncOptimisticLockingTest`: version increments and stale-entity failures
- `CassandraTypeMappingTest`: Spring Data converters and Cassandra type mapping
- `SchemaGeneratorTest`: table existence and creation boundaries

The shared test `CqlSession` is more than a speed optimization. Creating a new session for every application context accumulates connections and can end in `AllNodesFailedException`.

## Ecosystem learning paths

Choose the next module by abstraction level:

- driver statements, paging, and CQL helpers: [`bluetape4k-cassandra`](../bluetape4k-cassandra.md)
- coroutine cancellation and Flow fundamentals: [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- common Spring context and execution boundaries: [`bluetape4k-spring-boot-core`](../bluetape4k-spring-boot-core.md)
- comparison with relational async access: [`bluetape4k-spring-boot-r2dbc`](../bluetape4k-spring-boot-r2dbc.md)

Cassandra repositories may resemble relational repositories, but the query and transaction models differ. Start the data model from access patterns and use coroutines as the call representation above that model.

## Sources

- [`AsyncCassandraOperationsCoroutinesUnitTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutinesUnitTest.kt)
- [`AsyncOptimisticLockingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/async/AsyncOptimisticLockingTest.kt)
- [`CassandraTypeMappingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/convert/CassandraTypeMappingTest.kt)
- [`AbstractCassandraTestConfiguration.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractCassandraTestConfiguration.kt)
