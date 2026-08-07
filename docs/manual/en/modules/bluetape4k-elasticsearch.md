---
manualId: bluetape4k-elasticsearch
title: "Elasticsearch Client Extensions"
description: "Elasticsearch client library for Kotlin with Coroutines support."
kind: library
group: data
learningOrder: 660
---

# Elasticsearch Client Extensions

## Problem {#problem}

Elasticsearch client library for Kotlin with Coroutines support. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-elasticsearch` when the application needs client lifecycle, reconnect policy, backpressure, retries, and observability. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-elasticsearch")
}
```

Gradle project path: `:bluetape4k-elasticsearch`. Source directory: `infra/elasticsearch`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ElasticsearchClientDsl`, `ElasticsearchClients`, `ElasticsearchDefaults`, `BulkApiCoroutines`, `BulkIngesterCoroutines`, `ElasticsearchCoroutines`, `SearchApiCoroutines`, and `JsonpMappers`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ElasticsearchClientDsl`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClientDsl.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ElasticsearchClientDsl`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClientDsl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ElasticsearchClients`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClients.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ElasticsearchDefaults`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchDefaults.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BulkApiCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkApiCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BulkIngesterCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkIngesterCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ElasticsearchCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/ElasticsearchCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SearchApiCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/SearchApiCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JsonpMappers`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/support/JsonpMappers.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Features**, **Architecture**, **Installation**, **Gradle (Kotlin DSL)**, **Gradle (Groovy DSL)**, **Maven**, **Dependencies**, **Client API Structure**, **Usage Examples**, and **1. Creating an Elasticsearch Async Client**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(project(":bluetape4k-io"))
api(project(":bluetape4k-logging"))
api(libs.elasticsearch.java)
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
compileOnly(project(":bluetape4k-jackson2"))
compileOnly(libs.jackson.databind)
compileOnly(libs.jackson.module.kotlin)
compileOnly(project(":bluetape4k-jackson3"))
compileOnly(libs.jackson3.databind)
compileOnly(libs.jackson3.module.kotlin)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track connection state, queue depth, retries, timeouts, remote errors, and graceful shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-elasticsearch:test --no-configuration-cache
```

Representative test anchors:

- [`ElasticsearchClientsTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClientsTest.kt)
- [`BulkApiCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkApiCoroutinesTest.kt)
- [`BulkIngesterCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkIngesterCoroutinesTest.kt)
- [`ElasticsearchCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/ElasticsearchCoroutinesTest.kt)
- [`SearchApiCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/SearchApiCoroutinesTest.kt)
- [`ProductIndexExample`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/examples/ProductIndexExample.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Elasticsearch client API structure

[![Elasticsearch client API structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-elasticsearch-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-elasticsearch-diagram-01.svg)

_Release README: [`infra/elasticsearch/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/elasticsearch/README.md)_

### Elasticsearch module architecture

[![Elasticsearch module architecture](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-elasticsearch-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/infra-elasticsearch-diagram-02.svg)

_Release README: [`infra/elasticsearch/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/infra/elasticsearch/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../infra/elasticsearch/README.md)
- [Module build](../../../../infra/elasticsearch/build.gradle.kts)
- [`ElasticsearchClientDsl`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClientDsl.kt)
- [`ElasticsearchClients`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClients.kt)
- [`ElasticsearchDefaults`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/ElasticsearchDefaults.kt)
- [`BulkApiCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkApiCoroutines.kt)
- [`BulkIngesterCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkIngesterCoroutines.kt)
- [`ElasticsearchCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/ElasticsearchCoroutines.kt)
- [`SearchApiCoroutines`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/coroutines/SearchApiCoroutines.kt)
- [`JsonpMappers`](../../../../infra/elasticsearch/src/main/kotlin/io/bluetape4k/elasticsearch/support/JsonpMappers.kt)
- [`ElasticsearchClientsTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/ElasticsearchClientsTest.kt)
- [`BulkApiCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkApiCoroutinesTest.kt)
- [`BulkIngesterCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/BulkIngesterCoroutinesTest.kt)
- [`ElasticsearchCoroutinesTest`](../../../../infra/elasticsearch/src/test/kotlin/io/bluetape4k/elasticsearch/coroutines/ElasticsearchCoroutinesTest.kt)
