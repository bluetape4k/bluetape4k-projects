---
manualId: bluetape4k-hibernate-reactive
title: "Reactive Hibernate Extensions"
description: "Use Hibernate Reactive Mutiny and Stage APIs from Kotlin coroutines with reified types."
kind: library
group: data
learningOrder: 650
---

# Reactive Hibernate Extensions

## What it provides {#problem}

`bluetape4k-hibernate-reactive` makes Hibernate Reactive's Mutiny and Stage APIs more convenient from Kotlin. It unwraps a JPA `EntityManagerFactory` as a reactive `SessionFactory`, bridges session and transaction callbacks to suspending blocks, and replaces repeated Java `Class` arguments with reified types for queries and EntityGraphs.

It is not a separate ORM or transaction manager. Session creation and closing, commit and rollback, and query execution remain Hibernate Reactive responsibilities. Choose it when you need Vert.x SQL Client based non-blocking I/O while retaining Hibernate entity mapping.

## Decisions before adoption {#when-to-use}

- Choose Mutiny `Uni` or Java `CompletionStage` as the primary API for a use case.
- Make the component that creates the reactive factory responsible for closing it at shutdown.
- Decide whether ORM persistence context and dirty checking are needed or whether direct SQL and row mapping with R2DBC fit better.
- Do not run blocking JDBC, file I/O, or long CPU work in session callbacks running on the Vert.x dispatcher.
- Plan fetch joins, EntityGraphs, fetch profiles, or explicit `fetch()` calls so lazy associations are not read outside the session.

## Dependency {#coordinates}

Consumers manage only the central BOM version, not individual Hibernate Reactive or ORM versions.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-reactive")
}
```

Gradle project path: `:bluetape4k-hibernate-reactive`. Source directory: `data/hibernate-reactive`.

## First transaction {#quick-start}

Unwrap the JPA factory as a Mutiny factory, then await reactive work inside `withTransactionSuspending`.

```kotlin
import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.smallrye.mutiny.coroutines.awaitSuspending

val sessionFactory = entityManagerFactory.asMutinySessionFactory()

val saved = sessionFactory.withTransactionSuspending { session ->
    session.persist(author).awaitSuspending()
    author
}
```

`withTransactionSuspending` does not close the factory. Its owner must close it during application shutdown.

## API map {#api-by-task}

| Task | Mutiny | Stage | Boundary |
| --- | --- | --- | --- |
| Convert a JPA factory | `asMutinySessionFactory` | `asStageSessionFactory` | Unwraps the existing provider factory. |
| Run suspending session work | `withSessionSuspending` | same name | Hibernate Reactive owns the session lifecycle. |
| Define commit and rollback scope | `withTransactionSuspending` | same name | Completion and failure follow upstream `withTransaction`. |
| Work without a first-level cache | `withStatelessSessionSuspending` | same name | Use a regular Session when entity state tracking is required. |
| Typed entity lookup | `findAs<T>` | `findAs<T>` | Mutiny has additional `LockModeType` and EntityGraph overloads. |
| Typed query | `createSelectionQueryAs<R>` | same name | Failures surface through `Uni` or `CompletionStage`. |
| Graph and native mapping | `createEntityGraphAs`, `getResultSetMappingAs` | same names | Registered names and result types must match provider mappings. |

## Learning path {#concepts}

These chapters go beyond an API inventory. Each explains why a boundary matters, gives concrete examples, and links to the 1.12.1 release source and MySQL Testcontainers tests. Readers can move directly from explanation to executable evidence.

1. [Choosing and bootstrapping Mutiny or Stage](./bluetape4k-hibernate-reactive/mutiny-stage-bootstrap.md) — provider setup, factory unwrapping, and API differences.
2. [Session and transaction lifecycle](./bluetape4k-hibernate-reactive/session-transaction-lifecycle.md) — regular, tenant, and stateless scopes plus factory ownership.
3. [Typed queries and fetch plans](./bluetape4k-hibernate-reactive/typed-queries-fetching.md) — reified queries, lazy associations, fetch joins, and EntityGraphs.
4. [Using StatelessSession](./bluetape4k-hibernate-reactive/stateless-sessions.md) — benefits and limits of work without a first-level cache.
5. [Failures, cancellation, and operations](./bluetape4k-hibernate-reactive/failure-cancellation-operations.md) — exception propagation, cancellation limits, event loops, and observability.
6. [Choosing a persistence technology](./bluetape4k-hibernate-reactive/persistence-choice.md) — conventional Hibernate/JPA, Hibernate Reactive, and R2DBC boundaries.

For first adoption, follow chapters 1, 2, 3, then 5. Read chapter 4 for bulk work and chapter 6 while choosing the persistence layer.

## Recommended patterns {#patterns}

Use either Mutiny or Stage consistently within one use case. Place the transaction block around the smallest atomic application-service operation and resolve every lazy association with an explicit fetch plan inside that scope. Reserve stateless sessions for work that does not need a persistence context.

## Integrations {#integrations}

The module exposes `bluetape4k-hibernate`, `bluetape4k-mutiny`, `bluetape4k-vertx`, Hibernate Reactive, Mutiny Kotlin, and coroutine bridges as API dependencies. The transitive presence of `bluetape4k-hibernate` does not make blocking `EntityManager` or JDBC helpers safe inside a Vert.x session callback.

Hibernate Reactive uses the JPA metamodel generator instead of Querydsl. The `Author_` and `Book_` test types and Criteria examples show the intended typed-metamodel path.

## Configuration {#configuration}

A reactive persistence unit uses `org.hibernate.reactive.provider.ReactivePersistenceProvider` and may list entities explicitly. The 1.12.1 test XML uses the Jakarta Persistence 3.0 XML schema while the BOM resolves the Jakarta Persistence 3.2 API line. Do not describe the XML schema and library API version as the same setting.

The subordinate dependency versions printed in the module README do not match the 1.12.1 version catalog. Do not copy them into consumer instructions; verify compatibility through the central BOM.

## Failure behavior {#failures}

Exceptions from session work reach the caller. Transaction blocks delegate their boundary to Hibernate Reactive `withTransaction`; the extension adds no retry or compensation policy. Query syntax, mapping, and lock errors remain provider failures.

The coroutine bridge explicitly rethrows `CancellationException`. However, the release has no test proving immediate cancellation of an in-flight driver query. Do not promise that cancelling the coroutine immediately cancels its SQL statement.

## Operations {#operations}

Observe Vert.x event-loop delay, connection pool wait and utilization, query latency, transaction rollback, lock timeout, and lazy-fetch counts together. Correlate blocking-call detection and slow queries with the request context and verify factory shutdown completes.

## Testing {#testing}

The representative suites cover Mutiny and Stage factory unwrapping, session and transaction exception propagation, typed queries, EntityGraphs, and stateless work against MySQL Testcontainers. Docker is required and the test configuration disables parallel execution.

```bash
./gradlew :bluetape4k-hibernate-reactive:test --no-build-cache --no-configuration-cache
```

## Workshops {#workshops}

No separate workshop is registered yet. `MutinySessionFactoryExamples`, `StageSessionFactoryExamples`, both `SessionSupportTest` classes, and both `StatelessSessionExamples` classes serve as executable learning material linked from the chapters.

## 1.12.1 scope {#limitations}

This manual describes only production source and tests in the `bluetape4k-projects` 1.12.1 tag. The module does not provide schema migration, driver-level SQL cancellation guarantees, Querydsl, or a process-wide retry policy. There are no additional production APIs between 1.12.1 and the current source, but later version manuals must compare again.

## Source and tests {#sources}

- [Mutiny `SessionFactorySupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt)
- [Mutiny `SessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt)
- [Mutiny `StatelessSessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/StatelessSessionSupport.kt)
- [Stage `SessionFactorySupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt)
- [Stage `SessionSupport.kt`](../../../../data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt)
- [`MutinyExtrasTest.kt`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/mutiny/MutinyExtrasTest.kt)
- [`StageExtrasTest.kt`](../../../../data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/examples/stage/StageExtrasTest.kt)
- [`persistence.xml`](../../../../data/hibernate-reactive/src/test/resources/META-INF/persistence.xml)

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Reactive Extension Structure diagram

[![Reactive Extension Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-01.svg)

_Release README: [`data/hibernate-reactive/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.md)_

### Hibernate Reactive API Structure diagram

[![Hibernate Reactive API Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-02.svg)

_Release README: [`data/hibernate-reactive/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.md)_

### Session Type Comparison diagram

[![Session Type Comparison diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-hibernate-reactive-diagram-03.svg)

_Release README: [`data/hibernate-reactive/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/hibernate-reactive/README.md)_

<!-- release-readme-diagrams:end -->
