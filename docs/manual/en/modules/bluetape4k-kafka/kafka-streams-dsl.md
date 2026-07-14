---
title: Kafka Streams DSL factories
description: Explains Consumed, Produced, Materialized, join/repartition/branch helpers and topology/state-store ownership.
manualId: bluetape4k-kafka
chapterId: kafka-streams-dsl
---

# Kafka Streams DSL factories

## Factories, not a replacement DSL

The `streams.kstream` package creates Kafka Streams parameter objects with Kotlin functions. It does not create a `StreamsBuilder`, run a topology, or configure state directories, threads, and exception handlers.

```kotlin
val builder = StreamsBuilder()
val source = builder.stream(
    "orders",
    consumedOf(Serdes.String(), orderSerde),
)

source
    .groupByKey(groupedOf(Serdes.String(), orderSerde))
    .count(materializedOf("order-counts"))
    .toStream()
    .to("order-counts-output", producedOf(Serdes.String(), Serdes.Long()))
```

## Helpers by task

| Task | Helper |
| --- | --- |
| Source consumption | `consumedOf` |
| Sink production | `producedOf` |
| Grouping | `groupedOf` |
| Stream/table joins | `joinedOf`, `streamJoinedOf`, `tableJoinedOf` |
| State stores | `materializedOf` |
| Repartition | `repartitionedOf` |
| Branch | `branchedOf` |
| Windowed keys | `windowedOf` |

Each function forwards arguments to Kafka Streams `with`, `as`, or constructors. It does not validate serde compatibility, topic creation, partition counts, or store retention.

## State stores and names

`materializedOf` accepts a store name, `StoreType`, or supplier. Store names affect changelog topics and interactive queries, so renaming one can require state migration. Compare topology descriptions and generated topics before deployment.

`StreamConfig.streamsConfigDef` exposes Kafka `StreamsConfig.configDef()`. It helps inspect the valid schema, but it does not populate application configuration or handle validation errors.

## Joins and repartition

Serdes and store suppliers passed to join helpers must match both key/value types. `repartitionedOf(numberOfPartitions)` only sets the parameter; it does not check source or downstream topic compatibility.

Changing keys before joins or aggregation can create repartition topics. Include topology, volume, partition count, and retention in the operational plan.

## Branch helpers

`branchedOf` assembles a name, function, or consumer into Kafka `Branched`. Topology predicates and order still define routing. Stable names help locate generated children and metrics; inspect topology changes before renaming them.

## Test boundary

`KStreamDslTest` verifies parameter-object construction without a broker. It does not prove topology output, state restoration, rebalance, exactly-once, or upgrade compatibility. Add `TopologyTestDriver` and broker integration tests for those contracts.

## Sources and tests

- [`StreamConfig.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/StreamConfig.kt)
- [`Consumed.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Consumed.kt)
- [`Materialized.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Materialized.kt)
- [`StreamJoined.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/StreamJoined.kt)
- [`Repartitioned.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Repartitioned.kt)
- [`KStreamDslTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)
