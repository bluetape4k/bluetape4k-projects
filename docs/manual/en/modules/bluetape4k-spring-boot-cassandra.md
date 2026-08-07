---
manualId: bluetape4k-spring-boot-cassandra
title: "Spring Data Cassandra Coroutine Support"
description: "Use Spring Data Cassandra reactive and async APIs through Kotlin coroutines and Flow, with option, model, and schema helpers."
kind: library
group: spring
learningOrder: 910
---

# Spring Data Cassandra Coroutine Support

## What it provides {#problem}

`bluetape4k-spring-boot-cassandra` connects Spring Data Cassandra reactive and async APIs to Kotlin coroutines and `Flow`. It adds suspend and Flow extensions for `ReactiveSession`, `ReactiveCassandraOperations`, `AsyncCassandraOperations`, and low-level CQL operations. It also provides option DSLs, Flow-to-batch adapters, `Persistable` and auditing base models, and mapping-metadata-based schema helpers.

This module is not a Spring Boot starter or auto-configuration module. Spring Boot, Spring Data Cassandra, and the application still configure `CqlSession`, contact points, keyspace, authentication, driver profiles, templates, repositories, health indicators, and metric exporters. This module adapts already configured objects to Kotlin call sites.

## Decide before adopting it {#when-to-use}

- Use this module when the application relies on Spring Data entity mapping and templates or repositories. If it only uses the DataStax Java Driver directly, [`bluetape4k-cassandra`](./bluetape4k-cassandra.md) is a smaller boundary.
- The reactive path converts `Publisher` values to `Flow` or suspend functions. The async path awaits `CompletableFuture`. Avoid mixing both styles without a clear service boundary.
- Flow batch adapters call `toList()` before handing values to Spring Data batch operations. They do not fit infinite or very large streams.
- `SchemaGenerator` is a convenience tool, not a migration system. Production schema history and rollback need a separate process.
- `AbstractCassandraAuditable.isNew()` checks `createdAt`, not the identifier. Without active auditing, an entity with an ID can still appear new.

## Add the dependency {#coordinates}

Consumers manage only the `bluetape4k-dependencies` BOM version, rather than aligning Spring Data, the Cassandra driver, and bluetape4k artifacts individually.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-cassandra")
}
```

The artifact includes the Spring Data Cassandra starter as an implementation dependency, but connection and session policies remain application configuration. DataStax mapper runtime and Micrometer driver integration are `compileOnly`; applications using them must add their runtime capabilities.

## First example {#quick-start}

Inject the `ReactiveCassandraOperations` configured by Spring Boot. Return a `Flow` for streams and use a suspend function for one result:

```kotlin
class UserReader(
    private val operations: ReactiveCassandraOperations,
) {
    fun findAll(): Flow<User> =
        operations.selectAsFlow<User>("SELECT * FROM users")

    suspend fun findOrNull(id: UUID): User? =
        operations.selectOneOrNullByIdSuspending<User>(id)
}
```

`selectAsFlow` returns a cold flow. CQL execution and row-mapping failures occur during collection, not when the function returns. Choose an `OrNull` operation when absence is valid.

## API by task {#api-by-task}

| Task | API | Boundary to keep visible |
| --- | --- | --- |
| Stream reactive results | `selectAsFlow`, `queryForFlow`, `queryForRowsFlow` | Subscription and errors occur during `Flow.collect`. |
| Reactive one-row and writes | `selectOneSuspending`, `insertSuspending`, `updateSuspending`, `deleteSuspending` | Some `awaitSingle` adapters reject an empty publisher. |
| Call the driver session | `ReactiveSession.executeSuspending`, `prepareSuspending` | Does not create or close the session; preserves statement options. |
| Call async templates | `AsyncCassandraOperations.*Suspending` | Awaits `CompletableFuture` and preserves Spring Data failures. |
| Map low-level CQL | `AsyncCqlOperations.querySuspending`, `ReactiveCqlOperations.queryForFlow` | The caller owns row mappers, extractors, and bind-marker types. |
| Build options | `queryOptions`, `insertOptions`, `updateOptions`, `writeOptions`, `deleteOptions` | Preserves Spring Data builder validation and semantics. |
| Add Flow values to a batch | `insertFlow`, `updateFlow`, `deleteFlow` | Collects all input in memory; execution remains a later batch step. |
| Base entity models | `AbstractCassandraPersistable`, `AbstractCassandraAuditable` | Decide ID, auditing, `isNew`, and equality rules first. |
| Write criteria | `Criteria.eq` | Alias for Spring Data `Criteria.is(value)`. |
| Assist schema setup | `SchemaGenerator` | Reads current keyspace metadata before creating UDTs/tables or truncating. |

## Learning path {#concepts}

The chapters are grounded in the 1.12.1 release source and tests. They connect the API surface to session ownership, cold streams, empty results, memory use, and schema-change responsibility.

1. [Configuration and object ownership](./bluetape4k-spring-boot-cassandra/configuration-and-ownership.md) — distinguishes this library from auto-configuration and assigns `CqlSession`, template, and repository responsibilities.
2. [Reactive operations and coroutines](./bluetape4k-spring-boot-cassandra/reactive-coroutine-operations.md) — explains execution timing and empty results when adapting publishers to Flow and suspend functions.
3. [Async and low-level CQL operations](./bluetape4k-spring-boot-cassandra/async-and-cql-operations.md) — covers future awaiting, row mappers, extractors, and prepared-statement boundaries.
4. [WriteOptions and batches](./bluetape4k-spring-boot-cassandra/write-options-and-batches.md) — connects TTL, timestamp, LWT, and full Flow collection.
5. [Models, conversion, and schema](./bluetape4k-spring-boot-cassandra/models-schema-and-converters.md) — covers new-entity detection, auditing, converters, and UDT/table creation.
6. [Failures, testing, and ecosystem paths](./bluetape4k-spring-boot-cassandra/failures-testing-and-ecosystem.md) — explains cancellation, driver failures, Testcontainers checks, and where to continue.

For a first adoption, read chapters 1 and 2. If the application uses hand-written CQL, continue with 3 and 4. Read 5 and 6 when entity and schema policies are part of the design.

## Recommended patterns {#patterns}

Let application configuration own `CqlSession` and Spring Data templates, then choose one data-access style per service. Use reactive plus `Flow` for streaming reads. Keep the future+suspend path for code already built around Spring Data async templates. Make absence explicit with an `OrNull` operation.

Prefer prepared statements and typed mapping over string interpolation. Build TTL, timestamp, consistency, timeout, and LWT policies in option objects near the use case instead of scattering them through call sites.

## Integration boundaries {#integrations}

```text
Spring Boot / application configuration
       └── CqlSession + keyspace + driver config
                    ↓
       Spring Data Cassandra mapping layer
       ├── ReactiveSession / ReactiveCassandraOperations
       ├── AsyncCqlOperations / AsyncCassandraOperations
       ├── CassandraTemplate and repositories
       └── MappingContext / converter
                    ↓
       bluetape4k coroutine, Flow, option,
       model, and schema helper APIs
```

`bluetape4k-cassandra` provides driver-level statement, paging, and CQL helpers. This module builds on that foundation and Spring Data Cassandra. It does not define a new Spring Data repository interface.

## Configuration {#configuration}

The 1.12.1 release contains no `src/main/resources`, `AutoConfiguration.imports`, `@ConfigurationProperties`, or auto-configuration class in this module. There is therefore no module-specific property prefix or activation condition.

Manage contact points, local datacenter, keyspace, authentication, request timeouts, pooling, and driver metrics through Spring Boot Cassandra settings or an application-owned `AbstractCassandraConfiguration`. The tests also configure and share their own `CqlSession`. A session is a costly, thread-safe object; do not create one per request.

## Failure behavior {#failures}

Reactive adapters use `awaitSingle`, `awaitSingleOrNull`, and `asFlow`. A non-null one-row API fails on an empty publisher, while nullable variants return `null`. A Flow propagates driver and mapping errors during collection.

Async adapters use `CompletableFuture.await()` and preserve Spring Data and driver exceptions. They add no retry, timeout, fallback, or exception translation. Coroutine cancellation cancels the wait, but it does not prove that a query already submitted to Cassandra stopped on the server.

`SchemaGenerator` fails when required entity metadata is unavailable. Its `truncate` operation deletes all rows when the table exists, so it does not belong in a normal production request path.

## Operations {#operations}

The module registers no health indicator or observation bean. Configure Spring Boot Actuator and Cassandra driver metrics in the application. Observe session connectivity, request latency, timeouts, unavailable and overloaded errors, and pool utilization.

Collect Flow results at a rate the consumer can handle and avoid unnecessary `toList()` calls. The batch Flow adapters deliberately collect everything, so bound their input. Isolate schema creation and truncation to deployment or test stages and record their execution.

## Testing {#testing}

Separate lightweight adapter, model, and option checks from tests that start Cassandra:

```bash
# Fast mock and value-object checks
./gradlew :bluetape4k-spring-boot-cassandra:test \
  --tests '*UnitTest' --tests '*OptionsSupportTest' --tests '*AbstractCassandraModelTest'

# Full module verification with Cassandra Testcontainers
./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache
```

The full suite uses `CassandraServer.Launcher.cassandra4`. Its test configurations share a companion-object `CqlSession` because creating another session for every Spring context exhausts connections. Do not pile this container-backed suite on top of other heavyweight tests in parallel.

## Workshops and examples {#workshops}

No dedicated workshop is registered in the manual manifest. The tests provide a practical progression: start with `ReactiveSessionCoroutinesExamples`, continue with `ReactiveCassandraTemplateTest` and `AsyncCassandraTemplateTest` for CRUD, slices, and options, then read `AsyncOptimisticLockingTest` for version-based LWT failures.

For lower-level driver APIs and paging, see [`bluetape4k-cassandra`](./bluetape4k-cassandra.md). For common Spring coroutine and context helpers, continue with [`bluetape4k-spring-boot-core`](./bluetape4k-spring-boot-core.md).

## 1.12.1 scope {#limitations}

This manual describes the `bluetape4k-projects` 1.12.1 release source. Despite the artifact name, it provides no auto-configuration, property binding, health or observation integration, or repository implementation. DataStax mapper runtime and driver Micrometer integration are not automatically present at runtime either.

Flow batch adapters are not streaming batches; they hold all input in memory. `SchemaGenerator` does not diff an existing table or keep migration history. `AbstractCassandraAuditable` maps its last-modified user field with the source spelling `lastModified_by`; verify that it matches an existing schema naming contract.

## Source and tests {#sources}

- [Module README](../../../../spring-boot/cassandra/README.md)
- [Module build](../../../../spring-boot/cassandra/build.gradle.kts)
- [`ReactiveCassandraOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutines.kt)
- [`ReactiveSessionCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt)
- [`AsyncCassandraOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutines.kt)
- [`AsyncCqlOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutines.kt)
- [`OptionsSupport.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt)
- [`SchemaGenerator.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt)
- [`AbstractCassandraPersistable.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraPersistable.kt)
- [`AbstractReactiveCassandraTestConfiguration.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
- [`ReactiveCassandraOperationsCoroutinesUnitTest.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutinesUnitTest.kt)
- [`SchemaGeneratorTest.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGeneratorTest.kt)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Extension and Class Structure diagram

[![Core Extension and Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-01.svg)

_Release README: [`spring-boot/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.md)_

### Cassandra Data Access Layer diagram

[![Cassandra Data Access Layer diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-02.svg)

_Release README: [`spring-boot/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.md)_

### Coroutine Conversion Sequence diagram

[![Coroutine Conversion Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-sequence-01.svg)

_Release README: [`spring-boot/cassandra/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.md)_

<!-- release-readme-diagrams:end -->
