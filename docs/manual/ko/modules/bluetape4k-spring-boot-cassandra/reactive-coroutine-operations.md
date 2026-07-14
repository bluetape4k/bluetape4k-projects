---
title: "Reactive operations와 coroutine"
description: "Spring Data Cassandra Publisher를 Flow와 suspend 함수로 사용할 때의 실행 시점, 빈 결과와 cancellation을 설명합니다."
---

# Reactive operations와 coroutine

## Flow와 suspend를 나누는 기준

여러 row를 순차 처리할 때는 `Flow`, 결과가 하나뿐인 작업은 suspend 함수를 사용합니다. `selectAsFlow`는 Spring Data가 반환한 `Flux`를 `asFlow()`로 바꾸며 결과를 미리 모으지 않습니다.

```kotlin
fun activeUsers(operations: ReactiveCassandraOperations): Flow<User> =
    operations.selectAsFlow(
        Query.query(Criteria.where("active").eq(true))
    )
```

반환된 Flow는 cold stream입니다. 호출만 해서는 CQL이 실행되지 않습니다. `collect`, `toList`, `first` 같은 terminal operation이 subscription을 시작합니다. 같은 Flow를 두 번 수집하면 쿼리도 두 번 실행될 수 있습니다.

## 단건 결과의 두 계약

`selectOneSuspending`은 `awaitSingle()`을 사용하므로 반드시 한 결과를 기대합니다. 결과 부재가 정상이라면 `selectOneOrNullSuspending` 또는 `selectOneOrNullByIdSuspending`을 사용합니다.

```kotlin
suspend fun requiredUser(operations: ReactiveCassandraOperations, id: UUID): User =
    operations.selectOneSuspending(Query.query(Criteria.where("id").eq(id)))

suspend fun optionalUser(operations: ReactiveCassandraOperations, id: UUID): User? =
    operations.selectOneOrNullByIdSuspending<User>(id)
```

두 API를 try/catch로 뒤섞기보다 service 계약에 맞는 이름을 선택합니다. nullable 결과와 backend 실패는 전혀 다른 상태입니다.

## CRUD와 options

insert, update, delete 확장은 Spring Data operations에 위임하고 `Mono`를 기다립니다. options 오버로드는 `EntityWriteResult`나 `WriteResult`를 반환하므로 LWT 적용 여부와 execution 정보를 확인할 수 있습니다.

```kotlin
val result = operations.insertSuspending(
    user,
    insertOptions { withIfNotExists() },
)
check(result.wasApplied())
```

`updateSuspending(query, update)`, `deleteSuspending(query)`, `countSuspending`, `existsSuspending`, `sliceSuspending`도 같은 방식으로 reactive 결과를 suspend 값으로 바꿉니다. retry나 domain exception 변환은 추가하지 않습니다.

## ReactiveSession을 직접 사용할 때

`ReactiveSession.executeSuspending`은 문자열, 위치 인자, 이름 인자 map, `Statement` 오버로드를 제공합니다. 문자열과 인자를 받는 함수도 내부에서 `SimpleStatement.newInstance`를 만든 뒤 statement 오버로드로 위임합니다.

```kotlin
val prepared = session.prepareSuspending(
    "SELECT * FROM users WHERE id = ?"
)
val resultSet = session.executeSuspending(prepared.bind(userId))
```

반복 실행하는 쿼리는 prepared statement를 재사용합니다. statement에 설정한 fetch size, consistency와 execution profile은 그대로 유지됩니다.

## cancellation과 오류

Coroutine cancellation은 Reactor subscription을 취소합니다. 하지만 Cassandra에 이미 전달된 요청이 server에서 즉시 중단됐다는 뜻은 아닙니다. timeout과 idempotence, 재시도 정책은 driver 설정에서 따로 다룹니다.

row mapping이나 driver 오류는 `Flow` 수집 또는 suspend 호출에서 그대로 발생합니다. `runCatching`으로 넓게 감싸 cancellation까지 일반 실패로 바꾸지 않습니다.

## 선택 규칙

- 큰 결과는 `Flow`로 처리하고 불필요하게 `toList()`하지 않습니다.
- 단건 부재가 가능한 경로는 `OrNull` API로 표현합니다.
- low-level result metadata가 필요할 때만 `ReactiveResultSet`을 직접 반환합니다.
- prepared statement와 typed entity mapping 중 service에 필요한 가장 높은 수준을 선택합니다.

## 근거

- [`ReactiveCassandraOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutines.kt)
- [`ReactiveSelectOperationSupport.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSelectOperationSupport.kt)
- [`ReactiveSessionCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt)
- [`ReactiveCassandraOperationsCoroutinesUnitTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutinesUnitTest.kt)
