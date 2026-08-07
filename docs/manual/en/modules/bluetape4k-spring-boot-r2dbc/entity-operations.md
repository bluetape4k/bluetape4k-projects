---
title: Start with entity operations
description: Understand the coroutine extensions on R2dbcEntityOperations and the responsibilities the module leaves to the application.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: entity-operations
---

# Start with entity operations

## A thin coroutine extension layer

The public functions in `bluetape4k-spring-boot-r2dbc` are Kotlin extensions on Spring Data R2DBC operation interfaces. They do not introduce a client or repository base class. Instead, they add read and write functions to an already configured `R2dbcEntityOperations`.

```kotlin
@Repository
class PostRepository(
    private val operations: R2dbcEntityOperations,
) {
    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending(id)

    fun findAll(): Flow<Post> =
        operations.selectAllSuspending()
}
```

Spring Data still owns entity metadata and row mapping. The extensions adapt Reactor results to coroutine types, so mapping annotations, naming strategies, converters, and transaction binding retain their Spring Data behavior.

## Receiver types

| Receiver | Operations added |
| --- | --- |
| `R2dbcEntityOperations` | ID lookup, conditional select, count, and exists |
| `ReactiveInsertOperation` | Entity insert |
| `ReactiveUpdateOperation` | Update with `Query` and `Update` |
| `ReactiveDeleteOperation` | Conditional or full delete |

Spring's `R2dbcEntityTemplate` normally implements these interfaces, allowing one bean to use all the extensions. The extensions do not own or close the receiver lifecycle.

## ID lookup helpers

The ID helpers build a `Criteria` with the default name `id` and delegate to the general select functions.

```kotlin
val post: Post = operations.findOneByIdSuspending(1L)
val missing: Post? = operations.findOneByIdOrNullSuspending(-1L)

val first: LegacyPost =
    operations.findFirstByIdSuspending(1L, LegacyPost::postId.name)
```

`idName` is the name used in the query. When the property and physical column differ, confirm the Spring Data mapping rule and pass the intended name explicitly. The 1.12.1 tests cover both the default `id` and `Post::id.name` forms.

## Exactly one versus first

`findOneById*` delegates to `selectOne*`, so duplicate rows are not accepted. `findFirstById*` consumes only the first result. A true unique ID usually benefits from the `one` contract because it exposes invalid data. Use `first` for a non-unique, explicitly sorted query that intentionally selects one leading result.

The nullable variants map only an absent row to `null`. They do not convert database, mapping, or connection failures to `null`.

## What the module does not own

- Driver and `ConnectionFactory` selection
- Connection pool creation and shutdown
- Schema migration and seed data
- Transaction manager and transaction boundaries
- Raw SQL and custom row mapping

Those responsibilities remain with Spring Boot, Spring Data R2DBC, or [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md). The artifact name is not evidence of auto-configuration.

## Sources and tests

- [`R2dbcEntityOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt)
- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`PostRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`PostRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/PostRepositoryTest.kt)

## Next chapter

Continue to [Flow and result cardinality](./flow-and-cardinality.md).
