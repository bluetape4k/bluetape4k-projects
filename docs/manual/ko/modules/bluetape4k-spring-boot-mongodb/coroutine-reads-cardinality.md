---
title: Coroutine 조회와 cardinality
description: 여러 문서 Flow, nullable 단건, 필수 단건 조회의 실행 시점과 실패 계약을 설명합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: coroutine-reads-cardinality
---

# Coroutine 조회와 cardinality

## 여러 문서는 Flow

`findAsFlow<T>(query)`와 `findAllAsFlow<T>()`는 Spring Data publisher에 `asFlow()`를 적용합니다. 함수 호출만으로 query 결과가 list에 적재되지는 않습니다. consumer가 수집할 때 subscription이 시작됩니다.

```kotlin
fun findAdults(city: String): Flow<User> {
    val query = queryOf(
        "age".criteria() gte 20,
        "city".criteria() eq city,
    ).sortAscBy("name")

    return mongoOperations.findAsFlow(query)
}
```

0건은 빈 Flow입니다. 같은 Flow를 다시 수집하면 새 subscription과 query가 생길 수 있습니다.

## Nullable 조회와 필수 조회

| 기대 결과 | API | 0건 |
| --- | --- | --- |
| 문서가 없어도 정상 | `findOneOrNullSuspending<T>`, `findByIdOrNullSuspending<T>` | `null` |
| 반드시 한 문서가 있어야 함 | `findOneSuspending<T>`, `findByIdSuspending<T>` | `NoSuchElementException` |

두 계열은 각각 `awaitSingleOrNull()`과 `awaitSingle()`을 사용합니다. connection이나 mapping 실패를 `null`로 바꾸는 API는 아닙니다.

```kotlin
suspend fun requireUser(id: String): User =
    mongoOperations.findByIdSuspending(id)

suspend fun findUser(id: String): User? =
    mongoOperations.findByIdOrNullSuspending(id)
```

HTTP 404 같은 domain 의미는 repository 밖에서 `null`을 해석해 붙입니다. driver 장애까지 404로 바꾸면 운영에서 데이터 부재와 장애를 구분할 수 없습니다.

## Count와 exists

`countSuspending<T>(query)`는 `Long`, `existsSuspending<T>(query)`는 `Boolean`을 반환합니다. 존재 여부만 필요할 때 document를 읽어 list로 만들지 않습니다.

```kotlin
suspend fun emailExists(email: String): Boolean =
    mongoOperations.existsSuspending<User>(
        queryOf("email".criteria() eq email)
    )
```

0건은 `0L` 또는 `false`이며 실패가 아닙니다.

## Cancellation과 backpressure

`Flow` 수집 취소는 reactive subscription에 전달됩니다. 호출자가 취소한 작업을 `catch { emit(...) }`으로 정상 데이터처럼 바꾸지 않습니다. 결과가 매우 크면 limit와 정렬을 query에 명시하고 consumer 처리량, pool 점유 시간을 함께 관찰합니다.

## Source와 tests

- [`ReactiveMongoOperationsCoroutines.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`QueryExtensionsTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)

## 다음 읽을 장

[쓰기와 원자적 연산](./writes-and-atomic-operations.md)에서 write API별 결과와 원자성 범위를 구분합니다.
