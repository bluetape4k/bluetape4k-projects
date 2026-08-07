---
manualId: bluetape4k-spring-boot-r2dbc
title: "Spring Data R2DBC Coroutine Support"
description: "Use Spring Data R2DBC entity operations through Kotlin suspend functions and Flow."
kind: library
group: spring
learningOrder: 930
---

# Spring Data R2DBC Coroutine Support

## What it provides {#problem}

`bluetape4k-spring-boot-r2dbc` adapts Spring Data R2DBC's `R2dbcEntityOperations`, `ReactiveInsertOperation`, `ReactiveUpdateOperation`, and `ReactiveDeleteOperation` to Kotlin coroutine APIs. Multi-row reads return `Flow<T>`, single-row operations are suspending functions, and updates and deletes return their affected row count.

The module does not create a `ConnectionFactory` or connection pool, and it contains no auto-configuration. It is a thin extension layer over the `R2dbcEntityOperations` configured by Spring Boot and the application, adapting Reactor publishers with `awaitSingle`, `awaitSingleOrNull`, and `flow`.

## Decide before using it {#when-to-use}

- Decide whether Spring Data R2DBC entity mapping and `Query`/`Criteria` fit the data access boundary.
- Select the result contract: exactly one row, the first row, or multiple rows as a `Flow`.
- Keep insert and update distinct. This module has no state-aware `save` or upsert operation.
- Own transactions at the service or Spring configuration boundary. Calling one extension does not start a new transaction.
- Keep driver, `ConnectionFactory`, pool capacity, and shutdown ownership in the application.

For direct SQL or connection and pool helpers, start with [`bluetape4k-r2dbc`](./bluetape4k-r2dbc.md). If you need table DSLs and repository abstractions, compare Exposed R2DBC in the [ecosystem path](./bluetape4k-spring-boot-r2dbc/ecosystem-paths.md).

## Add the dependency {#coordinates}

Users manage only the `bluetape4k-dependencies` BOM version rather than aligning individual library versions. The application selects the database driver.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-r2dbc")

    runtimeOnly("org.postgresql:r2dbc-postgresql") // replace with your driver
}
```

The artifact exposes `bluetape4k-r2dbc`, `bluetape4k-spring-boot-core`, and the Spring Boot Data R2DBC starter as API dependencies.

## First repository {#quick-start}

Inject Spring Boot's `R2dbcEntityOperations` and use only the extensions needed by the repository.

```kotlin
@Repository
class PostRepository(
    private val operations: R2dbcEntityOperations,
) {
    fun findAll(): Flow<Post> =
        operations.selectAllSuspending()

    suspend fun findById(id: Long): Post? =
        operations.findOneByIdOrNullSuspending(id)

    suspend fun insert(post: Post): Post =
        operations.insertSuspending(post)
}
```

The query behind `selectAllSuspending` runs when the returned `Flow` is collected. A controller can return `Flow<Post>` directly, or a service can collect it with `toList()`. Introducing a blocking bridge removes the benefit of the R2DBC path.

## API by task {#api-by-task}

| Task | API | Result and boundary |
| --- | --- | --- |
| Exactly one row by ID | `findOneByIdSuspending<T>` | Missing or duplicate results propagate the underlying Spring Data error. |
| Nullable row by ID | `findOneByIdOrNullSuspending<T>` | Zero rows become `null`; duplicates still fail. |
| First row by ID | `findFirstByIdSuspending<T>`, `findFirstByIdOrNullSuspending<T>` | Multiple rows are allowed and only the first is used. |
| Multiple rows | `selectSuspending<T>`, `selectAllSuspending<T>` | Returns a cold `Flow<T>`. |
| One or first row | `selectOne*`, `selectFirst*` | `one` and `first` have different cardinality contracts. |
| Existence and count | `existsSuspending<T>`, `countSuspending<T>`, `countAllSuspending<T>` | Returns `Boolean` or `Long`. |
| Insert an entity | `insertSuspending<T>`, `insertOrNullSuspending<T>` | Returns the inserted entity; it does not update or upsert. |
| Conditional update | `updateSuspending<T>` | Returns the affected row count. |
| Conditional or full delete | `deleteSuspending<T>`, `deleteAllSuspending<T>` | Returns the deleted row count. |

## Learning path {#concepts}

The chapters go beyond an API inventory. They use the 1.12.1 source and tests to explain the cardinality, transaction, and failure boundaries encountered in a real repository. Each chapter links directly to the relevant implementation and verification code.

1. [Start with entity operations](./bluetape4k-spring-boot-r2dbc/entity-operations.md) — Understand receiver types, ID helpers, and the configuration this module does not own.
2. [Flow and result cardinality](./bluetape4k-spring-boot-r2dbc/flow-and-cardinality.md) — Distinguish multi-row, exactly-one, first-row, and nullable results.
3. [Insert, update, and delete](./bluetape4k-spring-boot-r2dbc/write-operations.md) — Interpret inserted entities and affected row counts without confusing insert with save.
4. [Queries and repositories](./bluetape4k-spring-boot-r2dbc/queries-and-repositories.md) — Build repositories and WebFlux endpoints with Spring Data `Query` and `Criteria`.
5. [Transactions, failures, and testing](./bluetape4k-spring-boot-r2dbc/transactions-and-testing.md) — Connect external transaction ownership, cancellation, H2 integration tests, and failure assertions.
6. [R2DBC ecosystem path](./bluetape4k-spring-boot-r2dbc/ecosystem-paths.md) — Choose the next step among core R2DBC, JDBC, Exposed R2DBC, and workshops.

New users should follow chapters 1 through 5. If the persistence technology or abstraction level is still undecided, start with chapter 6 and return to the relevant chapter.

## Recommended patterns {#patterns}

A repository should inject `R2dbcEntityOperations` and decide query shape and result cardinality. Put business transactions at a service boundary that can span several repositories. Keep a `Flow` intact until its endpoint or service consumer, and terminate a single write publisher as a suspending function.

Do not replace `one` with `first` based only on naming preference. Use `one` when duplicates must surface as an error; use `first` when a sorted result intentionally selects its leading row. Check update and delete counts to distinguish a stale condition from a successful write.

## Integration boundary {#integrations}

The module sits on this path:

```text
WebFlux controller / coroutine service
                  ↓
bluetape4k Spring Data R2DBC coroutine extensions
                  ↓
R2dbcEntityOperations / Spring Data Query and mapping
                  ↓
ConnectionFactory / R2DBC driver / database
```

`bluetape4k-r2dbc` supplies connection, raw SQL, mapping, and transaction helpers. This module adds coroutine-friendly Spring Data entity operations above that layer. Both can be used together, but they should not duplicate responsibility for the same query.

## Configuration {#configuration}

The module has no property class, auto-configuration class, or `src/main/resources` configuration. Spring Boot configures `spring.r2dbc.*`, the driver, `ConnectionFactory`, and pool. Applications that configure R2DBC manually must provide `R2dbcEntityOperations` as well.

The 1.12.1 test application creates an H2 `ConnectionFactory` through `AbstractR2dbcConfiguration` and applies a schema with `ConnectionFactoryInitializer`. That code is a test fixture, not an auto-configuration feature of this module.

## Failure behavior {#failures}

The extensions do not turn Spring Data and driver failures into fallback values. `selectOneSuspending` does not hide zero or duplicate rows. `selectOneOrNullSuspending` maps only zero rows to `null`; duplicate rows can still fail. `selectFirstOrNullSuspending` allows duplicates because it intentionally consumes only the first result.

Insert mapping failures, constraint violations, and connection acquisition errors propagate. Cancellation is not changed into success or an empty result. An update or delete count of `0L` is a normal “no matching row” result, so callers must compare it with the expected count.

## Operations {#operations}

This module provides no metrics or health indicator. Observe query latency, pending pool acquisitions, timeouts, transaction rollbacks, and WebFlux request latency through Spring Boot, the driver, and the pool. Recollecting a `Flow` may rerun its query, so traces should make repeated subscriptions visible.

A `Flow` does not automatically bound database or memory load. Add sorting and limits to the `Query`, then monitor consumer throughput and connection hold time.

## Testing {#testing}

The 1.12.1 tests use an H2 in-memory database and a real Spring Boot context to verify reads, inserts, updates, deletes, and WebFlux endpoints.

```bash
./gradlew :bluetape4k-spring-boot-r2dbc:test --no-build-cache --no-configuration-cache
```

`R2dbcEntityOperationsExtensionsTest` verifies insert→update→select→delete→exists in one flow. `PostRepositoryTest` and `CommentRepositoryTest` cover nullable one-row lookup, first-row lookup, `Flow`, and count at the repository boundary. `PostControllerTest` verifies that `Flow` and suspending values reach a WebFlux endpoint.

## Workshops and examples {#workshops}

The in-module `coroutines.blog` test application is the closest runnable example. Read `PostRepository`, `CommentRepository`, and `PostController` together to follow the entity→repository→WebFlux endpoint path.

Continue to the [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop) for a higher-level SQL DSL and repository exercises. To start from raw R2DBC, follow the [`bluetape4k-r2dbc` learning path](./bluetape4k-r2dbc/ecosystem-paths.md).

## 1.12.1 scope {#limitations}

This manual targets the `bluetape4k-projects` 1.12.1 release source. Despite `spring-boot` in the module name, it has no separate auto-configuration, conditions, property binding, or pool management. It does not generate `R2dbcRepository` implementations or extend Spring Data repository interfaces.

The API depends on reified types and Spring Data entity mapping. Use `bluetape4k-r2dbc` or Spring `DatabaseClient` for raw SQL, batching, detailed generated-key control, or custom row mapping.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Core Class Structure diagram

[![Core Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-01.svg)

_Release README: [`spring-boot/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.md)_

### R2DBC + Coroutines Data Flow diagram

[![R2DBC + Coroutines Data Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-02.svg)

_Release README: [`spring-boot/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.md)_

### CRUD Operation Hierarchy diagram

[![CRUD Operation Hierarchy diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-diagram-03.svg)

_Release README: [`spring-boot/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.md)_

### Coroutine Conversion Sequence diagram

[![Coroutine Conversion Sequence diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-r2dbc-sequence-01.svg)

_Release README: [`spring-boot/r2dbc/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/r2dbc/README.md)_

<!-- release-readme-diagrams:end -->

## Sources and tests {#sources}

- [Module README](../../../../spring-boot/r2dbc/README.md)
- [Module build](../../../../spring-boot/r2dbc/build.gradle.kts)
- [`R2dbcEntityOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/R2dbcEntityOperationExtensions.kt)
- [`ReactiveSelectOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveSelectOperationExtensions.kt)
- [`ReactiveInsertOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveInsertOperationExtensions.kt)
- [`ReactiveUpdateOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveUpdateOperationExtensions.kt)
- [`ReactiveDeleteOperationExtensions.kt`](../../../../spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines/ReactiveDeleteOperationExtensions.kt)
- [`R2dbcEntityOperationsExtensionsTest.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/domain/R2dbcEntityOperationsExtensionsTest.kt)
- [`PostRepository.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/PostRepository.kt)
- [`CommentRepository.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/domain/CommentRepository.kt)
- [`PostControllerTest.kt`](../../../../spring-boot/r2dbc/src/test/kotlin/io/bluetape4k/spring/r2dbc/coroutines/blog/test/controller/PostControllerTest.kt)
