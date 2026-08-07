---
title: 모듈 경계와 client·record helper
description: Kafka 3.x line의 범위, producer·consumer 생성과 수명주기, TopicPartition parsing과 metric helper의 계약을 설명합니다.
manualId: bluetape4k-kafka
chapterId: module-boundary-client-records
---

# 모듈 경계와 client·record helper

## Kafka 3.x를 위한 Kotlin 보조 계층

1.12.1의 `bluetape4k-kafka`는 Kafka 3.9.x와 Spring Kafka 3.x에 맞춘 모듈입니다. Kafka client, Reactor Kafka와 Spring Kafka API를 감싸되 broker, topic, consumer group이나 Spring bean을 만들지 않습니다. `src/main/resources`도 없습니다.

Kafka client API는 `api` dependency라 consumer가 직접 사용할 수 있습니다. Kafka Streams는 `compileOnly`이므로 Streams helper를 쓰는 애플리케이션이 runtime dependency를 제공해야 합니다.

## producer와 consumer 생성

`producerOf`와 `consumerOf`는 `Map<String, Any?>` 또는 `Properties`와 선택적인 serializer/deserializer를 `KafkaProducer`·`KafkaConsumer` constructor에 전달합니다.

```kotlin
val producer = producerOf<String, OrderEvent>(producerProps)
val consumer = consumerOf<String, OrderEvent>(consumerProps)

try {
    consumer.subscribe(listOf("orders"))
    // poll and process
} finally {
    consumer.close()
    producer.close()
}
```

factory는 cache, retry, default security나 shutdown hook을 추가하지 않습니다. 생성한 client와 network resource는 호출자가 소유합니다. Spring이 `ProducerFactory`, `KafkaTemplate`과 listener container를 관리한다면 native client를 별도로 만들 필요가 있는지 먼저 확인합니다.

## 설정과 serializer ownership

serializer instance를 argument로 넘기지 않으면 Kafka 설정의 class 이름으로 생성합니다. 둘을 섞을 때 어떤 instance가 실제로 사용되는지 Kafka client constructor 계약을 기준으로 판단합니다. `bootstrap.servers`, `acks`, idempotence, transaction id, deserializer, group id, auto commit과 security 설정은 그대로 caller 책임입니다.

credential이 들어간 전체 config를 log에 남기지 않습니다. client id와 제한된 topic·group 정보만 기록하고 SASL password, token과 truststore credential을 지웁니다.

## `TopicPartition` parsing

`topicPartitionOf("order-events-12")`는 마지막 `-`를 구분자로 사용합니다. 따라서 dash가 있는 topic도 처리합니다.

```kotlin
val partition = "order-events-12".toTopicPartition()
check(partition.topic() == "order-events")
check(partition.partition() == 12)
```

blank는 `IllegalArgumentException`, 구분자 없음은 `IllegalArgumentException`, 정수가 아닌 suffix는 `NumberFormatException`입니다. 음수 partition은 local validation 없이 만들어집니다. 외부 입력이라면 Kafka에 넘기기 전에 `partition >= 0`을 별도로 검사합니다.

## metric helper

`Producer.getMetricValueOrNull(name)`은 `metrics()`의 key name이 같은 첫 entry의 `metricValue()`를 반환합니다. metric이 없으면 `null`입니다. group이나 tag를 구분하지 않고 name만 비교하므로 같은 이름의 metric이 여럿인 경우 원하는 entry를 고른다는 보장은 없습니다.

운영 코드는 Kafka의 `MetricName` group과 tags까지 확인하거나, application metric으로 변환할 때 label cardinality를 제한합니다.

## Source와 tests

- [`build.gradle.kts`](../../../../../infra/kafka/build.gradle.kts)
- [`ProducerSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/ProducerSupport.kt)
- [`ConsumerSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/ConsumerSupport.kt)
- [`TopicPartitionSupport.kt`](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/TopicPartitionSupport.kt)
- [`TopicPartitionSupportTest.kt`](../../../../../infra/kafka/src/test/kotlin/io/bluetape4k/kafka/TopicPartitionSupportTest.kt)
