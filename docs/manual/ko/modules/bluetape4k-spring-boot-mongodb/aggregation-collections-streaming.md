---
title: 집계, 컬렉션과 스트리밍
description: Aggregation, distinct, collection 관리와 capped collection의 tailable cursor를 Flow로 사용하는 방법을 설명합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: aggregation-collections-streaming
---

# 집계, 컬렉션과 스트리밍

## Aggregation 결과를 Flow로 받기

`aggregateAsFlow<I, O>(aggregation)`은 input·output type을 지정하고 Spring Data aggregation 결과를 `Flow<O>`로 바꿉니다. `TypedAggregation`을 받는 overload도 있습니다.

```kotlin
data class CityCount(val id: String, val count: Long)

val aggregation = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("age").gte(20)),
    Aggregation.group("city").count().`as`("count"),
)

val counts: Flow<CityCount> =
    mongoOperations.aggregateAsFlow<User, CityCount>(aggregation)
```

output property와 aggregation 결과 field가 mapping되는지 통합 테스트로 확인합니다. 큰 pipeline은 server memory, disk use와 index 적용 여부도 함께 살핍니다.

## Distinct

`findDistinctAsFlow<I, O>(query, field)`는 input document type과 distinct result type을 분리합니다.

```kotlin
val cities: Flow<String> =
    mongoOperations.findDistinctAsFlow<User, String>(Query(), "city")
```

문자열 field가 실제 schema와 result type에 맞지 않으면 runtime mapping 오류가 날 수 있습니다.

## Collection 관리

컬렉션 존재 확인, 생성과 삭제를 suspend 함수로 호출할 수 있습니다. `createCollectionSuspending(options)`는 capped collection이나 validation 같은 `CollectionOptions`를 전달합니다.

```kotlin
if (!mongoOperations.collectionExistsSuspending("events")) {
    mongoOperations.createCollectionSuspending<Event>(
        CollectionOptions.empty().capped().size(16L * 1024 * 1024)
    )
}
```

존재 확인 뒤 생성하는 두 호출은 하나의 원자 연산이 아닙니다. 여러 instance가 동시에 시작할 수 있다면 이미 존재한다는 server 오류를 안전하게 처리하거나 migration 단계에서 컬렉션을 만듭니다.

## Tailable cursor

`tailAsFlow<T>(query)`는 capped collection의 tailable cursor를 `Flow`로 노출합니다. 일반 collection에서는 기대한 대기형 stream으로 동작하지 않습니다.

```kotlin
mongoOperations.tailAsFlow<Event>(Query())
    .collect { event -> handle(event) }
```

이 Flow는 오래 유지될 수 있습니다. scope 취소, shutdown, connection 단절과 재구독 정책을 명시합니다. 재구독은 마지막 처리 지점을 자동 복원하지 않으므로 중복과 누락을 허용할 수 있는 event 처리에 적합한지 먼저 판단합니다.

## Source와 tests

- [`ReactiveMongoOperationsCoroutines.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)

## 다음 읽을 장

[테스트, 운영과 생태계](./testing-operations-ecosystem.md)에서 실제 MongoDB 검증과 운영 관찰 지점을 연결합니다.
