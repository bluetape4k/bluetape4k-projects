---
title: Module boundary and client lifecycle
description: Separate MongoDB coroutine driver responsibilities from bluetape4k helpers and manage direct and provider-cached clients correctly.
manualId: bluetape4k-mongodb
chapterId: module-boundary-client-lifecycle
---

# Module boundary and client lifecycle

## Small helpers over the official driver

The 1.12.1 production surface contains seven files. They cover client construction, caching, and sessions; database and collection extensions; `Document` helpers; and aggregation stage builders. The MongoDB Kotlin Coroutine Driver still owns the network protocol, pools, retries, codec execution, and `Flow` implementation.

The build exposes the coroutine driver, Kotlin extensions, and BSON Kotlin as API dependencies. Adding this artifact therefore makes driver APIs available, but bluetape4k does not choose driver settings or provision a server.

## Direct clients

`mongoClient` applies a `MongoClientSettings.Builder` and creates a new client. `mongoClientOf` applies the connection string before the additional builder.

```kotlin
val client = mongoClientOf("mongodb://localhost:27017") {
    applicationName("report-worker")
}

try {
    // use client
} finally {
    client.close()
}
```

Neither function registers a cache entry. The caller owns the client and its connection pool and must close it with the application component.

## Provider-shared clients

`MongoClientProvider.getOrCreate(connectionString)` shares a client through a string-keyed `ConcurrentHashMap`. New clients are registered with `ShutdownQueue` and close during JVM shutdown.

```kotlin
val first = MongoClientProvider.getOrCreate(url)
val second = MongoClientProvider.getOrCreate(url)
check(first === second)
```

Closing a provider-returned client in one caller can break every caller sharing that instance. Release 1.12.1 has no public eviction or close-all operation, so treat the provider as an application-lifetime singleton.

## Two caches in 1.12.1

The release source has separate caches for URL strings and `MongoClientSettings`. This affects instance identity.

| Call | Cache key | Result |
| --- | --- | --- |
| `getOrCreate(url)` | Raw URL string | Equal strings share an instance |
| `getOrCreate(url) { ... }` | Raw URL string | Only the first builder for that URL creates the client |
| `getOrCreate(settings)` | `MongoClientSettings` | Equal settings share an instance |

When one URL needs different timeouts or application names, build complete settings and use the settings overload. Also note that URL and settings overloads use different caches and can create two clients for the same logical endpoint.

## Cache cardinality and credentials

Per-tenant URLs or credentials create a client and pool for every distinct key. Do not feed an unbounded tenant set into the 1.12.1 provider because it has no eviction. Use an application registry with explicit creation and shutdown for dynamic tenants.

The release provider also logs the URL when it creates a client. Review logging policy when credentials are embedded in connection strings.

## Using it with Spring

If Spring Boot manages a `MongoClient` bean, first decide whether a separate provider client is necessary. Avoid duplicate pools when Spring Data and low-level workloads use the same cluster and credentials.

Use [`bluetape4k-spring-boot-mongodb`](../bluetape4k-spring-boot-mongodb.md) when repositories and mapping are required. This module does not provide Spring lifecycle or auto-configuration.

## Sources and tests

- [`build.gradle.kts`](../../../../../data/mongodb/build.gradle.kts)
- [`MongoClientSupport.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt)
- [`MongoClientProvider.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
