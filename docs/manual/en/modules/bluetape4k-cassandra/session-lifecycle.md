---
title: CqlSession lifecycle and cache boundaries
description: Understand ownership, cache identity, and the 1.12.1 bootstrap limitation for direct and provider-managed sessions.
manualId: bluetape4k-cassandra
chapterId: session-lifecycle
---

# CqlSession lifecycle and cache boundaries

## Problem

`CqlSession` owns connection pools and driver execution resources. Creating one for every request makes ownership clear but repeatedly pays connection setup costs. Reusing sessions by keyspace alone can instead route different endpoints or configuration sets through the same session. Decide whether the caller or the provider owns the session before choosing an API.

## Direct creation

When one operation or bounded component owns a session, create it with `cqlSessionOf` and close it with `use`. An explicit `InetSocketAddress` also keeps the target visible at the call site.

```kotlin
import io.bluetape4k.cassandra.cqlSessionOf
import java.net.InetSocketAddress

fun readReleaseVersion(): String? {
    val contactPoint = InetSocketAddress("127.0.0.1", 9042)

    return cqlSessionOf(
        contactPoint = contactPoint,
        localDatacenter = "datacenter1",
        keyspaceName = "system",
    ).use { session ->
        session.execute("SELECT release_version FROM system.local")
            .one()
            ?.getString("release_version")
    }
}
```

The function creates the session, so the function closes it. Do not continue consuming a returned `Row` or paging state after leaving `use`.

## Provider identity

For application-scoped reuse, put only bounded configuration dimensions in `CqlSessionIdentity`. `of` trims each context part, removes empty values, and sorts the remainder into a stable context string. The same identity reuses the same open session; different identities create different sessions even when their keyspace matches.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.CqlSessionIdentity
import io.bluetape4k.cassandra.CqlSessionProvider
import java.net.InetSocketAddress

fun tenantOrdersSession(
    username: String,
    password: String,
): CqlSession {
    val contactPoint = InetSocketAddress("cassandra-a.example.com", 9042)
    val localDatacenter = "dc-a"
    val clientId = "order-reader"
    val identity = CqlSessionIdentity.of(
        keyspace = "tenant_orders",
        contextParts = listOf(
            "contactPoint=${contactPoint.hostString}:${contactPoint.port}",
            "localDatacenter=$localDatacenter",
            "routingProfile=tenant-a-primary",
            "credentialVersion=orders-v3",
            "clientId=$clientId",
        ),
    )

    return CqlSessionProvider.getOrCreateSession(
        identity = identity,
        builderSupplier = {
            CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(localDatacenter)
                .withAuthCredentials(username, password)
        },
    ) {
        withApplicationName("order-reader")
    }
}
```

`builderSupplier` must return a fresh builder each time it is called. In 1.12.1, session creation logs `${identity.context}` at INFO. Never put passwords, tokens, raw usernames, tenant identifiers, or customer identifiers in the context. Use only log-approved opaque routing profile IDs or credential versions.

Do not include request IDs, correlation IDs, random UUIDs, or other unbounded per-request values either. The 1.12.1 cache has no size limit or idle eviction, so each distinct value can create another session that remains until it is closed or the process shuts down. The `clientId`, `routingProfile`, and `credentialVersion` above are bounded deployment configuration values.

## 1.12.1 bootstrap limitation

In 1.12.1, the provider creates two sessions in sequence so it can create a missing keyspace.

```kotlin
builderSupplier().build().use { adminSession ->
    CassandraAdmin.createKeyspace(adminSession, identity.keyspace)
}

builderSupplier()
    .withKeyspace(identity.keyspace)
    .apply(builder)
    .build()
```

`builderSupplier` therefore supplies the contact point, local datacenter, authentication, and TLS settings needed by both the admin and final builders. The trailing builder block applies only to the final session. In the example above, `withApplicationName("order-reader")` does not affect the bootstrap session.

This is the behavior before the bootstrap-builder fix merged after 1.12.1. If the application should not own keyspace DDL, or bootstrap requires separate settings, manage the keyspace during deployment and open a directly owned session with `cqlSessionOf`.

## Shutdown

Close directly created sessions with `use` or an explicit `close`. The provider registers final sessions in `ShutdownQueue`, so do not wrap a provider-managed session in `use` by default. The first caller would close a shared session and break reuse for other callers.

The default provider-session lifetime is the lifetime of the process or owning component. To close or retire one identity explicitly, first quiesce every user of that identity and wait for in-flight work to finish. The provider has no atomic retire or evict API. Closing the session does not immediately remove its cache entry; the next `getOrCreateSession` lookup must observe and discard the closed entry. `ShutdownQueue` handles process-wide cleanup at shutdown.

## Failure table

| Situation | 1.12.1 behavior | Response |
| --- | --- | --- |
| Blank keyspace | `CqlSessionIdentity` and the keyspace overload throw `IllegalArgumentException`. | Validate the keyspace at the input boundary. |
| Blank local datacenter | `newCqlSessionBuilder` throws `IllegalArgumentException`. | Reject an empty driver setting immediately after loading configuration. |
| Same keyspace, different connection context | Different explicit identities do not reuse the same session. | Put only log-safe, bounded configuration IDs in the context. |
| Per-request values are used in identity | With no cache size limit or idle eviction, the session count keeps growing. | Exclude request IDs and random UUIDs; use deployment configuration dimensions. |
| A closed session remains cached | The next provider lookup removes the `isClosed` entry and recreates the requested identity. | Quiesce every user first and plan around the lack of an atomic retire or evict API. |
| Bootstrap settings exist only in the builder block | The block does not configure the admin session, so connection or authentication can fail first. | Move shared settings into `builderSupplier` or manage the keyspace separately. |

## Sources and tests

- [`CqlSessionProvider.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt): identity construction, INFO context logging, open-session reuse, closed-cache eviction, bootstrap and final session creation, and `ShutdownQueue` registration
- [`CqlSessionSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt): `cqlSession` and `cqlSessionOf`
- [`CassandraAdmin.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt): `CREATE KEYSPACE IF NOT EXISTS`
- [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt): cleanup of registered resources at process shutdown
- [`CqlSessionProviderTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt): same-identity reuse, context separation, and rejection of blank keyspace and local datacenter
- [`CqlSessionSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt): creation and closing of directly owned sessions

## Next reading

After choosing session ownership, continue with [Coroutine queries](./coroutine-queries.md) for suspend execution and paging boundaries.
