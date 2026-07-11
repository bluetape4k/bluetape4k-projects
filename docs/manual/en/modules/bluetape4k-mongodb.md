---
manualId: bluetape4k-mongodb
title: "Module bluetape4k-mongodb"
description: "An extension library that makes the MongoDB Kotlin Coroutine Driver more convenient to use."
kind: library
group: data
---

# Module bluetape4k-mongodb

## Problem {#problem}

An extension library that makes the MongoDB Kotlin Coroutine Driver more convenient to use. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-mongodb` when the application needs transaction boundaries, connection ownership, query behavior, and serialization. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mongodb")
}
```

Gradle project path: `:bluetape4k-mongodb`. Source directory: `data/mongodb`.

## Concepts {#concepts}

The first source-level concepts to inspect are `MongoClientExtensions`, `MongoClientProvider`, `MongoClientSupport`, `MongoCollectionExtensions`, `MongoDatabaseExtensions`, `AggregationSupport`, and `DocumentExtensions`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`MongoClientExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`MongoClientExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MongoClientProvider`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MongoClientSupport`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MongoCollectionExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MongoDatabaseExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AggregationSupport`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DocumentExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture Diagrams**, **Core Class Structure**, **Module API Structure**, **Aggregation Pipeline Data Flow**, **Dependency**, **Core Features**, **1. Creating a MongoClient**, **2. Database & Collection Extensions**, and **3. Collection Convenience Functions**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-coroutines"))
api(libs.mongodb.driver.kotlin.coroutine)
api(libs.mongodb.driver.kotlin.extensions)
api(libs.mongo.bson.kotlin)
compileOnly(libs.mongo.bson.kotlinx)
implementation(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-mongodb:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractMongoTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/AbstractMongoTest.kt)
- [`MongoClientSupportTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`MongoDatabaseExtensionsTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensionsTest.kt)
- [`AggregationSupportTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
- [`DocumentExtensionsTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensionsTest.kt)
- [`AggregationExamples`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/AggregationExamples.kt)
- [`BasicCrudExamples`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../data/mongodb/README.md)
- [Module build](../../../../data/mongodb/build.gradle.kts)
- [`MongoClientExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt)
- [`MongoClientProvider`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt)
- [`MongoClientSupport`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt)
- [`MongoCollectionExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt)
- [`MongoDatabaseExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`AggregationSupport`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt)
- [`DocumentExtensions`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt)
- [`AbstractMongoTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/AbstractMongoTest.kt)
- [`MongoClientSupportTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`MongoDatabaseExtensionsTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensionsTest.kt)
- [`AggregationSupportTest`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
