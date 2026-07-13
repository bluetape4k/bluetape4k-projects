---
title: Session and transaction lifecycle
description: Understand regular, tenant, and stateless scopes plus factory ownership in coroutine call paths.
manualId: bluetape4k-hibernate-reactive
chapterId: session-transaction-lifecycle
---

# Session and transaction lifecycle

## Owners of factories, sessions, and transactions

Keep the factory for the application lifetime and close it at shutdown. Hibernate Reactive, by contrast, aligns sessions and transactions opened by `withSessionSuspending` and `withTransactionSuspending` with block completion. Do not return or retain a session outside that block.

```kotlin
val result = sessionFactory.withTransactionSuspending { session, transaction ->
    check(!transaction.isMarkedForRollback)
    session.persist(entity).awaitSuspending()
    entity.id
}
```

Commit on successful completion and rollback on failure are Hibernate Reactive `withTransaction` semantics. The extensions do not add another transaction manager or retry policy.

## Available scopes

Regular Session and StatelessSession paths both provide these combinations:

- a session-only block;
- a session block with a tenant ID;
- a transaction block;
- a block receiving both session and transaction objects;
- a tenant transaction block receiving both objects.

The tenant ID passes directly to the upstream overload. Tenant resolution, connection selection, and schema separation remain responsibilities of the application's Hibernate Reactive multi-tenancy configuration.

## Vert.x dispatcher

Each wrapper runs its suspending block with `async(currentVertxDispatcher())` inside the Hibernate Reactive callback. This bridge preserves the Vert.x context; it does not permit arbitrary blocking calls.

```kotlin
sessionFactory.withSessionSuspending { session ->
    val book = session.findAs<Book>(id).awaitSuspending()
    // blockingJdbcCall()  // Do not call this here.
    book
}
```

If a blocking library is unavoidable, isolate it on a separate dispatcher and outside the reactive transaction with an explicit data boundary. Do not let JDBC and a reactive session obtain separate connections inside what is intended to be one transaction.

## Failure boundary

`MutinyExtrasTest` and `StageExtrasTest` verify that an `IllegalStateException` from session work and a `RuntimeException` from transaction work reach the caller. They do not assert rollback state independently in the database, so document rollback as upstream transaction semantics rather than a separate extension guarantee.

## Executable evidence

- [Mutiny lifecycle source](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Stage lifecycle source](../../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [`AbstractMutinyTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/AbstractMutinyTest.kt)
- [`AbstractStageTest.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/AbstractStageTest.kt)
- [`MutinySessionFactoryExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinySessionFactoryExamples.kt)
- [`StageSessionFactoryExamples.kt`](../../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageSessionFactoryExamples.kt)
