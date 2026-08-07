---
manualId: bluetape4k-spring-boot-mongodb
title: "Spring Data MongoDB Coroutine Support"
description: "Use ReactiveMongoOperations with Flow and suspending functions, and compose Criteria, Query, and Update objects with Kotlin extensions."
kind: library
group: spring
learningOrder: 920
---

# Spring Data MongoDB Coroutine Support

## Provided capabilities {#problem}

`bluetape4k-spring-boot-mongodb` adapts Spring Data MongoDB's `ReactiveMongoOperations` to Kotlin coroutines. Operations that can produce multiple documents return `Flow<T>`; operations with one result become suspending functions. The module also provides concise Kotlin extensions for composing `Criteria`, `Query`, and `Update` objects.

It does not create a MongoDB client or connection pool. It uses the `ReactiveMongoDatabaseFactory` and `MongoConverter` configured by Spring Boot, while the MongoDB driver owns connections, the wire protocol, and server errors. Its auto-configuration is a small fallback that supplies `ReactiveMongoTemplate` only when no `ReactiveMongoOperations` bean exists.

## Decisions before adoption {#when-to-use}

- Decide whether Spring Data mapping and `ReactiveMongoOperations` are the application's persistence boundary.
- Distinguish an optional lookup, where zero documents means `null`, from a required lookup, where zero documents is an error.
- Choose where a multi-result or multi-insert `Flow` will be collected.
- Keep the different write semantics of `save`, `updateFirst`, `updateMulti`, and `upsert` explicit.
- Let Spring Boot and the driver own transactions, retryable writes, concerns, pool settings, and timeouts.

If the application wants the MongoDB Kotlin driver without Spring Data, start with [`bluetape4k-mongodb`](./bluetape4k-mongodb.md).

## Dependency {#coordinates}

Consumers manage only the `bluetape4k-dependencies` BOM version rather than aligning individual Spring and MongoDB driver versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb")
}
```

The artifact exposes the Spring Data MongoDB Reactive starter, coroutine/Reactor integration, and `bluetape4k-spring-boot-core`.

## First repository {#quick-start}

Inject the `ReactiveMongoOperations` supplied by Spring Boot and let the repository choose the query and result shape.

```kotlin
@Repository
class UserRepository(
    private val operations: ReactiveMongoOperations,
) {
    fun findAdults(city: String): Flow<User> =
        operations.findAsFlow(
            queryOf("age".criteria() gte 20, "city".criteria() eq city)
                .sortAscBy("name")
        )

    suspend fun findById(id: String): User? =
        operations.findByIdOrNullSuspending(id)

    suspend fun insert(user: User): User =
        operations.insertSuspending(user)
}
```

The `Flow` returned by `findAdults` subscribes to the Spring Data publisher when collected. Preserve that boundary through a WebFlux controller or coroutine service instead of inserting `block()` or `runBlocking`.

## API by task {#api-by-task}

| Task | API | Result and boundary |
| --- | --- | --- |
| Read multiple documents | `findAsFlow`, `findAllAsFlow` | `Flow<T>`; zero results is an empty Flow |
| Optional single lookup | `findOneOrNullSuspending`, `findByIdOrNullSuspending` | zero results is `null` |
| Required single lookup | `findOneSuspending`, `findByIdSuspending` | zero results throws `NoSuchElementException` |
| Count and existence | `countSuspending`, `existsSuspending` | `Long` and `Boolean` |
| Insert and save | `insertSuspending`, `insertAllAsFlow`, `saveSuspending` | saved entity or `Flow<T>` |
| Conditional update and upsert | `updateFirstSuspending`, `updateMultiSuspending`, `upsertSuspending` | `UpdateResult` |
| Atomic modify and remove | `findAndModifySuspending`, `findAndRemoveSuspending` | `null` when no document matches |
| Aggregate, distinct, and tail | `aggregateAsFlow`, `findDistinctAsFlow`, `tailAsFlow` | `Flow<T>` |
| Collection management | `collectionExistsSuspending`, `createCollectionSuspending`, `dropCollectionSuspending` | `Boolean` or `Unit` |

## Learning path {#concepts}

The chapters explain execution timing, result cardinality, and the Spring Boot/driver boundary before listing APIs. Each example links to its 1.12.1 source and tests.

1. [Auto-configuration and ownership boundaries](./bluetape4k-spring-boot-mongodb/auto-configuration-boundaries.md) — identify the fallback bean conditions and configuration owned by Spring Boot and the driver.
2. [Coroutine reads and cardinality](./bluetape4k-spring-boot-mongodb/coroutine-reads-cardinality.md) — distinguish `Flow`, optional single results, and required single results.
3. [Writes and atomic operations](./bluetape4k-spring-boot-mongodb/writes-and-atomic-operations.md) — interpret insert, save, update, upsert, and find-and-modify results.
4. [Criteria, Query, and Update DSL](./bluetape4k-spring-boot-mongodb/query-dsl.md) — compose filters, ordering, pagination, and updates with Kotlin syntax.
5. [Aggregation, collections, and streaming](./bluetape4k-spring-boot-mongodb/aggregation-collections-streaming.md) — use aggregation, distinct queries, capped collections, and tailable cursors.
6. [Testing, operations, and ecosystem](./bluetape4k-spring-boot-mongodb/testing-operations-ecosystem.md) — connect Testcontainers evidence, operational signals, and the native-driver path.

For first use, read chapters 1 through 4 in order. Start with chapter 5 for aggregation or tailable cursors, and chapter 6 for an adoption review or test plan.

## Recommended patterns {#patterns}

A repository should receive `ReactiveMongoOperations` and decide the query and cardinality. A service should own transactions and retries that span repository calls. Inspect `UpdateResult.matchedCount`, `modifiedCount`, and `DeleteResult.deletedCount` to distinguish no match from an actual change.

The string-based field DSL is concise, but the compiler cannot check it against the document schema. Map external sort and field inputs through an allowlist. `paginate` also performs no validation for a negative page or non-positive size, so validate HTTP input before building the query.

## Integration boundary {#integrations}

```text
WebFlux controller / coroutine service
                  ↓
bluetape4k coroutine operations + query DSL
                  ↓
ReactiveMongoOperations / mapping converter
                  ↓
ReactiveMongoDatabaseFactory / MongoDB driver
                  ↓
MongoDB server
```

The module does not replace these layers. The coroutine adapter maps Reactor publishers to `Flow`, `awaitSingle`, or `awaitSingleOrNull`; the DSL creates ordinary Spring Data query objects.

## Configuration and auto-configuration {#configuration}

`AutoConfiguration.imports` registers `ReactiveMongoAutoConfiguration`. It creates a `ReactiveMongoTemplate` only when `ReactiveMongoOperations` is on the classpath and no bean of that type exists. The required `ReactiveMongoDatabaseFactory` and `MongoConverter` must already be present in the Spring context.

Spring Boot and the MongoDB driver own the URI, credentials, database, SSL, timeouts, and pool. This module has no custom `@ConfigurationProperties`, client builder, conversion registry, or auditing switch. Put application-specific `MongoCustomConversions` and auditing configuration in a separate Spring configuration.

## Failure behavior {#failures}

The coroutine extensions do not translate Spring Data or driver failures into domain values. Nullable lookups turn only an absent result into `null`. Connection failures, duplicate keys, mapping errors, and timeouts remain exceptions. A required single lookup uses `awaitSingle()` and throws `NoSuchElementException` for an empty publisher.

Cancelling collection of a `Flow` cancels its subscription. Do not turn cancellation into an empty result or success. Before adding a retry, confirm that the whole operation is idempotent and that it does not duplicate driver-level retryable writes.

## Operations {#operations}

The module adds no metrics or health indicator. Use Spring Boot Actuator, MongoDB driver command monitoring, and pool metrics to observe server selection, pool waits, query latency, and timeouts. A slow `Flow` consumer can extend subscription and connection use, so observe result size and consumer throughput together.

A tailable cursor is a long-lived subscription. Verify that application shutdown and coroutine-scope cancellation reach the cursor, then place reconnect policy outside the adapter according to the tolerated duplicate and loss behavior.

## Testing {#testing}

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test --no-build-cache --no-configuration-cache
```

The 1.12.1 integration suite uses a `MongoDBServer` Testcontainer and a real Spring Boot context. `ReactiveMongoOperationsCoroutinesTest` covers insert, save, reads, updates, upsert, delete, aggregation, and collection management. Criteria, Query, and Update DSL tests compare generated BSON structures without a MongoDB connection.

Run the Docker-backed suite serially with other Testcontainers work. Keeping the DSL unit tests separate from integration tests helps determine whether a failure comes from query construction or from the server and driver boundary.

## Examples and next steps {#workshops}

No dedicated workshop is registered. `ReactiveMongoOperationsCoroutinesTest` is the broadest executable example in the module; read it with the `User` document to follow insert through aggregation.

Use [`bluetape4k-mongodb`](./bluetape4k-mongodb.md) when direct control of the MongoDB Kotlin driver API and codecs matters more than Spring Data mapping. If a Spring Data repository interface is required, choose Spring Data's reactive or coroutine repository support separately from these extensions.

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release source. The module does not generate Spring Data repository implementations and does not configure custom conversions, auditing, a transaction manager, a MongoDB client, or a pool. Although MongoDB Kotlin sync and coroutine driver dependencies are present, its public surface focuses on `ReactiveMongoOperations` extensions and the query DSL.

`tailAsFlow` requires a capped collection. `paginate` is offset based, can become expensive for large pages, and does not validate its arguments. Implement sorted seek pagination in the application query for large result sets.

## Source and tests {#sources}

- [Module README](../../../../spring-boot/mongodb/README.md)
- [Module build](../../../../spring-boot/mongodb/build.gradle.kts)
- [`ReactiveMongoAutoConfiguration.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt)
- [`ReactiveMongoOperationsCoroutines.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`CriteriaExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensions.kt)
- [`QueryExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensions.kt)
- [`UpdateExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`CriteriaExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensionsTest.kt)
- [`QueryExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)
- [`UpdateExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensionsTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.svg)

_Release README: [`spring-boot/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.md)_

### ReactiveMongoOperations Coroutine Extension Flow diagram

[![ReactiveMongoOperations Coroutine Extension Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.svg)

_Release README: [`spring-boot/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.md)_

### Criteria / Query / Update DSL Flow diagram

[![Criteria / Query / Update DSL Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.svg)

_Release README: [`spring-boot/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.md)_

### Coroutine Conversion Sequence diagram

[![Coroutine Conversion Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.svg)

_Release README: [`spring-boot/mongodb/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.md)_

<!-- release-readme-diagrams:end -->
