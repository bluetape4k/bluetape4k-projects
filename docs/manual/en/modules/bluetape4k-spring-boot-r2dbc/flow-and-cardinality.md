---
title: Flow and result cardinality
description: Choose between multi-row, exactly-one, first-row, and nullable reads with the correct execution and failure contract.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: flow-and-cardinality
---

# Flow and result cardinality

## Return multiple rows as Flow

`selectSuspending<T>(query)` and `selectAllSuspending<T>()` return `Flow<T>`. Internally, they apply `flow()` to the Spring Data reactive select chain.

```kotlin
fun findComments(postId: Long): Flow<Comment> {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.selectSuspending(query)
}
```

The query runs when the `Flow` is collected. Collecting the same instance twice may execute the database operation twice. If results must be reused, collect once and pass the materialized value explicitly after considering transaction and memory limits.

## Cardinality table

| Expected result | API | Zero rows | Multiple rows |
| --- | --- | --- | --- |
| Exactly one | `selectOneSuspending<T>` | Error | Error |
| Zero or exactly one | `selectOneOrNullSuspending<T>` | `null` | Error |
| First of one or more | `selectFirstSuspending<T>` | Error | First row |
| Zero or first | `selectFirstOrNullSuspending<T>` | `null` | First row |
| Multiple rows | `selectSuspending<T>` | Empty `Flow` | All rows |

Making `one` nullable does not permit duplicates. Conversely, `first` does not verify uniqueness. Select the operation that matches the business invariant.

## Count and exists

Use `countAllSuspending<T>()` for the full count and `countSuspending<T>(query)` for a filtered count. When only existence matters, `existsSuspending<T>(query)` avoids loading a row collection.

```kotlin
suspend fun hasComments(postId: Long): Boolean {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.existsSuspending<Comment>(query)
}
```

No match returns `0L` for count and `false` for exists. Database and mapping failures remain exceptions.

## Keep Flow through WebFlux

The 1.12.1 `PostController` returns the repository `Flow<Post>` directly.

```kotlin
@GetMapping
fun findAll(): Flow<Post> = postRepository.findAll()

@GetMapping("/{id}")
suspend fun findOne(@PathVariable id: Long): Post =
    postRepository.findOneByIdOrNull(id)
        ?: throw PostNotFoundException(id)
```

Using `Flow` for multiple values and a suspending function for one value keeps the path non-blocking from controller to database. Do not insert `runBlocking` or Reactor `block()` in the middle.

## Sort before selecting first

When the first row has business meaning, add an explicit sort to the `Query`. Without sorting, the leading row may change with the database execution plan.

```kotlin
val latestQuery = Query.empty()
    .sort(Sort.by(Sort.Direction.DESC, "createdAt"))
    .limit(1)

val latest = operations.selectFirstOrNullSuspending<Post>(latestQuery)
```

## Sources and tests

- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`CommentRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`CommentRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/CommentRepositoryTest.kt)
- [`PostController.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/controller/PostController.kt)
- [`PostControllerTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)

## Next chapter

Continue to [Insert, update, and delete](./write-operations.md).
