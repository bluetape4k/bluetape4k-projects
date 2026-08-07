---
title: Dynamic queries
description: Compose predicates and count queries with QueryBuilder while respecting the 1.12.1 validation and side-effect boundaries.
manualId: bluetape4k-r2dbc
chapterId: dynamic-queries
---

# Dynamic queries

## Structure SQL fragments

`QueryBuilder` combines optional where predicates and appends group, having, order, limit, and offset clauses. It is a string builder, not a type-safe table and column DSL.

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

Only one root `whereGroup` is allowed. Operators normalize to `and` or `or`; blank predicates and unsupported operators fail before SQL execution. Empty groups are omitted.

## Typed nullable parameters and count queries

`parameterNullable<T>` and `parameterNull` preserve type information. `queryWithCount` returns the data query and count query as a pair, but it executes the builder block twice.

```kotlin
val (items, count) = queryWithCount {
    select("SELECT * FROM users")
    selectCount("SELECT COUNT(*) FROM users")
    whereGroup { where("active = :active") }
    parameter("active", true)
}
```

Keep the block free of counter increments, external mutations, time-dependent values, and random generation so both queries receive the same conditions.

## Query lazy SQL

`Query` stores a mutable `StringBuilder` and trims and caches `sql` on first access. Buffer changes before first access are visible; changes afterward are not. Treat a built `Query` as an immutable value and do not share its buffer.

## The 1.12.1 limit and offset contract

Version 1.12.1 appends limit and offset values without validating their range. Positive-limit and non-negative-offset checks were added after the release and are not 1.11 behavior.

```kotlin
require(pageSize > 0) { "pageSize must be positive" }
require(offset >= 0) { "offset must be zero or positive" }
```

Validate external request values before passing them to the builder.

## Sources and tests

- [`QueryBuilder.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilder.kt)
- [`QueryBuilderSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupport.kt)
- [`Query.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Query.kt)
- [`Filter.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/Filter.kt)
- [`QueryBuilderTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderTest.kt)
- [`QueryBuilderSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupportTest.kt)

## Next chapter

Continue to [Transactions and lifecycle](./transactions-and-lifecycle.md).
