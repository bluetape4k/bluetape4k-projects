---
title: Using StatelessSession
description: Apply StatelessSession to reactive work that does not need a first-level cache while respecting its limits.
manualId: bluetape4k-hibernate-reactive
chapterId: stateless-sessions
---

# Using StatelessSession

## Difference from a regular Session

StatelessSession does not use a first-level cache or regular persistence-context state tracking. It fits bulk single-row lookups and explicit bulk work that does not need managed-entity dirty checking. Use a regular Session when a use case changes an object graph and relies on flush semantics.

```kotlin
val count = sessionFactory.withStatelessTransactionSuspending { session, tx ->
    check(!tx.isMarkedForRollback)
    session.createSelectionQueryAs<Long>("select count(a) from Author a")
        .singleResult.awaitSuspending()
}
```

## Available work

- ID-based `getAs<T>`;
- typed HQL/JPQL, named queries, and native queries;
- result-set mapping and EntityGraph metadata;
- session-only and transaction blocks;
- tenant-ID overloads.

Mutiny supports `LockModeType`, EntityGraph, and graph-name `getAs` overloads, while Stage provides only ID and Hibernate `LockMode` variants.

## Fetch and consistency

The tests use fetch joins, EntityGraphs, and explicit `fetch()` with stateless sessions. The examples explicitly note that `enableFetchProfile` is unavailable for StatelessSession. Another example runs multiple count queries sequentially in one stateless transaction and checks consistent results.

Choosing StatelessSession does not define batch size or backpressure automatically. Bound rows per transaction, pool occupancy time, and database timeouts for bulk work.

## Executable evidence

- [Mutiny stateless extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt)
- [Stage stateless extensions](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/StatelessSessionSupport.kt)
- [`MutinyStatelessSessionExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyStatelessSessionExamples.kt)
- [`StageStatelessSessionExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageStatelessSessionExamples.kt)
