---
title: "WriteOptions와 batch"
description: "TTL, timestamp, LWT options를 명시하고 Flow batch의 전체 수집과 실행 경계를 이해합니다."
---

# WriteOptions와 batch

## Options DSL은 builder의 얇은 포장이다

`queryOptions`, `insertOptions`, `updateOptions`, `writeOptions`, `deleteOptions`는 Spring Data builder를 Kotlin receiver lambda로 호출합니다. 검증 규칙이나 기본값을 새로 정의하지 않습니다.

```kotlin
val options = insertOptions {
    withIfNotExists()
    ttl(Duration.ofMinutes(5))
}
val result = operations.insertSuspending(user, options)
if (!result.wasApplied()) {
    // 이미 존재하는 row 처리
}
```

TTL, timestamp, timeout, consistency와 LWT는 서로 다른 장애·정합성 의미를 갖습니다. “성능 옵션”으로 한데 묶지 않고 use case 계약으로 다룹니다.

## Query Builder statement에 WriteOptions 적용

`addWriteOptions`는 DataStax Query Builder의 `Insert`, `UpdateStart`, `DeleteSelection`에 TTL이나 timestamp를 추가합니다.

- `Insert`: TTL과 timestamp를 적용합니다.
- `UpdateStart`: TTL과 timestamp를 적용합니다.
- 이미 다음 단계로 진행한 일반 `Update`: 옵션을 적용하지 않고 그대로 반환할 수 있습니다.
- `DeleteSelection`: timestamp만 적용합니다.

따라서 update statement는 `setColumn` 등으로 shape를 바꾸기 전에 options를 적용하는 것이 안전합니다. 반환된 새 statement를 반드시 사용합니다.

## TTL의 경계

`WriteOptions.isPositiveTtl` 구현은 `ttl != null && !ttl.isNegative`입니다. 이름과 달리 zero duration도 `true`가 됩니다. TTL의 실제 허용 범위와 zero 의미는 Spring Data와 Cassandra 계약을 확인하고, 애플리케이션 입력에서는 명시적으로 양수인지 검증합니다.

timestamp는 Cassandra write timestamp입니다. 애플리케이션 wall-clock을 무조건 넣으면 node 간 순서와 재처리에서 오래된 쓰기가 최신 값을 이길 수 있습니다. business version과 driver timestamp를 구분합니다.

## Flow batch의 실제 동작

`insertFlow`, `updateFlow`, `deleteFlow`는 `mono { entities.toList() }`를 Spring Data batch에 추가합니다. 이름에 Flow가 있지만 row를 하나씩 Cassandra에 streaming하지 않습니다.

```kotlin
val batch = reactiveOperations.batchOps()
    .insertFlow(users.take(batchSize))

val result = batch.execute().awaitSingle()
```

확장은 batch chain만 구성합니다. 실제 실행은 `execute()`를 호출할 때입니다. 입력 Flow가 실패하면 list 수집 단계에서 batch도 실패합니다.

## Batch 크기와 원자성

Cassandra batch는 일반적인 JDBC bulk insert가 아닙니다. 관련 partition에 대한 원자성이 필요한 작은 묶음에 사용합니다. 서로 다른 partition의 대량 데이터를 한 batch로 묶으면 coordinator 부하와 timeout 위험이 커집니다.

Flow 입력은 먼저 메모리에 모이므로 두 가지 한계를 동시에 지켜야 합니다.

1. 애플리케이션 heap이 감당할 collection 크기
2. Cassandra가 감당할 batch statement 크기와 partition 분포

큰 stream은 bounded chunk로 나누고 각 chunk를 순차 또는 제한된 동시성으로 실행합니다. 무한 Flow를 직접 넘기면 batch 실행 단계에 도달하지 않습니다.

## LWT와 실패 처리

`withIfNotExists`와 version 기반 optimistic locking은 Cassandra LWT를 사용합니다. transport 성공과 조건 적용 성공을 구분해 `wasApplied()`를 확인합니다. timeout 후 무작정 재시도하면 원래 요청의 적용 여부가 불확실할 수 있으므로 idempotency와 조회 확인 절차를 먼저 정합니다.

## 근거

- [`OptionsSupport.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt)
- [`ReactiveCassandraBatchOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraBatchOperationsCoroutines.kt)
- [`OptionsSupportTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupportTest.kt)
- [`AsyncOptimisticLockingTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/async/AsyncOptimisticLockingTest.kt)
