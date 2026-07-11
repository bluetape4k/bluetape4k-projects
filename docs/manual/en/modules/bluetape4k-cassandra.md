---
manualId: bluetape4k-cassandra
title: "Module bluetape4k-cassandra"
description: "A Kotlin extension library that makes it easier to use the Apache Cassandra Java Driver."
kind: library
group: data
---

# Module bluetape4k-cassandra

## Problem {#problem}

A Kotlin extension library that makes it easier to use the Apache Cassandra Java Driver. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-cassandra` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

Gradle project path: `:bluetape4k-cassandra`. Source directory: `data/cassandra`.

## Concepts {#concepts}

The first source-level concepts to inspect are `CassandraAdmin`, `CqlIdentifierSupport`, `CqlQuerySupport`, `CqlSessionProvider`, `CqlSessionSupport`, `AsyncCqlSessionSupport`, `AsyncResultSetSupport`, and `DataTypeSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`CassandraAdmin`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`CassandraAdmin`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CqlIdentifierSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlIdentifierSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CqlQuerySupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlQuerySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CqlSessionProvider`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CqlSessionSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncCqlSessionSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AsyncResultSetSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DataTypeSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/DataTypeSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RowSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StatementSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture Diagrams**, **Extension Function API Overview**, **Core API Structure**, **Asynchronous Query Execution Flow**, **Dependency**, **Core Features**, **1. Creating a CqlSession**, **Cached sessions with explicit identity**, and **2. Asynchronous Queries (Coroutines)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-coroutines"))
api(libs.cassandra.java.driver.core)
api(libs.cassandra.java.driver.query.builder)
api(libs.cassandra.java.driver.mapper.runtime)
compileOnly(libs.cassandra.java.driver.metrics.micrometer)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.reactor)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track pool saturation, query latency, retries, transaction rollbacks, and schema compatibility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-cassandra:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractCassandraTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/AbstractCassandraTest.kt)
- [`CassandraAdminTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CassandraAdminTest.kt)
- [`CqlIdentifierSupportTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlIdentifierSupportTest.kt)
- [`CqlQuerySupportTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlQuerySupportTest.kt)
- [`CqlSessionProviderTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt)
- [`CqlSessionSupportTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt)
- [`AsyncCqlSessionSupportTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupportTest.kt)
- [`AsyncResultSetSupportTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/cassandra/README.md)
- [Module build](../../../../data/cassandra/build.gradle.kts)
- [`CassandraAdmin`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt)
- [`CqlIdentifierSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlIdentifierSupport.kt)
- [`CqlQuerySupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlQuerySupport.kt)
- [`CqlSessionProvider`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt)
- [`CqlSessionSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt)
- [`AsyncCqlSessionSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt)
- [`AsyncResultSetSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt)
- [`DataTypeSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/DataTypeSupport.kt)
- [`RowSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt)
- [`StatementSupport`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt)
- [`AbstractCassandraTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/AbstractCassandraTest.kt)
- [`CassandraAdminTest`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CassandraAdminTest.kt)
