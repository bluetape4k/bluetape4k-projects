---
manualId: bluetape4k-cassandra
title: "Module bluetape4k-cassandra"
description: Use the Apache Cassandra Java Driver from Kotlin with explicit session ownership, coroutine queries, and typed value mapping.
kind: library
group: data
---

# Module bluetape4k-cassandra

## What this library owns

`bluetape4k-cassandra` adds Kotlin session factories, coroutine queries, and row and statement extensions to the Apache Cassandra Java Driver. It does not operate the Cassandra cluster or its schema. The application still chooses contact points, credentials, keyspaces, and when sessions end.

## Decisions before adopting it

- Decide whether each operation creates and closes its own session or the application reuses sessions.
- For reuse, use bounded configuration dimensions such as contact point, datacenter, routing profile, credential version, and client id rather than request-specific values.
- Choose blocking `execute` or coroutine-based `executeSuspending` to match the calling layer.
- Decide whether the application may create keyspaces or deployment manages them separately.

## Add the dependency

Expose only the central BOM version instead of repeating versions for individual bluetape4k artifacts.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

## First query

The code that creates a direct session also closes it. Keeping the query inside `use` closes the session after either a successful return or an exception.

```kotlin
import io.bluetape4k.cassandra.cqlSessionOf
import java.net.InetSocketAddress

val contactPoint = InetSocketAddress("127.0.0.1", 9042)

val releaseVersion = cqlSessionOf(
    contactPoint = contactPoint,
    localDatacenter = "datacenter1",
    keyspaceName = "system",
).use { session ->
    session.execute("SELECT release_version FROM system.local")
        .one()
        ?.getString("release_version")
}
```

## API decision map

| Task | Start with | Ownership or caution |
| --- | --- | --- |
| Create a session for a short scope | `cqlSessionOf`, `cqlSession` | The caller closes it with `use` or `close`. |
| Reuse a session for one connection context | `CqlSessionProvider`, `CqlSessionIdentity` | The identity is the cache boundary; the provider registers shutdown. |
| Query or prepare from a coroutine | `executeSuspending`, `prepareSuspending` | Preserve caller cancellation and paging boundaries. |
| Map rows and driver values to Kotlin types | `RowSupport`, `GettableSupport`, `DataTypeSupport` | Check null and column-type contracts first. |
| Assemble statements and query builders | `StatementSupport`, `QueryBuilderSupport` | Keep consistency, timeout, and keyspace visible at the call site. |
| Manage keyspaces and integration tests | `CassandraAdmin`, `AbstractCassandraTest` | Separate production DDL authority from test-container lifecycle. |

## Learning path

1. [CqlSession lifecycle and cache boundaries](./bluetape4k-cassandra/session-lifecycle.md)
2. [Coroutine queries](./bluetape4k-cassandra/coroutine-queries.md)
3. [Rows and data mapping](./bluetape4k-cassandra/rows-data-mapping.md)
4. [Statements and query builder](./bluetape4k-cassandra/statements-query-builder.md)
5. [Operations and testing](./bluetape4k-cassandra/operations-testing.md)

## 1.11.0 limitation

In 1.11.0, `CqlSessionProvider` builds its keyspace-bootstrap admin session with `builderSupplier().build()`. The trailing builder block applies only to the final keyspace-bound session. Put contact point, local datacenter, authentication, and TLS settings required by both sessions in `builderSupplier`. This differs from the behavior introduced by PR #986 after 1.11.0.

## Sources and tests

- [`CqlSessionProvider.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt)
- [`CqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt)
- [`AsyncCqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt)
- [`RowSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt)
- [`StatementSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt)
- [`CqlSessionProviderTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt)
- [`CqlSessionSupportTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt)
