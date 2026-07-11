---
manualId: bluetape4k-cache-lettuce
title: "Module bluetape4k-cache-lettuce"
description: "bluetape4k-cache-lettuce provides a Lettuce (Redis)-based JCache provider and NearCache implementations."
kind: library
group: caching
---

# Module bluetape4k-cache-lettuce

## Problem {#problem}

bluetape4k-cache-lettuce provides a Lettuce (Redis)-based JCache provider and NearCache implementations. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-cache-lettuce` when the application needs cache key design, consistency, invalidation, and backend ownership. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-lettuce")
}
```

Gradle project path: `:bluetape4k-cache-lettuce`. Source directory: `cache/cache-lettuce`.

## Concepts {#concepts}

The first source-level concepts to inspect are `LettuceCaches`, `LettuceCacheConfig`, `LettuceCacheManager`, `LettuceCachingProvider`, `LettuceJCache`, `LettuceJCaching`, `LettuceSuspendCacheManager`, and `LettuceSuspendJCache`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`LettuceCaches`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`LettuceCaches`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceCacheConfig`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceCacheManager`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceCachingProvider`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceJCache`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceJCaching`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCaching.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceSuspendCacheManager`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceSuspendJCache`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceAsyncMemoizer`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceMemoizer`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceMemoizer.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

Choose one loading contract explicitly. With **cache-aside**, the caller handles a miss, loads the value, and writes it back. With **read-through**, the cache loader owns that miss path. With **write-through**, the cache API propagates the write to the backing store before reporting success; do not describe a plain `put` as write-through unless its implementation has that contract. For a two-level Near Cache, read L1 first, consult L2 on a miss, then fill L1. Write or invalidate L2 and L1 in the order required by the implementation, and test partial failure so stale L1 data cannot silently survive a failed backend update.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-cache-core"))
api(project(":bluetape4k-lettuce"))
api(libs.lettuce.core)
api(libs.caffeine)
implementation(project(":bluetape4k-coroutines"))
implementation(project(":bluetape4k-resilience4j"))
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.reactive)
implementation(project(":bluetape4k-protobuf"))
implementation(project(":bluetape4k-io"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`javax.cache.spi.CachingProvider`](../../../../cache/cache-lettuce/src/main/resources/META-INF/services/javax.cache.spi.CachingProvider)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track hit ratio, load latency, eviction, stale reads, backend errors, and reconnect behavior. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-cache-lettuce:test --no-configuration-cache
```

Representative test anchors:

- [`LettuceJCachesTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/LettuceJCachesTest.kt)
- [`RedisServers`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/RedisServers.kt)
- [`LettuceCachingProviderTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProviderTest.kt)
- [`LettuceJCacheManagerTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheManagerTest.kt)
- [`LettuceJCacheTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheTest.kt)
- [`LettuceSuspendJCacheManagerTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCacheManagerTest.kt)
- [`LettuceSuspendJCacheTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCacheTest.kt)
- [`LettuceAsyncMemoizerTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizerTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../cache/cache-lettuce/README.md)
- [Module build](../../../../cache/cache-lettuce/build.gradle.kts)
- [`LettuceCaches`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/LettuceCaches.kt)
- [`LettuceCacheConfig`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt)
- [`LettuceCacheManager`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt)
- [`LettuceCachingProvider`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt)
- [`LettuceJCache`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt)
- [`LettuceJCaching`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCaching.kt)
- [`LettuceSuspendCacheManager`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt)
- [`LettuceSuspendJCache`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt)
- [`LettuceAsyncMemoizer`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceAsyncMemoizer.kt)
- [`LettuceMemoizer`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/memoizer/LettuceMemoizer.kt)
- [`LettuceJCachesTest`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/LettuceJCachesTest.kt)
- [`RedisServers`](../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/RedisServers.kt)
