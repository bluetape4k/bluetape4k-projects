---
manualId: bluetape4k-r2dbc
title: "Module bluetape4k-r2dbc"
description: "A library that supports reactive data access using Coroutines and Flow in an R2DBC (Reactive Relational Database Connectivity) environment."
kind: library
group: data
---

# Module bluetape4k-r2dbc

## Problem {#problem}

A library that supports reactive data access using Coroutines and Flow in an R2DBC (Reactive Relational Database Connectivity) environment. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-r2dbc` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-r2dbc")
}
```

Gradle project path: `:bluetape4k-r2dbc`. Source directory: `data/r2dbc`.

## Concepts {#concepts}

The first source-level concepts to inspect are `R2dbcClient`, `R2dbcClientAutoConfiguration`, `ConnectionFactoryUtils`, `R2dbcTransactionManager`, `CompositeDatabasePopulator`, `ConnectionFactoryInitializer`, `ResourceDatabasePopulator`, and `MappingR2dbcConverter`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`R2dbcClientAutoConfiguration`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConnectionFactoryUtils`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`R2dbcTransactionManager`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/R2dbcTransactionManager.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CompositeDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/CompositeDatabasePopulator.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConnectionFactoryInitializer`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ResourceDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ResourceDatabasePopulator.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MappingR2dbcConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/MappingR2dbcConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonToMapConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/JsonToMapConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MapToJsonConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/MapToJsonConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture Diagrams**, **Extension Function API Overview**, **Core API Class Structure**, **R2DBC Query Execution Flow**, **JDBC vs R2DBC Comparison**, **Dependency**, **Core Features**, **1. R2DBC Connection Pool Tuning**, and **Tuning guide from the measurement**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-jackson3"))
compileOnly(libs.jackson3.module.kotlin)
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
api(libs.kotlinx.coroutines.reactive)
api(libs.kotlinx.coroutines.reactor)
compileOnly(libs.reactor.core)
compileOnly(libs.reactor.kotlin.extensions)
api(libs.r2dbc.pool)
compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`spring.factories`](../../../../data/r2dbc/src/main/resources/META-INF/spring.factories)
- [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](../../../../data/r2dbc/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track pool saturation, query latency, retries, transaction rollbacks, and schema compatibility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-r2dbc:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractR2dbcTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/AbstractR2dbcTest.kt)
- [`R2dbcTestApplication`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/R2dbcTestApplication.kt)
- [`R2dbcClientAutoConfigurationTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfigurationTest.kt)
- [`R2dbcConfigurationTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/config/R2dbcConfigurationTest.kt)
- [`ConnectionInitTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionInitTest.kt)
- [`PostgresJsonConvertersTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/convert/postgresql/PostgresJsonConvertersTest.kt)
- [`DatabaseClientBuilderTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DatabaseClientBuilderTest.kt)
- [`DeleteTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DeleteTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/r2dbc/README.md)
- [Module build](../../../../data/r2dbc/build.gradle.kts)
- [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt)
- [`R2dbcClientAutoConfiguration`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt)
- [`ConnectionFactoryUtils`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt)
- [`R2dbcTransactionManager`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/R2dbcTransactionManager.kt)
- [`CompositeDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/CompositeDatabasePopulator.kt)
- [`ConnectionFactoryInitializer`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt)
- [`ResourceDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ResourceDatabasePopulator.kt)
- [`MappingR2dbcConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/MappingR2dbcConverter.kt)
- [`JsonToMapConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/JsonToMapConverter.kt)
- [`MapToJsonConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/MapToJsonConverter.kt)
- [`AbstractR2dbcTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/AbstractR2dbcTest.kt)
- [`R2dbcTestApplication`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/R2dbcTestApplication.kt)
