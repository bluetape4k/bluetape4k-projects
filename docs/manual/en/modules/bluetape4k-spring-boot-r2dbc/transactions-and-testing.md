---
title: Transactions, failures, and testing
description: Explain external transaction and connection ownership, cardinality failures, and H2-based integration testing.
manualId: bluetape4k-spring-boot-r2dbc
chapterId: transactions-and-testing
---

# Transactions, failures, and testing

## Transactions are owned outside the extension

The module's extensions do not create a transaction manager. When the current coroutine context carries a Spring reactive transaction, the underlying Spring Data operation uses its bound connection. Otherwise, the publisher follows its normal lifecycle.

Wrap the full service operation in one reactive transaction when several writes must commit together. [`withTransactionSuspend` in `bluetape4k-r2dbc`](../bluetape4k-r2dbc/transactions-and-lifecycle.md) is one option, provided every operation stays on the same `ConnectionFactory` and `DatabaseClient` boundary.

```kotlin
suspend fun createPost(command: CreatePost): Post =
    databaseClient.withTransactionSuspend {
        val post = posts.insert(command.toPost())
        val postId = checkNotNull(post.id)
        comments.insert(command.toInitialComment(postId))
        post
    }
```

If the project standardizes on annotations or `TransactionalOperator`, follow that policy. The invariant is that an individual extension does not automatically start a transaction.

## Connection and pool ownership

The extensions execute publishers through `R2dbcEntityOperations`; they do not acquire or close connections directly. The application or Spring Boot owns:

- R2DBC URL and credentials
- Driver and `ConnectionFactory`
- Pool size, acquisition timeout, and validation
- Startup schema migration
- Pool shutdown

The 1.12.1 `R2dbcBlogApplication` creates an H2 `ConnectionFactory` and an initializer explicitly. It is a test application example, not module auto-configuration.

## Keep failures distinct from results

| Situation | Result |
| --- | --- |
| `selectOneSuspending` with zero rows | Underlying error |
| `selectOneOrNullSuspending` with zero rows | `null` |
| `selectOne*` with multiple rows | Cardinality error |
| `selectFirstOrNullSuspending` with zero rows | `null` |
| Update/delete with no match | `0L` |
| Count with no match | `0L` |
| Connection, SQL, or mapping failure | Propagated exception |
| Coroutine cancellation | Propagated cancellation |

`null`, `0L`, and an exception are separate contracts. Do not collapse them into one repository outcome.

## The 1.12.1 integration test setup

The test application uses `r2dbc:h2:mem:///test` and initializes `posts` and `comments` from `schema.sql`. An `ApplicationReadyEvent` listener inserts two posts and four comments.

`R2dbcEntityOperationsExtensionsTest` runs insert→update→select→delete→exists within one test. It verifies each affected count and the final state, which makes failure location easy to identify.

```kotlin
@Test
fun `insert update delete extensions`() = runTest {
    val saved = operations.insertSuspending(createPost())
    val query = Query.query(Criteria.where(Post::id.name).isEqual(saved.id))

    operations.updateSuspending<Post>(query, Update.update("title", "Updated")) shouldBeEqualTo 1L
    operations.selectOneSuspending<Post>(query).title shouldBeEqualTo "Updated"
    operations.deleteSuspending<Post>(query) shouldBeEqualTo 1L
    operations.existsSuspending<Post>(query).shouldBeFalse()
}
```

## Keep tests isolated

A shared H2 database is fast but can allow data collisions between tests. Use generated IDs or unique input and clean only rows created by the test, or apply transaction rollback. Assert before/after deltas rather than a fixed global seed count to reduce ordering dependencies.

H2 does not prove production-driver type conversion, locking, or isolation behavior. Add sequential integration tests with the target PostgreSQL or MySQL R2DBC driver when those contracts matter.

## Sources and tests

- [`R2dbcBlogApplication.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/R2dbcBlogApplication.kt)
- [`DatabaseInitializer.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/DatabaseInitializer.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)
- [`PostRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/PostRepositoryTest.kt)
- [`CommentRepositoryTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/CommentRepositoryTest.kt)
- [`PostControllerTest.kt`](../../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)

## Next chapter

Continue to the [R2DBC ecosystem path](./ecosystem-paths.md).
