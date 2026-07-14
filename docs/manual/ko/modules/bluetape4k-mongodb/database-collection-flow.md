---
title: Database·Collection과 Flow
description: typed collection, 첫 조회, 존재 확인, upsert와 filter·sort·pagination Flow helper의 실행 경계를 설명합니다.
manualId: bluetape4k-mongodb
chapterId: database-collection-flow
---

# Database·Collection과 Flow

## 이름 목록을 즉시 모으기

`listDatabaseNamesAsList`와 `listCollectionNamesList`는 driver가 반환한 `Flow<String>`에 `toList()`를 호출합니다.

```kotlin
val databaseNames: List<String> = client.listDatabaseNamesAsList()
val collectionNames: List<String> = database.listCollectionNamesList()
```

관리 화면처럼 이름이 제한적인 경우에는 편리합니다. 결과가 많거나 첫 값부터 처리해야 한다면 native `listDatabaseNames()`와 `listCollectionNames()`를 직접 collect해서 eager allocation을 피합니다.

## typed collection과 codec

`getCollectionOf<T>(name)`은 `getCollection(name, T::class.java)`의 reified 축약형입니다.

```kotlin
val products = database.getCollectionOf<Product>("products")
```

이 호출만으로 `Product` codec이 생기지는 않습니다. `Document`는 기본 codec으로 처리할 수 있지만 application class는 POJO, Kotlin 또는 kotlinx.serialization codec을 driver registry에 등록해야 합니다. codec 누락은 첫 encode/decode 시점에 드러날 수 있습니다.

## 첫 결과 조회

`findFirst(filter)`는 `find(filter).limit(1).firstOrNull()`을 호출합니다. `findFirstOrNull`은 같은 함수의 별칭입니다.

```kotlin
val product = products.findFirst(Filters.eq("sku", "A-100"))
```

결과가 없으면 `null`입니다. 필터가 여러 문서와 일치하고 sort를 지정하지 않으면 어떤 문서가 첫 번째인지는 업무 계약으로 삼을 수 없습니다. 정렬이 필요한 조회는 native find builder에서 sort까지 명시합니다.

## 존재 확인의 비용

`exists(filter)`는 `countDocuments(filter) > 0`입니다. boolean만 반환하지만 “첫 문서를 찾으면 중단”하는 helper는 아닙니다.

```kotlin
if (products.exists(Filters.eq("sku", sku))) {
    // document exists at the time of this read
}
```

존재 확인 뒤 insert를 따로 실행하면 경쟁 상태가 생깁니다. uniqueness는 unique index로 보장하고 duplicate key를 처리합니다. 단순 존재 확인이 병목이면 explain과 index를 확인하고 native query 대안도 비교합니다.

## upsert의 원자적 경계

`upsert(filter, update)`는 `updateOne`에 `UpdateOptions().upsert(true)`를 전달합니다.

```kotlin
val result = products.upsert(
    Filters.eq("sku", sku),
    Updates.combine(
        Updates.set("stock", stock),
        Updates.setOnInsert("sku", sku),
    ),
)
```

한 `updateOne` command의 원자성은 MongoDB가 보장합니다. helper가 idempotency key, unique index 또는 optimistic locking을 추가하지는 않습니다. filter와 index가 업무 identity를 정확히 표현해야 합니다.

## filter·sort·pagination Flow

`findAsFlow`는 filter를 적용한 `FindFlow`에 optional skip, limit, sort를 조립하고 `Flow<T>`로 반환합니다.

```kotlin
val page = products.findAsFlow(
    filter = Filters.gt("stock", 0),
    sort = Sorts.ascending("sku"),
    skip = 20,
    limit = 20,
)

page.collect { product -> consume(product) }
```

helper가 결과를 미리 수집하지는 않습니다. cancellation, buffering, driver exception과 command 실행 시점은 반환된 native flow의 계약을 따릅니다. pagination에는 안정적인 sort를 사용하고 같은 sort 값이 가능한 경우 `_id` 같은 tie-breaker를 더합니다.

## skip pagination의 한계

큰 skip은 server가 앞 문서를 건너뛰는 비용을 키울 수 있습니다. 데이터가 계속 추가되는 collection에서는 페이지 사이에 중복이나 누락도 생길 수 있습니다. 높은 page 번호를 자주 조회한다면 마지막 sort key를 다음 filter에 전달하는 cursor 방식과 비교합니다.

## Source와 tests

- [`MongoDatabaseExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`MongoCollectionExtensions.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt)
- [`MongoDatabaseExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensionsTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`BasicCrudExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)
