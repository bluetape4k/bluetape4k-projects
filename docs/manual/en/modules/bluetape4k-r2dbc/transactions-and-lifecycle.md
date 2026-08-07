---
title: Transactions and lifecycle
description: Connect suspend transactions, transaction-aware connections, schema initialization, and the 1.12.1 auto-configuration boundary.
manualId: bluetape4k-r2dbc
chapterId: transactions-and-lifecycle
---

# Transactions and lifecycle

## The smallest atomic boundary

`withTransactionSuspend` creates a `TransactionalOperator` for the `DatabaseClient.connectionFactory`. Successful completion commits; an exception rolls back.

```kotlin
val result = client.databaseClient.withTransactionSuspend {
    client.databaseClient
        .sql("INSERT INTO accounts (owner, balance) VALUES (:owner, :balance)")
        .bind("owner", "kim")
        .bind("balance", 1000)
        .fetch()
        .awaitRowsUpdated()

    client.databaseClient
        .sql("INSERT INTO audit_log (message) VALUES (:message)")
        .bind("message", "account created")
        .fetch()
        .awaitRowsUpdated()

    "created"
}
```

Place the boundary around the smallest service operation whose writes must succeed together. The block's `ReactiveTransaction` can mark rollback-only state when required.

## Transaction manager cache and direct connections

Version 1.12.1 caches one manager per `ConnectionFactory` in a locked `WeakHashMap`. This avoids repeated manager construction but does not own the pool lifecycle. `withTransactionSuspending` is deprecated; use `withTransactionSuspend`.

`getConnectionAndAwait` and `releaseConnectionAndAwait` recognize Spring transaction-bound connections. `fetchConnectionAndAwait` calls `ConnectionFactory.create()` directly; the caller must close the result and must not assume it is the current transaction connection. Prefer the same `DatabaseClient` inside a transaction unless low-level access is necessary.

## Schema and data initialization

`resourceDatabasePopulatorOf` wraps SQL resources, `compositeDatabasePopulatorOf` orders multiple populators, and `connectionFactoryInitializer` attaches them to a factory.

```kotlin
val initializer = connectionFactoryInitializer(connectionFactory) {
    setDatabasePopulator(
        compositeDatabasePopulatorOf(
            resourceDatabasePopulatorOf(ClassPathResource("schema.sql")),
            resourceDatabasePopulatorOf(ClassPathResource("data.sql")),
        )
    )
}
```

Do not let this initializer and a migration tool both own production schema state. Select one source of truth and test execution timing and repeat safety.

## 1.12.1 auto-configuration

`R2dbcClientAutoConfiguration` registers `R2dbcClient` from `DatabaseClient`, `R2dbcEntityTemplate`, and `MappingR2dbcConverter` when the `DatabaseClient` class is present. Version 1.12.1 has no `@ConditionalOnMissingBean(R2dbcClient::class)`. A user bean does not automatically cause back-off; explicitly exclude or reconcile the auto-configuration.

Preserve transaction exceptions and cancellation. Retry an entire transaction only when the operation is idempotent and the failure is known to be transient.

## Sources and tests

- [`TransactionSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt)
- [`ConnectionFactoryUtils.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt)
- [`R2dbcClientAutoConfiguration.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt)
- [`ConnectionFactoryInitializer.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt)
- [`TransactionSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/TransactionSupportTest.kt)
- [`ConnectionInitTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionInitTest.kt)

## Next chapter

Continue to the [R2DBC ecosystem path](./ecosystem-paths.md).
