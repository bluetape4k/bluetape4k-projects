---
title: Connections and pools
description: Configure R2DBC connection options and pools while making shutdown and overload ownership explicit.
manualId: bluetape4k-r2dbc
chapterId: connections-and-pools
---

# Connections and pools

## The pool belongs to the application

`r2dbcConnectionPool` creates and returns a `ConnectionPool`; it does not install a shutdown hook. Attach a directly created pool to application startup and shutdown and always call `close()`. Close directly acquired connections as well so their slots return to the pool.

```kotlin
val pool = r2dbcConnectionPool(
    "r2dbc:postgresql://app:secret@db.example.com:5432/app"
) {
    maxSize = 32
    initialSize = 8
    minIdle = 8
    maxAcquireTime = Duration.ofSeconds(3)
    maxPendingAcquire = 128
    poolName = "app-r2dbc"
}

try {
    // application work
} finally {
    pool.close()
}
```

Keep real credentials in application secret management rather than a URL that may appear in logs.

## Connection-option DSL

`R2dbcConnectionConfig` transfers driver, protocol, host, port, database, user, password, SSL, timeouts, and driver-specific options into `ConnectionFactoryOptions`. A non-blank driver is required.

```kotlin
val pool = r2dbcConnectionPool {
    connection {
        driver = "postgresql"
        host = "db.example.com"
        port = 5432
        database = "app"
        user = "app"
        password = databasePassword
        ssl = true
    }
    pool {
        maxSize = 32
        initialSize = 8
        minIdle = 8
    }
}
```

SSL defaults to `false`. Production configuration must also verify the driver's certificate and hostname-validation options.

## Pool validation

- `maxSize` must be positive.
- `initialSize` and `minIdle` must be between zero and `maxSize`.
- `acquireRetry` must be zero or positive.
- `maxPendingAcquire` must be `-1` or zero or positive.
- JMX registration requires a non-blank `poolName`.
- `validationQuery`, when present, must not be blank.

Pool conversion calls `validate()` again, so invalid mutations after construction still fail before the pool is created.

## Do not hide overload

`maxPendingAcquire = -1` creates an unbounded queue. It can absorb a short burst, but memory and tail latency grow together while the database is slow. User-facing services should use a finite queue and acquire timeout and observe their failure rate.

`R2dbcPoolConfig.highThroughput(maxSize)` starts with warmed connections, a `maxSize * 4` pending queue, a three-second acquire timeout, and LOCAL validation. It is a starting profile, not automatic capacity planning. Derive pool size from the database connection budget and application instance count, then load-test it.

## Validation cost

Prefer `validationQuery = null` when driver-local validation is sufficient. A query such as `SELECT 1` adds a database round trip to acquisition. Use remote validation only when the operational benefit justifies that cost.

## Sources and tests

- [`R2dbcConnectionConfig.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionConfig.kt)
- [`R2dbcPoolConfig.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt)
- [`ConnectionPoolSupport.kt`](../../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt)
- [`R2dbcConnectionConfigTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/R2dbcConnectionConfigTest.kt)
- [`ConnectionPoolSupportTest.kt`](../../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupportTest.kt)

## Next chapter

Continue to [SQL execution and parameter binding](./sql-and-binding.md).
