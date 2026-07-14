---
title: "Async와 저수준 CQL operations"
description: "CompletableFuture 기반 Spring Data API와 row mapper, extractor, prepared statement를 coroutine에서 사용합니다."
---

# Async와 저수준 CQL operations

## Async operations를 suspend로 기다리기

`AsyncCassandraOperationsCoroutines.kt`는 Spring Data의 `CompletableFuture` 결과에 `await()`를 적용합니다. select, slice, count, exists, insert, update, delete와 truncate를 suspend 함수로 호출할 수 있습니다.

```kotlin
suspend fun findPage(
    operations: AsyncCassandraOperations,
    statement: Statement<*>,
): Slice<User> = operations.sliceSuspending(statement)
```

Reactive API와 달리 이 경로는 `Flow` streaming이 아닙니다. list 조회는 완료된 collection을 반환하며 Spring Data async API의 paging·materialization 규칙을 그대로 따릅니다.

## nullable 반환값을 읽는 법

Async Spring Data API가 nullable future 결과를 반환하는 경우 확장도 `T?`, `Boolean?`, `Long?`을 유지합니다. `selectSuspending`은 null list를 `emptyList()`로, `sliceSuspending`은 null을 빈 `SliceImpl`로 바꿉니다.

이 정규화는 “row가 없음”을 표현할 뿐 backend 오류를 숨기지 않습니다. `selectOneOrNullSuspending`과 nullable count·exists를 호출하는 service는 null을 어떤 domain 상태로 볼지 결정해야 합니다.

## AsyncCqlOperations와 mapper

저수준 CQL 확장은 CQL 문자열 또는 `Statement`와 함께 result-set extractor나 row mapper를 받습니다.

```kotlin
val names: List<String> = asyncCql.querySuspending(
    "SELECT name FROM users WHERE team = ?",
    teamId,
) { row, _ -> row.getString("name")!! }
```

row mapper는 column 이름과 nullability를 정확히 알아야 합니다. full entity mapping이 필요하면 `AsyncCassandraOperations`가 더 안전합니다. result-set extractor는 paging과 execution metadata를 직접 다뤄야 할 때만 사용합니다.

## ReactiveCqlOperations의 선택지

`ReactiveCqlOperationsSupport`는 object, map, row, result set을 suspend 또는 `Flow`로 반환하는 여러 overload를 제공합니다. prepared statement creator와 binder를 분리할 수도 있습니다.

```kotlin
val rows: Flow<Row> = reactiveCql.queryForRowsFlow(statement)
val user: User? = reactiveCql.queryForObjectSuspending(cql, rowMapper, id)
```

이 API들은 SQL client 같은 새 abstraction을 만들지 않습니다. Spring Data의 prepared statement creator, binder, row mapper와 exception translation을 그대로 사용합니다.

## Prepared statement와 binding

반복되는 CQL은 `ReactiveSession.prepareSuspending`으로 준비하고 `BoundStatement`를 재사용 가능한 query shape로 유지합니다. 값은 bind marker로 전달합니다. 문자열 interpolation은 escaping 문제뿐 아니라 prepare cache 활용과 관찰 가능성도 떨어뜨립니다.

Consistency, page size, timeout, tracing과 idempotence는 statement나 execution profile에 둡니다. coroutine adapter는 이 값을 덮어쓰지 않습니다.

## 실패와 cancellation

`CompletableFuture.await()`는 coroutine cancellation과 연결되지만, 이미 보낸 Cassandra query의 server 실행 상태와 동일하지는 않습니다. write retry는 idempotence와 LWT 의미를 검토한 뒤 driver 정책으로 설정합니다.

mapper의 null 단언, 잘못된 column 타입, bind marker 불일치는 adapter 바깥의 데이터 계약 오류입니다. domain layer에서 안정된 예외가 필요하다면 repository/service 경계에서 변환합니다.

## 근거

- [`AsyncCassandraOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutines.kt)
- [`AsyncCqlOperationsCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutines.kt)
- [`ReactiveCqlOperationsSupport.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/ReactiveCqlOperationsSupport.kt)
- [`AsyncCqlOperationsCoroutinesUnitTest.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutinesUnitTest.kt)
