---
manualId: bluetape4k-spring-boot-mongodb
title: "Module bluetape4k-spring-boot-mongodb"
description: "An extension library for working with Spring Data MongoDB Reactive using Kotlin Coroutines (Spring Boot 4.x)."
kind: library
group: spring
---

# Module bluetape4k-spring-boot-mongodb

## Problem {#problem}

An extension library for working with Spring Data MongoDB Reactive using Kotlin Coroutines (Spring Boot 4.x). This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-spring-boot-mongodb` when the application needs auto-configuration conditions, bean ownership, property binding, and application lifecycle. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb")
}
```

Gradle project path: `:bluetape4k-spring-boot-mongodb`. Source directory: `spring-boot/mongodb`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ReactiveMongoAutoConfiguration`, `ReactiveMongoOperationsCoroutines`, `CriteriaExtensions`, `QueryExtensions`, and `UpdateExtensions`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ReactiveMongoAutoConfiguration`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ReactiveMongoAutoConfiguration`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ReactiveMongoOperationsCoroutines`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CriteriaExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`QueryExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`UpdateExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Diagrams**, **Core Class Structure**, **ReactiveMongoOperations Coroutine Extension Flow**, **Criteria / Query / Update DSL Flow**, **Coroutine Conversion Sequence**, **Installation**, **Usage Examples**, **ReactiveMongoOperations Coroutine Extensions**, and **Criteria infix DSL**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-spring-boot-core"))
implementation(libs.mongodb.driver.kotlin.sync)
implementation(libs.mongodb.driver.kotlin.coroutine)
implementation(libs.mongodb.driver.kotlin.extensions)
api("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
compileOnly("org.springframework.boot:spring-boot-autoconfigure")
compileOnly("org.springframework.boot:spring-boot-configuration-processor")
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
api(libs.kotlinx.coroutines.reactor)
implementation(libs.reactor.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](../../../../spring-boot/mongodb/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track condition reports, startup failures, pool/client health, request latency, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractReactiveMongoCoroutineTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoCoroutineTest.kt)
- [`AbstractReactiveMongoTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt)
- [`MongoTestApplication`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [`ReactiveMongoOperationsCoroutinesTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`User`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/model/User.kt)
- [`CriteriaExtensionsTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensionsTest.kt)
- [`QueryExtensionsTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)
- [`UpdateExtensionsTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensionsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../spring-boot/mongodb/README.md)
- [Module build](../../../../spring-boot/mongodb/build.gradle.kts)
- [`ReactiveMongoAutoConfiguration`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt)
- [`ReactiveMongoOperationsCoroutines`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`CriteriaExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensions.kt)
- [`QueryExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensions.kt)
- [`UpdateExtensions`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt)
- [`AbstractReactiveMongoCoroutineTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoCoroutineTest.kt)
- [`AbstractReactiveMongoTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt)
- [`MongoTestApplication`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [`ReactiveMongoOperationsCoroutinesTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`User`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/model/User.kt)
- [`CriteriaExtensionsTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensionsTest.kt)
- [`QueryExtensionsTest`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)
