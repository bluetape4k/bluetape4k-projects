---
manualId: bluetape4k-jdbc
title: "Module bluetape4k-jdbc"
description: "A Kotlin extension library that reduces boilerplate when working with JDBC (Java Database Connectivity)."
kind: library
group: data
---

# Module bluetape4k-jdbc

## Problem {#problem}

A Kotlin extension library that reduces boilerplate when working with JDBC (Java Database Connectivity). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-jdbc` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jdbc")
}
```

Gradle project path: `:bluetape4k-jdbc`. Source directory: `data/jdbc`.

## Concepts {#concepts}

The first source-level concepts to inspect are `JdbcDrivers`, `HikariSupport`, `ArgumentSetter`, `ConnectionExtensions`, `DataSourceExtensions`, `DataSourceTransactionExtensions`, and `GetColumnToken`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`HikariSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ArgumentSetter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ConnectionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DataSourceExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DataSourceTransactionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`GetColumnToken`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/GetColumnToken.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PrepareStatementSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`PreparedStatementArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture Diagrams**, **Extension Function API Overview**, **Core API Structure**, **JDBC Query Execution Flow**, **Dependency**, **Core Features**, **1. DataSource/Connection Management**, and **2. Executing Statements**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(libs.hikaricp)
compileOnly(libs.tomcat.jdbc)
compileOnly(libs.agroal.spring.boot.starter)
compileOnly("org.springframework.boot:spring-boot-starter-jdbc")
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
./gradlew :bluetape4k-jdbc:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractJdbcTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/AbstractJdbcTest.kt)
- [`JdbcDriversTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/JdbcDriversTest.kt)
- [`HikariSupportTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/hikari/HikariSupportTest.kt)
- [`Actor`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/model/Actor.kt)
- [`TestBean`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/model/TestBean.kt)
- [`AbstractJdbcSqlTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/AbstractJdbcSqlTest.kt)
- [`ConnectionExtensionsTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensionsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/jdbc/README.md)
- [Module build](../../../../data/jdbc/build.gradle.kts)
- [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt)
- [`HikariSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt)
- [`ArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ArgumentSetter.kt)
- [`ConnectionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt)
- [`DataSourceExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`DataSourceTransactionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`GetColumnToken`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/GetColumnToken.kt)
- [`PrepareStatementSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt)
- [`PreparedStatementArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt)
- [`AbstractJdbcTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/AbstractJdbcTest.kt)
- [`JdbcDriversTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/JdbcDriversTest.kt)
