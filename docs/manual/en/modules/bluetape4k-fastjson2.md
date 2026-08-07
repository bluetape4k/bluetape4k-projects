---
manualId: bluetape4k-fastjson2
title: "Fastjson2 Serialization"
description: "bluetape4k-fastjson2 is a module that wraps the Fastjson2 library as Kotlin extension functions."
kind: library
group: io
learningOrder: 340
---

# Fastjson2 Serialization

## Problem {#problem}

bluetape4k-fastjson2 is a module that wraps the Fastjson2 library as Kotlin extension functions. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-fastjson2` when the application needs encoding boundaries, resource ownership, streaming, compatibility, and malformed input. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-fastjson2")
}
```

Gradle project path: `:bluetape4k-fastjson2`. Source directory: `io/fastjson2`.

## Concepts {#concepts}

The first source-level concepts to inspect are `FastjsonSerializer`, `JSONArrayExtensions`, `JSONBExtensions`, `JSONExtensions`, and `JSONObjectExtensions`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`FastjsonSerializer`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`FastjsonSerializer`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JSONArrayExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONArrayExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JSONBExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONBExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JSONExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JSONObjectExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONObjectExtensions.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Architecture Diagrams**, **Fastjson2 Class Structure**, **JSON vs JSONB Serialization Flow**, **Key Features**, **1. FastjsonSerializer**, **2. JSON String Extension Functions**, **3. JSONB Binary Extension Functions**, **4. JSONArray Extension Functions**, and **5. JSONObject Extension Functions**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(libs.fastjson2)
api(libs.fastjson2.kotlin)
api(project(":bluetape4k-json"))
api(project(":bluetape4k-io"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track payload size, allocation, latency, malformed-input rate, resource closure, and protocol errors. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-fastjson2:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractFastjson2Test`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/AbstractFastjson2Test.kt)
- [`AbstractJsonSerializerTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/AbstractJsonSerializerTest.kt)
- [`FastjsonSerializerTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/FastjsonSerializerTest.kt)
- [`JSONArrayExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONArrayExtensionsTest.kt)
- [`JSONBExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONBExtensionsTest.kt)
- [`JSONExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONExtensionsTest.kt)
- [`JSONObjectExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONObjectExtensionsTest.kt)
- [`models`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/model/models.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Fastjson2 Class Structure diagram

[![Fastjson2 Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-fastjson2-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-fastjson2-diagram-01.svg)

_Release README: [`io/fastjson2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/fastjson2/README.md)_

### JSON vs JSONB Serialization Flow diagram

[![JSON vs JSONB Serialization Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-fastjson2-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/io-fastjson2-diagram-02.svg)

_Release README: [`io/fastjson2/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/io/fastjson2/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../io/fastjson2/README.md)
- [Module build](../../../../io/fastjson2/build.gradle.kts)
- [`FastjsonSerializer`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt)
- [`JSONArrayExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONArrayExtensions.kt)
- [`JSONBExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONBExtensions.kt)
- [`JSONExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONExtensions.kt)
- [`JSONObjectExtensions`](../../../../io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/extensions/JSONObjectExtensions.kt)
- [`AbstractFastjson2Test`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/AbstractFastjson2Test.kt)
- [`AbstractJsonSerializerTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/AbstractJsonSerializerTest.kt)
- [`FastjsonSerializerTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/FastjsonSerializerTest.kt)
- [`JSONArrayExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONArrayExtensionsTest.kt)
- [`JSONBExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONBExtensionsTest.kt)
- [`JSONExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONExtensionsTest.kt)
- [`JSONObjectExtensionsTest`](../../../../io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/extensions/JSONObjectExtensionsTest.kt)
