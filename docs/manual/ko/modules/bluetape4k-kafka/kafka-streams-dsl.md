---
title: Kafka Streams DSL factory
description: Consumed, Produced, Materialized, join·repartition·branch parameter helper와 topology·state store 책임을 설명합니다.
manualId: bluetape4k-kafka
chapterId: kafka-streams-dsl
---

# Kafka Streams DSL factory

## DSL을 대체하지 않는 factory

`streams.kstream` package는 Kafka Streams의 parameter object를 Kotlin 함수로 만드는 계층입니다. `StreamsBuilder`, topology 실행, state directory, thread, exception handler를 만들지 않습니다.

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

## 작업별 helper

| 작업 | helper |
| --- | --- |
| source consume | `consumedOf` |
| sink produce | `producedOf` |
| grouping | `groupedOf` |
| stream/table join | `joinedOf`, `streamJoinedOf`, `tableJoinedOf` |
| state store | `materializedOf` |
| repartition | `repartitionedOf` |
| branch | `branchedOf` |
| windowed key | `windowedOf` |

각 함수는 Kafka Streams `with`, `as` 또는 constructor에 argument를 전달합니다. serde compatibility, topic creation, partition count나 store retention을 자동 검증하지 않습니다.

## state store와 naming

`materializedOf`는 store 이름, `StoreType` 또는 supplier를 받을 수 있습니다. store 이름은 changelog topic과 interactive query에 영향을 주므로 임의로 바꾸면 state migration이 될 수 있습니다. 배포 전 topology description과 생성 topic을 비교합니다.

`StreamConfig.streamsConfigDef`는 Kafka `StreamsConfig.configDef()`를 노출합니다. 유효한 config schema를 탐색하는 데 쓸 수 있지만 application config를 채우거나 검증 결과를 대신 처리하지 않습니다.

## join과 repartition

join helper에 전달하는 serde와 store supplier는 양쪽 key/value type과 일치해야 합니다. `repartitionedOf(numberOfPartitions)`는 partition 수를 parameter에 넣을 뿐 source와 downstream topic의 compatibility를 확인하지 않습니다.

key 변경 뒤 join 또는 aggregate를 수행하면 repartition topic이 생길 수 있습니다. topology, record volume, partition 수와 retention을 운영 계획에 포함합니다.

## branch helper

`branchedOf`는 name, function 또는 consumer를 Kafka `Branched`에 조립합니다. branch predicate와 순서는 topology에서 결정됩니다. 이름은 generated child name과 metric을 안정적으로 찾는 데 유용하므로 운영 중 바꾸기 전에 topology 차이를 검토합니다.

## 테스트의 범위

`KStreamDslTest`는 factory가 올바른 Kafka Streams object를 만드는지 server 없이 확인합니다. topology output, state restoration, rebalance, exactly-once와 upgrade compatibility는 보장하지 않습니다. 그런 계약은 `TopologyTestDriver`와 실제 broker 통합 test로 보완합니다.

## Source와 tests

- [`StreamConfig.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/StreamConfig.kt)
- [`Consumed.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Consumed.kt)
- [`Materialized.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Materialized.kt)
- [`StreamJoined.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/StreamJoined.kt)
- [`Repartitioned.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/streams/kstream/Repartitioned.kt)
- [`KStreamDslTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/streams/kstream/KStreamDslTest.kt)
