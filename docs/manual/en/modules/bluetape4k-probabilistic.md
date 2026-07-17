---
manualId: bluetape4k-probabilistic
title: "Probabilistic Data Structures"
description: "In-memory probabilistic data structures for JVM applications."
kind: library
group: utilities
learningOrder: 1260
---

# Probabilistic Data Structures

## Problem {#problem}

In-memory probabilistic data structures for JVM applications. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-probabilistic` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-probabilistic")
}
```

Gradle project path: `:bluetape4k-probabilistic`. Source directory: `utils/probabilistic`.

## Concepts {#concepts}

The first source-level concepts to inspect are `BloomFilter`, `BloomFilterConfig`, `BloomFilters`, `BloomHasher`, `InMemoryBloomFilter`, `InMemoryMutableBloomFilter`, `InMemorySuspendBloomFilter`, and `MutableBloomFilter`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BloomFilterConfig`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BloomFilters`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilters.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`BloomHasher`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomHasher.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`InMemoryBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`InMemoryMutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryMutableBloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`InMemorySuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/MutableBloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/SuspendBloomFilter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Gradle**, **Bloom Filter**, **Coroutine API**, and **Notes**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
compileOnly(libs.kotlinx.coroutines.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-probabilistic:test --no-configuration-cache
```

Representative test anchors:

- [`BloomFilterConfigTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfigTest.kt)
- [`InMemoryBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilterTest.kt)
- [`InMemorySuspendBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilterTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../utils/probabilistic/README.md)
- [Module build](../../../../utils/probabilistic/build.gradle.kts)
- [`BloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilter.kt)
- [`BloomFilterConfig`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfig.kt)
- [`BloomFilters`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilters.kt)
- [`BloomHasher`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomHasher.kt)
- [`InMemoryBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilter.kt)
- [`InMemoryMutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryMutableBloomFilter.kt)
- [`InMemorySuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilter.kt)
- [`MutableBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/MutableBloomFilter.kt)
- [`SuspendBloomFilter`](../../../../utils/probabilistic/src/main/kotlin/io/bluetape4k/probabilistic/bloomfilter/SuspendBloomFilter.kt)
- [`BloomFilterConfigTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/BloomFilterConfigTest.kt)
- [`InMemoryBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemoryBloomFilterTest.kt)
- [`InMemorySuspendBloomFilterTest`](../../../../utils/probabilistic/src/test/kotlin/io/bluetape4k/probabilistic/bloomfilter/InMemorySuspendBloomFilterTest.kt)
