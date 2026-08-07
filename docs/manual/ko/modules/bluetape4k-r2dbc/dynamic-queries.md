---
title: 동적 query
description: QueryBuilder로 선택 조건과 count query를 구성하면서 1.12.1의 검증 범위와 부수효과를 다룹니다.
manualId: bluetape4k-r2dbc
chapterId: dynamic-queries
---

# 동적 query

## SQL 조각을 구조화한다

`QueryBuilder`는 optional where 조건을 조합하고 group, having, order, limit, offset을 붙이는 문자열 builder입니다. table과 column을 Kotlin type으로 모델링하는 DSL은 아니므로 compile-time SQL 검증을 기대하면 안 됩니다.

```kotlin
val query = query {
    select("SELECT * FROM users")
    whereGroup("and") {
        where("active = :active")
        whereGroup("or") {
            where("name LIKE :prefix")
            where("description LIKE :prefix")
        }
    }
    parameter("active", true)
    parameter("prefix", "A%")
    orderBy("created_at DESC")
    limit(20)
    offset(0)
}

val users = client.execute<User>(query).flow().toList()
```

root `whereGroup`은 하나만 둘 수 있고 operator는 대소문자를 정규화한 `and` 또는 `or`만 허용합니다. blank where와 unsupported operator는 query 생성 전에 실패합니다. 빈 group은 SQL에 포함되지 않습니다.

## nullable parameter

`parameterNullable<T>`와 `parameterNull`은 null에 R2DBC type 정보를 붙입니다. `Query.parameters`를 `bindMap`으로 넘길 때 raw null이 남지 않게 합니다.

## count query

`queryWithCount`는 조회 query와 count query를 `Pair`로 반환합니다. 같은 조건 구성을 재사용할 수 있지만 block을 두 번 실행합니다.

```kotlin
val (items, count) = queryWithCount {
    select("SELECT * FROM users")
    selectCount("SELECT COUNT(*) FROM users")
    whereGroup { where("active = :active") }
    parameter("active", true)
}
```

block 안에서 counter 증가, 외부 collection 변경, 시간·random 값 생성 같은 부수효과를 수행하면 두 query의 조건이 달라질 수 있습니다. block은 같은 입력에서 같은 builder 상태만 구성하도록 유지합니다.

## Query의 lazy SQL

`Query`는 mutable `StringBuilder`를 보관하고 `sql`을 첫 접근 시 trim해 cache합니다. 첫 접근 전에는 buffer 변경이 반영되지만, 첫 접근 뒤 변경은 이미 cache된 SQL에 반영되지 않습니다. 만들어진 `Query`는 사실상 immutable value처럼 다루고 buffer를 공유하지 않습니다.

## 1.12.1의 limit·offset 계약

1.12.1은 `limit`과 `offset` 값을 그대로 SQL에 붙입니다. 배포 이후 브랜치에는 `limit > 0`, `offset >= 0` 검증이 추가됐지만 1.11 기능이 아닙니다.

```kotlin
require(pageSize > 0) { "pageSize must be positive" }
require(offset >= 0) { "offset must be zero or positive" }
```

외부 요청 값을 builder에 넘기기 전에 호출자가 범위를 검증합니다.

## Source와 tests

- [`QueryBuilder.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilder.kt)
- [`QueryBuilderSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupport.kt)
- [`Query.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Query.kt)
- [`Filter.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Filter.kt)
- [`QueryBuilderTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderTest.kt)
- [`QueryBuilderSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupportTest.kt)

## 다음 읽을 장

여러 statement를 묶어야 한다면 [Transaction과 lifecycle](./transactions-and-lifecycle.md)에서 commit·rollback 소유권을 확인합니다.
