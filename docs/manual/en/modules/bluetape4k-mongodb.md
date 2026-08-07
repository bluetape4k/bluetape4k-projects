---
manualId: bluetape4k-mongodb
title: "MongoDB Coroutine Extensions"
description: "Use MongoDB Kotlin Coroutine Driver client, collection, BSON, and aggregation APIs with small helpers that remove repeated setup without hiding driver behavior."
kind: library
group: data
learningOrder: 630
---

# MongoDB Coroutine Extensions

## What it provides {#problem}

`bluetape4k-mongodb` adds small convenience APIs on top of the MongoDB Kotlin Coroutine Driver. It provides a `MongoClientSettings` DSL and client cache, database and collection helpers, `Document` builders, aggregation stage builders, and basic session and transaction lifecycle wrappers.

It does not replace the MongoDB driver. The official driver still owns connection pools, server selection, retryable reads and writes, BSON codecs, query execution, `suspend`, and `Flow`. If you need Spring beans, repositories, `MongoTemplate`, or a mapping context, compare this module with [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md).

## Decide before adoption {#when-to-use}

- Decide whether you want the official coroutine driver directly with less repeated client and collection code.
- Choose caller-owned clients from `mongoClientOf` or JVM-wide shared clients from `MongoClientProvider`.
- Choose raw `Document` values or application types backed by a configured codec registry.
- Distinguish eager `List` collection from keeping the driver's `Flow` through the processing pipeline.
- Confirm that the deployment topology supports sessions and transactions. A standalone MongoDB server cannot run multi-document transactions.
- Do not mix low-level driver helpers with Spring Data repository and mapping responsibilities without a clear ownership boundary.

## Coordinates {#coordinates}

Consumers manage only the central BOM version, not individual MongoDB driver versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mongodb")
}
```

Gradle project path: `:bluetape4k-mongodb`. Source directory: `data/mongodb`. The kotlinx.serialization BSON codec is `compileOnly`, so applications that use it must provide the runtime dependency.

## First client and collection {#quick-start}

This minimal setup makes the caller own the client lifecycle.

```kotlin
val client = mongoClientOf("mongodb://localhost:27017")

try {
    val database = client.getDatabase("catalog")
    val products = database.getCollectionOf<Document>("products")

    products.insertOne(documentOf("sku" to "A-100", "stock" to 3))
    val product = products.findFirst(Filters.eq("sku", "A-100"))
} finally {
    client.close()
}
```

`getCollectionOf<T>` only passes `T::class.java` to the driver. It does not add serialization support, so the database codec registry must be able to encode and decode `T`.

## API selection map {#api-by-task}

| Task | Start with | Boundary to remember |
| --- | --- | --- |
| Create a caller-owned client | `mongoClient`, `mongoClientOf` | The caller closes the returned client. |
| Share a client in the JVM | `MongoClientProvider.getOrCreate` | Release 1.12.1 has no explicit eviction or close-all API. |
| Collect database or collection names | `listDatabaseNamesAsList`, `listCollectionNamesList` | These collect every name into memory. |
| Obtain a typed collection | `getCollectionOf<T>` | Encoding and decoding remain codec-registry responsibilities. |
| Find one, check existence, or upsert | `findFirst`, `exists`, `upsert` | These compose native driver operations. |
| Query filter, sort, and page as `Flow` | `findAsFlow` | Execution and backpressure follow the coroutine driver. |
| Scope sessions and transactions | `withClientSession`, `inTransaction` | Pass the session to native collection operations explicitly. |
| Build BSON documents | `documentOf`, `Document.getAs<T>` | `getAs` is a safe cast, not numeric conversion or schema validation. |
| Build aggregation stages | `pipeline`, `matchStage`, and peers | Native `aggregate` executes the pipeline. |

## Learning path {#concepts}

These chapters follow the 1.12.1 release source and tests from client ownership through transactions, codecs, and operational validation. Each chapter separates official driver behavior from the helper added by this module.

1. [Module boundary and client lifecycle](./bluetape4k-mongodb/module-boundary-client-lifecycle.md) — compare direct clients with provider caching and inspect the 1.12.1 cache-key constraints.
2. [Database, Collection, and Flow](./bluetape4k-mongodb/database-collection-flow.md) — use typed collections, first-result lookup, existence checks, upsert, and lazy query results.
3. [Documents, codecs, and query boundaries](./bluetape4k-mongodb/documents-codecs-queries.md) — distinguish the `Document` DSL, safe casts, codec registry, and official query extensions.
4. [Sessions and transactions](./bluetape4k-mongodb/sessions-transactions.md) — follow session passing, commit, abort, close, and cancellation boundaries.
5. [Aggregation pipeline construction](./bluetape4k-mongodb/aggregation-pipelines.md) — separate stage construction from execution by native `aggregate`.
6. [Testing, operations, and ecosystem paths](./bluetape4k-mongodb/testing-operations-ecosystem.md) — combine unit and Testcontainers evidence and move toward Spring Data MongoDB when needed.

For a first adoption, follow chapters 1→2→3 and complete one client and one collection. If transactions are required, read chapter 4 first and prepare a replica-set or sharded-cluster test environment.

## Recommended patterns {#patterns}

Do not create a client for every operation. Share it for the lifetime of an application component. The component closes directly created clients; provider-returned clients are shared resources and should be identified as such at the call site. The 1.12.1 provider does not include builder settings in the string-cache key, so use the completed `MongoClientSettings` overload when the same URL needs distinct settings.

For potentially large results, keep the driver `Flow` instead of using eager helpers such as `listDatabaseNamesAsList`. Give `findAsFlow` a stable sort and explicit bounds. For repeated paging APIs, compare the cost of large skips with cursor-based pagination.

## Integrations {#integrations}

The module exposes the MongoDB Kotlin Coroutine Driver, Kotlin query extensions, and BSON Kotlin APIs. KProperty-based filters, sorts, updates, and projections come from the official `mongodb-driver-kotlin-extensions`; this module does not define another query DSL.

Use [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md) when the application needs reactive repositories, `ReactiveMongoOperations`, auto-configuration, or a Criteria DSL. When both modules are present, avoid creating both a low-level client and a Spring-managed client for the same workload unless ownership is intentional.

## Configuration {#configuration}

The module has no properties or resource files of its own. Configure endpoints, credentials, TLS, application names, timeouts, pools, and codec registries through the official `MongoClientSettings.Builder` API.

```kotlin
val client = mongoClientOf(connectionString) {
    applicationName("catalog-api")
    applyToConnectionPoolSettings { pool ->
        pool.maxSize(32)
    }
}
```

Do not log connection strings that contain credentials. Provider caches use either the string or `MongoClientSettings` as keys, so unbounded per-tenant credentials also create unbounded clients and pools.

## Failure behavior {#failures}

Helpers do not translate MongoDB exceptions or add retries. `findFirst` returns `null` when nothing matches. `Document.getAs<T>` returns `null` when a key is absent or its runtime type differs. Add explicit validation when those cases must be distinguished from corrupt data.

`inTransaction` commits on success and attempts an abort before rethrowing the original failure. An abort failure is attached as a suppressed exception. In 1.12.1, cancellation-path abort does not run in a `NonCancellable` context, so do not assume cleanup is guaranteed under aggressive cancellation.

## Operations {#operations}

Observe driver command latency, server-selection timeout, connection-pool wait, retries, and transaction aborts. Use bounded operation and collection labels; never put filter values or entire documents into high-cardinality metric labels.

When using the provider, monitor client creation and settings cardinality. Release 1.12.1 has no cache eviction API, so it is not a suitable dynamic registry for an unbounded number of tenants or credentials.

## Testing {#testing}

`DocumentExtensionsTest` and `AggregationSupportTest` run without MongoDB. The remaining module tests use a `MongoDBServer` Testcontainer and should run sequentially with other heavy suites.

```bash
./gradlew :bluetape4k-mongodb:test --no-configuration-cache
```

`AbstractMongoTest` is an internal fixture under `src/test`, not an API in the published artifact. Application tests should own their Testcontainers fixture and connect a coroutine `MongoClient`, not the sync client.

## Workshops {#workshops}

`BasicCrudExamples` combines native suspend CRUD with `findFirst`, `exists`, `upsert`, and `findAsFlow`. `AggregationExamples` builds stages with `pipeline` and executes them through native `aggregate(...).toList()`.

Continue with the [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md) manual for Spring Data mapping, Criteria, Query, Update, and reactive operations. Understanding the driver-level boundary first makes the framework's lifecycle and exception translation easier to evaluate.

## Release 1.12.1 scope {#limitations}

This manual targets release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`. Its production API consists of seven Kotlin source files covering client, database, collection, BSON, and aggregation helpers.

It does not provide repositories, an object-mapping framework, schema migration, auto-configuration, health indicators, metrics, or a transaction manager. Explicit provider-cache close APIs and `NonCancellable` transaction cleanup are also outside the 1.12.1 scope.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-01.svg)

_Release README: [`data/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.md)_

### Module API Structure diagram

[![Module API Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-02.svg)

_Release README: [`data/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.md)_

### Aggregation Pipeline Data Flow diagram

[![Aggregation Pipeline Data Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-03.svg)

_Release README: [`data/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [`build.gradle.kts`](../../../../data/mongodb/build.gradle.kts)
- [`MongoClientSupport.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt)
- [`MongoClientProvider.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt)
- [`MongoClientExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt)
- [`MongoCollectionExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt)
- [`MongoDatabaseExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`DocumentExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt)
- [`AggregationSupport.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt)
- [`MongoClientSupportTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`AggregationSupportTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
- [`BasicCrudExamples.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)
