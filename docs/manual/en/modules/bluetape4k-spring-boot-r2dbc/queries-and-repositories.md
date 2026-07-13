---
title: Queries and repositories
description: Compose Spring Data Query and Criteria objects into coroutine repositories and WebFlux endpoints.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: queries-and-repositories
---

# Queries and repositories

## Use Spring Data Query directly

The module does not introduce another query DSL. Express filters, sorting, and limits with Spring Data Relational `Criteria` and `Query`.

```kotlin
fun findAllByPostId(postId: Long): Flow<Comment> {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.selectSuspending(query)
}

suspend fun countByPostId(postId: Long): Long {
    val query = Query.query(
        Criteria.where(Comment::postId.name).isEqual(postId)
    )
    return operations.countSuspending(query)
}
```

Reusing the same condition for select and count is useful for pagination and endpoint metadata. Extract complex conditions into small private functions without unnecessarily exposing Spring Data query types beyond the repository.

## Compose conditions

```kotlin
private fun publishedBy(authorId: Long, keyword: String?): Query {
    var criteria = Criteria.where(Post::authorId.name).isEqual(authorId)
        .and(Post::published.name).isEqual(true)

    if (!keyword.isNullOrBlank()) {
        criteria = criteria.and(Post::title.name).like("%$keyword%")
    }

    return Query.query(criteria)
        .sort(Sort.by(Sort.Direction.DESC, Post::createdAt.name))
        .limit(50)
}
```

Do not use user input directly as a property name or unrestricted wildcard pattern. Let Spring Data bind values, and select sortable properties from an allowlist.

## Repository responsibility

A repository needs to decide three things:

1. The entity type and `Query` to use
2. Whether cardinality is represented by `Flow`, one, or first
3. How an affected row count maps to the domain result

HTTP status, retry policy, and transactions spanning multiple repositories belong to a wider service or controller-advice boundary. Converting every database exception to `null` inside the repository makes absence indistinguishable from failure.

## Connect a WebFlux endpoint

```kotlin
@RestController
@RequestMapping("/posts")
class PostController(
    private val posts: PostRepository,
) {
    @GetMapping
    fun findAll(): Flow<Post> = posts.findAll()

    @GetMapping("/{id}")
    suspend fun findOne(@PathVariable id: Long): Post =
        posts.findOneByIdOrNull(id) ?: throw PostNotFoundException(id)

    @PostMapping
    suspend fun insert(@RequestBody post: Post): Post =
        posts.insert(post)
}
```

WebFlux handles `Flow` and suspending functions directly. Avoid blocking JSON conversion or `runBlocking` after the database call.

## When raw SQL is required

Use [`bluetape4k-r2dbc`](../bluetape4k-r2dbc.md) and its `R2dbcClient`/`DatabaseClient` helpers for complex joins, vendor SQL, detailed generated-key handling, or direct DTO projections. Do not force raw SQL into this module's entity extension boundary.

Compare the R2DBC modules in `bluetape4k-exposed` when you want Kotlin table and column DSLs plus repository patterns. The [ecosystem path](./ecosystem-paths.md) explains the trade-off.

## Sources and tests

- [`ReactiveSelectOperationExtensions.kt`](../../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`PostRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`CommentRepository.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`PostController.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/controller/PostController.kt)

## Next chapter

Continue to [Transactions, failures, and testing](./transactions-and-testing.md).
