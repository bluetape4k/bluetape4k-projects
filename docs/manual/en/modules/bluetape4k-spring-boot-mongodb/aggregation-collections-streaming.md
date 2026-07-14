---
title: Aggregation, collections, and streaming
description: Use aggregation, distinct queries, collection management, and capped-collection tailable cursors as Flow.
manualId: bluetape4k-spring-boot-mongodb
chapterId: aggregation-collections-streaming
---

# Aggregation, collections, and streaming

## Return aggregation results as Flow

`aggregateAsFlow<I, O>(aggregation)` supplies input and output types and adapts Spring Data aggregation results to `Flow<O>`. Another overload accepts `TypedAggregation`.

```kotlin
data class CityCount(val id: String, val count: Long)

val aggregation = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("age").gte(20)),
    Aggregation.group("city").count().`as`("count"),
)

val counts: Flow<CityCount> =
    mongoOperations.aggregateAsFlow<User, CityCount>(aggregation)
```

Use an integration test to verify mapping between output properties and pipeline fields. For a large pipeline, also inspect server memory, disk use, and index coverage.

## Distinct

`findDistinctAsFlow<I, O>(query, field)` separates the input document type from the distinct result type.

```kotlin
val cities: Flow<String> =
    mongoOperations.findDistinctAsFlow<User, String>(Query(), "city")
```

A string field that does not match the schema or result type can fail during runtime mapping.

## Collection management

Suspending functions cover collection existence, creation, and removal. `createCollectionSuspending(options)` accepts Spring Data `CollectionOptions` for capped collections and validation.

```kotlin
if (!mongoOperations.collectionExistsSuspending("events")) {
    mongoOperations.createCollectionSuspending<Event>(
        CollectionOptions.empty().capped().size(16L * 1024 * 1024)
    )
}
```

The existence check and creation are not one atomic operation. When several instances can start together, handle the server's already-exists error safely or create the collection during migration.

## Tailable cursors

`tailAsFlow<T>(query)` exposes a tailable cursor on a capped collection as Flow. An ordinary collection does not provide the expected waiting stream.

```kotlin
mongoOperations.tailAsFlow<Event>(Query())
    .collect { event -> handle(event) }
```

This can be a long-lived Flow. Define scope cancellation, shutdown, connection-loss, and resubscription policy. Resubscription does not automatically restore the last processed position, so first decide whether the event handler can tolerate duplicates or gaps.

## Source and tests

- [`ReactiveMongoOperationsCoroutines.kt`](../../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)

## Next chapter

Connect real MongoDB verification with operational signals in [Testing, operations, and ecosystem](./testing-operations-ecosystem.md).
