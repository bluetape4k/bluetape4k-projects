---
title: Coroutine reads and cardinality
description: Understand the execution and failure contracts of multi-document Flow, optional single, and required single reads.
manualId: bluetape4k-spring-boot-mongodb
chapterId: coroutine-reads-cardinality
---

# Coroutine reads and cardinality

## Use Flow for multiple documents

`findAsFlow<T>(query)` and `findAllAsFlow<T>()` call `asFlow()` on Spring Data publishers. Calling the extension does not immediately load a list. Subscription begins when a consumer collects the Flow.

```kotlin
fun findAdults(city: String): Flow<User> {
    val query = queryOf(
        "age".criteria() gte 20,
        "city".criteria() eq city,
    ).sortAscBy("name")

    return mongoOperations.findAsFlow(query)
}
```

Zero documents produces an empty Flow. Collecting the same Flow again can create another subscription and query.

## Optional and required reads

| Expected result | API | Zero documents |
| --- | --- | --- |
| Absence is valid | `findOneOrNullSuspending<T>`, `findByIdOrNullSuspending<T>` | `null` |
| A document is required | `findOneSuspending<T>`, `findByIdSuspending<T>` | `NoSuchElementException` |

The two families use `awaitSingleOrNull()` and `awaitSingle()`, respectively. Neither turns a connection or mapping failure into `null`.

```kotlin
suspend fun requireUser(id: String): User =
    mongoOperations.findByIdSuspending(id)

suspend fun findUser(id: String): User? =
    mongoOperations.findByIdOrNullSuspending(id)
```

Attach domain meaning such as HTTP 404 outside the repository. Mapping every driver failure to 404 would hide an outage as missing data.

## Count and exists

`countSuspending<T>(query)` returns `Long`; `existsSuspending<T>(query)` returns `Boolean`. Use `exists` instead of reading a document list when only presence matters.

```kotlin
suspend fun emailExists(email: String): Boolean =
    mongoOperations.existsSuspending<User>(
        queryOf("email".criteria() eq email)
    )
```

No match returns `0L` or `false`, not a failure.

## Cancellation and flow control

Cancelling Flow collection is propagated to the reactive subscription. Do not catch it and emit normal data. For large results, add an explicit order and limit, and observe consumer throughput together with pool usage.

## Source and tests

- [`ReactiveMongoOperationsCoroutines.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`QueryExtensionsTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)

## Next chapter

[Writes and atomic operations](./writes-and-atomic-operations.md) separates write results and atomicity boundaries.
