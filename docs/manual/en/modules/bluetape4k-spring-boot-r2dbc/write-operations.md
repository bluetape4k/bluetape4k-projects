---
title: Insert, update, and delete
description: Interpret inserted entities and affected row counts, with explicit failure and transaction boundaries.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: write-operations
---

# Insert, update, and delete

## Insert returns the stored entity

`insertSuspending(entity)` runs Spring Data's `insert<T>().using(entity)` and returns the stored entity. Generated IDs can be read from the result when mapping supports them.

```kotlin
val saved = operations.insertSuspending(
    Post(title = "R2DBC", content = "Coroutine extensions")
)
checkNotNull(saved.id)
```

`insertOrNullSuspending` returns `null` only if the publisher completes without a value. It does not convert constraint or mapping failures to `null`.

## It is not save or upsert

The module has no `save` operation that chooses insert or update from entity state. Passing an existing ID to `insertSuspending` does not imply an update. Express updates with `Query` and `Update`.

```kotlin
val query = Query.query(Criteria.where(Post::id.name).isEqual(postId))
val changes = Update.update(Post::title.name, "Updated")

val updated: Long =
    operations.updateSuspending<Post>(query, changes)
```

The result is the affected row count. For an ID query, `updated == 0L` means the target was absent or no longer matched. Validate the count when exactly one row was expected.

## Check delete results

Use `deleteSuspending<T>(query)` for conditional deletion and `deleteAllSuspending<T>()` for all rows.

```kotlin
val deleted = operations.deleteSuspending<Post>(query)
if (deleted != 1L) {
    throw PostNotFoundException(postId)
}
```

`deleteAllSuspending` delegates with `Query.empty()`. Reserve it for explicit cases such as fixture cleanup, and add safeguards before using it in production code.

## Group writes in a transaction

Each extension delegates one write to the supplied Spring Data operation; it does not start a transaction. If a post and its first comment must commit together, place both calls in one service-level reactive transaction.

Follow the application's transaction policy. Creating separate transactions around each repository function cannot provide atomicity for a larger business operation.

## Errors and cancellation

- Duplicate-key and foreign-key violations propagate from the driver and Spring Data.
- Entity-to-row conversion failures propagate as mapping errors.
- Connection acquisition failure and timeout are not hidden.
- Coroutine cancellation is not converted to success or `0L`.
- A normal zero-row update or delete remains distinct from an exception.

Retry outside the transaction only after the complete operation is known to be idempotent.

## The 1.12.1 verification flow

`R2dbcEntityOperationsExtensionsTest` inserts a `Post`, checks its generated ID, updates the title by ID, reloads it, deletes it, and finally verifies `existsSuspending` is `false`. Connecting each affected count to the next state assertion makes it a useful minimal example.

## Sources and tests

- [`ReactiveInsertOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt)
- [`ReactiveUpdateOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt)
- [`ReactiveDeleteOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)

## Next chapter

Continue to [Queries and repositories](./queries-and-repositories.md).
