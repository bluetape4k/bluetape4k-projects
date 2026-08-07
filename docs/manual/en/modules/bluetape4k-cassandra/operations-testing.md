---
title: Operational boundaries and Testcontainers verification
description: Diagnose keyspace side effects, session shutdown, query paging, and Cassandra integration tests.
manualId: bluetape4k-cassandra
chapterId: operations-testing
---

# Operational boundaries and Testcontainers verification

## CassandraAdmin changes cluster state

`CassandraAdmin` is not only a collection of read helpers. `createKeyspace` and `dropKeyspace` change schema, while `getReleaseVersion` reads `system.local`.

```kotlin
import io.bluetape4k.cassandra.CassandraAdmin

val created = CassandraAdmin.createKeyspace(
    session = adminSession,
    keyspace = "orders",
    replicationFactor = 1,
)

val version = CassandraAdmin.getReleaseVersion(adminSession)
```

`createKeyspace` uses `CREATE KEYSPACE IF NOT EXISTS` with `SimpleStrategy`; its default replication factor is 1. That default is convenient for local examples but is not a replacement for production topology and replication policy. In production, it is usually safer for deployment tooling to create the keyspace and for the application to consume it.

`dropKeyspace` executes `DROP KEYSPACE IF EXISTS` synchronously. It is useful for test cleanup, but a wrong keyspace removes all of its data. Do not pass user input directly, and do not grant the application schema privileges it does not need.

## Permissions and bootstrap configuration

In 1.12.1, `CqlSessionProvider` first opens an admin session so that it can create the target keyspace. The admin session comes from the builder returned by `builderSupplier`; the trailing builder block applies only to the final session. Put the contact point, `localDatacenter`, authentication, and TLS settings required by bootstrap in `builderSupplier`.

```kotlin
val session = CqlSessionProvider.getOrCreateSession(
    identity = identity,
    builderSupplier = {
        CqlSessionProvider.newCqlSessionBuilder(contactPoint, localDatacenter)
            .withAuthCredentials(username, password)
    },
) {
    withApplicationName("order-reader")
}
```

The admin account needs permission to create the keyspace. If the application account cannot receive that permission, or admin and workload connections require different settings, manage the keyspace during deployment and open a directly owned session. This is the behavior before the bootstrap-builder fix merged after 1.12.1.

## What to observe

Keep enough logs and metrics to distinguish the failure boundary without exposing data.

| Boundary | Observe |
| --- | --- |
| Keyspace administration | Operation, approved keyspace, replication settings, `wasApplied`, exception |
| Session creation | Safe target alias, local datacenter, opaque configuration IDs in the identity |
| Query execution | Query shape, consistency, timeout, success, failure, cancellation |
| Paging | Rows/pages consumed, mapper failure, next-page fetch failure, collection cancellation |
| Batch | Batch type, partition scope, statement count, payload, latency, timeout |
| Shutdown | Direct/provider ownership, close start/completion, in-flight work |

Version 1.12.1 logs `CqlSessionIdentity.context` at INFO when creating a session. Do not put passwords, tokens, raw user, tenant, or customer identifiers in the context. Request IDs and random UUIDs also create unbounded cache identities. Use only a bounded set of log-approved routing profile IDs or credential versions.

Logging bound query values can expose personal data or credentials. Prefer query shape and marker names for diagnosis, and apply the application's redaction policy to actual values.

## Troubleshooting boundaries

| Symptom | First boundary to check |
| --- | --- |
| Bootstrap authentication or connection failure | Confirm that `builderSupplier` contains the settings required by the 1.12.1 admin session |
| Wrong session reused for the same keyspace | Confirm that `CqlSessionIdentity` context includes the connection/tenant boundary |
| Flow returns only some rows | Check collection cancellation, mapper exception, and next-page fetch failure |
| Connections remain after shutdown | Separate shutdown ownership for direct and provider-owned sessions |
| Batch latency or timeout | Check partition scope, statement count, consistency, and timeout |

When only some rows were observed, do not assume that `asFlow` accumulates the full result atomically. Rows emitted before a mapper or collector failure may already have been processed. A next-page fetch failure surfaces only after the current page is exhausted.

Close directly created sessions with `use` or `close`. Provider-created sessions are shared resources registered in `ShutdownQueue`; do not wrap ordinary uses in `use`. Before explicitly closing one, stop new users of that identity and wait for in-flight work. The provider has no atomic retire or evict API.

## Run the Testcontainers integration tests

The module tests use Testcontainers where real Cassandra behavior matters.

```bash
./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

The command requires a working Docker runtime and permission to pull or run the Cassandra image. Run it sequentially with other heavy Testcontainers tests because they share containers, ports, CPU, and disk. Report daemon connection, image pull, and startup timeout failures separately from assertion failures, but do not count them as success.

`AbstractCassandraTest` opens a Cassandra 4 container session, closes it in `@AfterAll`, and uses `SAME_THREAD`. `CassandraAdminTest` covers create/drop/version and blank-keyspace rejection. `CqlSessionProviderTest` covers identity reuse and connection-context separation. `AsyncResultSetSupportTest` inserts 6,000 rows and verifies that plain and mapped rows are collected across pages.

Start with the closest test when reproducing an incident, then use the module command above for final verification. Passing only mock-based tests does not prove authentication, schema permissions, paging, or container lifecycle.

## Sources and representative tests

- [`CassandraAdmin.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt): keyspace creation, deletion, and release-version lookup
- [`CqlSessionProvider.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt): 1.12.1 bootstrap, identity cache, and shutdown registration
- [`AbstractCassandraTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/AbstractCassandraTest.kt): Cassandra 4 Testcontainers fixture and session shutdown
- [`CassandraAdminTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CassandraAdminTest.kt): schema side effects and version verification
- [`CqlSessionProviderTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt): identity reuse and connection-context separation
- [`AsyncResultSetSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt): 6,000-row multi-page Flow integration tests

## Continue reading

- Previous: [Choose statements and QueryBuilder APIs](./statements-query-builder.md)
- Overview: [bluetape4k-cassandra manual](../bluetape4k-cassandra.md)
