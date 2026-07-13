---
title: Reading and mapping ResultSet
description: Handle SQL NULL, row cardinality, cursor movement, and the lifecycle of lazy sequences.
manualId: bluetape4k-jdbc
chapterId: resultset-mapping
---

# Reading and mapping ResultSet

## Map SQL NULL to Kotlin null

Primitive JDBC getters such as `getInt`, `getLong`, and `getBoolean` may return zero or `false` for SQL NULL. The caller must immediately check `wasNull()` to distinguish those values. Extensions such as `getIntOrNull` and `getLongOrNull` combine both calls and return a Kotlin nullable value.

```kotlin
data class AccountRow(
    val id: Long,
    val retryCount: Int?,
    val nickname: String?,
)

val accounts = dataSource.executeQuery(
    "SELECT id, retry_count, nickname FROM accounts",
) { rs ->
    rs.toList { row ->
        AccountRow(
            id = row.getLong("id"),
            retryCount = row.getIntOrNull("retry_count"),
            nickname = row.getString("nickname"),
        )
    }
}
```

Column indexes follow the JDBC convention and start at one. Label access respects SQL aliases and is useful when a projection keeps stable names.

## Make the row-count contract explicit

| Expected result | API | No rows | More than one row |
| --- | --- | --- | --- |
| Zero or one row | `mapFirst` | `null` | Returns only the first row |
| Exactly one row | `mapSingle` | `NoSuchElementException` | `IllegalStateException` |
| First of one or more rows | `mapFirstOrThrow` | `NoSuchElementException` | Returns only the first row |
| Multiple rows | `toList`, `toSet`, `toMap`, `groupBy` | Empty collection | Consumes all rows |

`mapFirst` does not verify that a query is unique. When uniqueness is part of the business contract, enforce it in the database and use `mapSingle` to check the expected cardinality.

## Functions consume the cursor

Most mapping functions call `ResultSet.next()` and advance from the current position to the end. Calling `count`, `any`, and then `toList` on the same result set makes each function begin where the previous one stopped. Use one conversion per cursor, then derive other forms from the resulting collection.

As their names state, `isEmptyByMovingCursor` and `isNotEmptyByMovingCursor` move the cursor by one row. For a non-empty result, the cursor is positioned on the first row and that row must be read immediately. Calling `toList` next would collect from the second row. The older `isEmpty` and `isNotEmpty` names are deprecated because they hid this side effect.

```kotlin
dataSource.executeQuery("SELECT id FROM accounts ORDER BY id") { rs ->
    if (rs.isNotEmptyByMovingCursor()) {
        val firstId = rs.getLong("id")
        val remainingIds = rs.toList { it.getLong("id") }
        listOf(firstId) + remainingIds
    } else {
        emptyList()
    }
}
```

`moveToPrevious()` can fail for a forward-only driver, in which case the helper returns `false`. Do not treat rewinding a cursor as a portable recovery strategy.

## Sequence lifecycle

`sequence` and `mapAsSequence` consume the `ResultSet` cursor lazily; they do not copy the data. Complete the terminal operation inside the mapper block.

```kotlin
val ids: List<Long> = dataSource.executeQuery(
    "SELECT id FROM accounts ORDER BY id",
) { rs ->
    rs.sequence { row -> row.getLong("id") }
        .take(100)
        .toList()
}
```

Returning the sequence from the mapper lets `executeQuery` close the statement and `ResultSet` first. Iterating later would read an already closed cursor.

## Token-based mapping

Projections that repeatedly read several types can use `extract` and `ResultSetGetColumnTokens`.

```kotlin
val rows = dataSource.executeQuery(
    "SELECT id, display_name FROM accounts",
) { rs ->
    rs.extract {
        AccountRow(
            id = long["id"]!!,
            retryCount = null,
            nickname = string["display_name"],
        )
    }
}
```

Use `!!` only when the SQL and schema guarantee a non-null value. Keep nullable columns nullable in the domain type, or define an explicit default or failure policy at the mapping boundary.

## Sources and tests

- [`ResultSetExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt)
- [`ResultSetMappingExtensions.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt)
- [`ResultSetGetColumnTokens.kt`](../../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetGetColumnTokens.kt)
- [`ResultSetExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensionsTest.kt)
- [`ResultSetMappingExtensionsTest.kt`](../../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensionsTest.kt)

## Next chapter

After defining how query results become values, continue with [Transactions and state restoration](./transactions.md) to group multiple statements into one success or failure boundary.
