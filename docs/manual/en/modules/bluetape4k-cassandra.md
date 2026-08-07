---
manualId: bluetape4k-cassandra
title: "Cassandra Coroutine Client"
description: Use the Apache Cassandra Java Driver from Kotlin with explicit session ownership, coroutine queries, and typed value mapping.
kind: library
group: data
learningOrder: 620
---

# Cassandra Coroutine Client

<span id="what-this-library-owns"></span>

## Features {#problem}

`bluetape4k-cassandra` adds Kotlin session factories, session-reuse boundaries, coroutine queries, row mapping, and statement extensions to the Apache Cassandra Java Driver. It removes repetitive application code from short-lived sessions through asynchronous multi-page reads and conversion of driver values into Kotlin types.

The module does not operate the Cassandra cluster or its schema. The application still owns contact points, credentials, keyspaces, consistency, and session shutdown.

## Decisions before adopting it {#when-to-use}

- Decide whether each operation creates and closes its own session or the application reuses sessions.
- For reuse, use bounded configuration dimensions such as contact point, datacenter, routing profile, credential version, and client id rather than request-specific values.
- Choose blocking `execute` or coroutine-based `executeSuspending` to match the calling layer.
- Decide whether the application may create keyspaces or deployment manages them separately.

## Add the dependency {#coordinates}

Expose only the central BOM version instead of repeating versions for individual bluetape4k artifacts.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

## First query {#quick-start}

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

## API decision map {#api-by-task}

| Task | Start with | Ownership or caution |
| --- | --- | --- |
| Create a session for a short scope | `cqlSessionOf`, `cqlSession` | The caller closes it with `use` or `close`. |
| Reuse a session for one connection context | `CqlSessionProvider`, `CqlSessionIdentity` | The identity is the cache boundary; the provider registers shutdown. |
| Query or prepare from a coroutine | `executeSuspending`, `prepareSuspending` | Preserve caller cancellation and paging boundaries. |
| Map rows and driver values to Kotlin types | `RowSupport`, `GettableSupport`, `DataTypeSupport` | Check null and column-type contracts first. |
| Assemble statements and query builders | `StatementSupport`, `QueryBuilderSupport` | Keep consistency, timeout, and keyspace visible at the call site. |
| Manage keyspaces and integration tests | `CassandraAdmin`, `AbstractCassandraTest` | Separate production DDL authority from test-container lifecycle. |

## Learning path {#concepts}

The five chapters below do more than list API names. Each chapter starts with the problem, then connects runnable examples, API selection rules, failure and operational boundaries, and the supporting 1.12.1 source and tests. Read them in order when adopting the module, or jump directly to the chapter that matches a problem in an existing application.

1. **[CqlSession lifecycle and cache boundaries](./bluetape4k-cassandra/session-lifecycle.md)**
   Start with the smallest `use`-scoped session example, then move to shared-session reuse with `CqlSessionProvider` and `CqlSessionIdentity`. This chapter helps you decide session ownership, cache identity, and where 1.12.1 bootstrap settings belong.
2. **[Coroutine queries](./bluetape4k-cassandra/coroutine-queries.md)**
   Follow single-result and multi-page examples built with `executeSuspending`, `prepareSuspending`, and `AsyncResultSet.asFlow()`. See how cancellation, mapper failures, and next-page fetch failures reach the caller.
3. **[Rows and data mapping](./bluetape4k-cassandra/rows-data-mapping.md)**
   Map `Row`, collections, tuples, UDTs, and `CqlDuration` into Kotlin values and domain objects. Learn when a null may become a domain default and when absence must remain explicit.
4. **[Statements and query builder](./bluetape4k-cassandra/statements-query-builder.md)**
   Compare raw CQL, prepared and bound statements, and QueryBuilder for the same work. Choose where bind markers, consistency, timeout, page size, and keyspace should remain visible.
5. **[Operations and testing](./bluetape4k-cassandra/operations-testing.md)**
   Connect keyspace side effects, session shutdown, paging failures, and Testcontainers verification. Decide whether production DDL authority belongs to the application or deployment and diagnose representative failures.

## Recommended patterns {#patterns}

Close directly created sessions where they are created, and reuse shared sessions through a bounded `CqlSessionIdentity`. Keep query values behind bind markers, map each `Row` into a domain type at the read boundary, and treat multi-page results as partially consumable and cancellable.

## Integrations {#integrations}

The module builds on the Apache Cassandra Java Driver core, query builder, and mapper runtime, and connects asynchronous execution and paging to Kotlin Coroutines. An application that uses mapper-generated `EntityHelper` types must also configure the DataStax mapper annotation processor in its build.

## Configuration {#configuration}

The application owns contact points, `localDatacenter`, authentication, TLS, keyspace, and statement consistency and timeout. Provider identities should contain only a bounded set of log-approved connection or credential configuration IDs.

## Failure behavior {#failures}

Blank keyspaces and local datacenters are rejected at the input boundary. Query preparation and execution, row mapping, and next-page fetch failures propagate at their respective operation boundaries. For bootstrap authentication failures, inspect the 1.12.1 admin-session settings first.

## Operations {#operations}

Keyspace creation and deletion are real cluster side effects. Separate production privileges and replication policy from application startup, and observe session shutdown, query and paging failures, batch size, and timeout. See [Operational boundaries and Testcontainers verification](./bluetape4k-cassandra/operations-testing.md) for the full checklist.

## Testing {#testing}

Tests that require real Cassandra behavior use Testcontainers and a working Docker runtime. Do not run them in parallel with other heavy integration tests.

```bash
./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

## Workshops {#workshops}

There is no module-specific workshop yet. The examples and source/test links verified against 1.12.1 provide a sequential path through sessions, coroutine paging, mapping, QueryBuilder, and operations.

## 1.12.1 limitation {#limitations}

In 1.12.1, `CqlSessionProvider` builds its keyspace-bootstrap admin session with `builderSupplier().build()`. The trailing builder block applies only to the final keyspace-bound session. Put contact point, local datacenter, authentication, and TLS settings required by both sessions in `builderSupplier`. This differs from the behavior introduced by PR #986 after 1.12.1.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Extension Function API Overview diagram

[![Extension Function API Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-01.svg)

_Release README: [`data/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.md)_

### Core API Structure diagram

[![Core API Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-02.svg)

_Release README: [`data/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.md)_

### Asynchronous Query Execution Flow diagram

[![Asynchronous Query Execution Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-sequence-01.svg)

_Release README: [`data/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`CqlSessionProvider.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt)
- [`CqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt)
- [`AsyncCqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt)
- [`RowSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt)
- [`StatementSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt)
- [`CqlSessionProviderTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt)
- [`CqlSessionSupportTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt)
