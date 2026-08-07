---
title: SQL 실행과 parameter binding
description: DatabaseClient와 R2dbcClient로 SQL을 실행하고 named·indexed parameter와 typed null을 안전하게 바인딩합니다.
manualId: bluetape4k-r2dbc
chapterId: sql-and-binding
---

# SQL 실행과 parameter binding

## 두 실행 경로

`DatabaseClient.execute`는 Spring `GenericExecuteSpec`을 그대로 반환합니다. `R2dbcClient.execute<T>`는 보관 중인 `MappingR2dbcConverter`로 row를 `T`에 mapping합니다. SQL과 mapping을 직접 제어하려면 전자를, Spring Data mapping metadata를 재사용하려면 후자를 선택합니다.

```kotlin
val users = client
    .execute<User>("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow()
    .toList()
```

`awaitOne()`은 정확히 한 행을 기대하고, `awaitOneOrNull()`은 0~1행, `flow()`는 여러 행을 표현합니다. query의 결과 개수 계약을 먼저 정하고 terminal operation을 고릅니다.

## named parameter map

```kotlin
val spec = client.databaseClient
    .sql("SELECT * FROM users WHERE username = :username AND active = :active")
    .bindMap(
        mapOf(
            "username" to "jsmith",
            "active" to true,
        )
    )
```

`bindMap`은 map을 순회해 Spring R2DBC `Parameter`로 바인딩합니다. 값이 raw `null`이면 DB type을 알 수 없으므로 `IllegalArgumentException`을 던집니다.

## nullable 값은 타입을 보존한다

```kotlin
val nullableParameters = mapOf(
    "description" to typedNullParameter<String>()
)

client.databaseClient
    .sql("UPDATE users SET description = :description WHERE user_id = :id")
    .bindMap(nullableParameters)
    .bind("id", 1)
    .fetch()
    .awaitRowsUpdated()
```

단일 parameter라면 `bindNullable<String>("description", value)`를 사용할 수 있습니다. 핵심은 null에도 target type을 남기는 것입니다.

## indexed parameter

Spring R2DBC의 indexed binding은 0부터 시작합니다.

```kotlin
client.databaseClient
    .sql("SELECT name FROM users WHERE username = ? AND active = ?")
    .bindIndexedMap(mapOf(0 to "jsmith", 1 to true))
    .map<String> { row, _ -> row.get("name", String::class.java)!! }
    .awaitOne()
```

`bindIndexedMap`은 음수 index와 raw null을 거부합니다. placeholder 표기 방식은 driver마다 다를 수 있으므로 `?`, `$1` 같은 문법을 섞지 말고 실제 driver 통합 테스트로 확인합니다.

## README 예제보다 test를 우선한다

1.12.1 README에는 배포 소스에 없는 `awaitList`, `awaitSingleAsMap`, `awaitCount`, `awaitExists`가 등장합니다. 이 장은 해당 이름을 사용하지 않고 `ExecuteTest`에서 검증한 Spring R2DBC의 `awaitOne`, `awaitOneOrNull`, `flow`, `awaitRowsUpdated`만 사용합니다.

## 실패와 cancellation

driver와 Spring이 던진 exception은 호출자에게 전파됩니다. backend 예외를 무조건 retry하지 말고 transaction 상태, idempotency와 timeout budget을 함께 판단합니다. Flow 수집이 취소되면 cancellation을 fallback 값으로 바꾸지 않습니다.

## Source와 tests

- [`DatabaseClientSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt)
- [`ParameterSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/ParameterSupport.kt)
- [`Execute.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Execute.kt)
- [`ExecuteTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/ExecuteTest.kt)
- [`DatabaseClientSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupportTest.kt)

## 다음 읽을 장

실행과 binding을 익혔다면 [CRUD와 row mapping](./crud-and-mapping.md)에서 table DSL과 converter 경계를 확인합니다.
