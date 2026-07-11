---
manualId: bluetape4k-cache-core
title: "Module bluetape4k-cache-core"
description: "bluetape4k-cache-core provides the shared cache API, core abstractions, and local cache implementations."
kind: library
group: caching
---

# Module bluetape4k-cache-core

## Problem {#problem}

bluetape4k-cache-core provides the shared cache API, core abstractions, and local cache implementations. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-cache-core` when the application needs cache key design, consistency, invalidation, and backend ownership. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cache-core")
}
```

Gradle project path: `:bluetape4k-cache-core`. Source directory: `cache/cache-core`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Cache2kSupport`, `CaffeineSupport`, `EhcacheSupport`, `CaffeineSuspendJCache`, `JCacheEntryEventListener`, `JCacheSupport`, `JCacheType`, and `JCaching`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CaffeineSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`EhcacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CaffeineSuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JCacheEntryEventListener`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListener.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JCacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JCacheType`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`JCaching`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCaching.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCache.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendJCacheEntry`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntry.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

Choose one loading contract explicitly. With **cache-aside**, the caller handles a miss, loads the value, and writes it back. With **read-through**, the cache loader owns that miss path. With **write-through**, the cache API propagates the write to the backing store before reporting success; do not describe a plain `put` as write-through unless its implementation has that contract. For a two-level Near Cache, read L1 first, consult L2 on a miss, then fill L1. Write or invalidate L2 and L1 in the order required by the implementation, and test partial failure so stale L1 data cannot silently survive a failed backend update.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-io"))
api(project(":bluetape4k-idgenerators"))
api(libs.javax.cache.api)
api(libs.caffeine)
api(libs.caffeine.jcache)
compileOnly(libs.cache2k.core)
compileOnly(libs.cache2k.jcache)
compileOnly(libs.ehcache)
compileOnly(libs.ehcache.clustered)
compileOnly(libs.ehcache.transactions)
implementation(libs.resilience4j.retry)
implementation(libs.resilience4j.kotlin)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track hit ratio, load latency, eviction, stale reads, backend errors, and reconnect behavior. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
```

Representative test anchors:

- [`Cache2kSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportExtTest.kt)
- [`Cache2kSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportTest.kt)
- [`CaffeineSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupportTest.kt)
- [`EhcacheSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupportTest.kt)
- [`CaffeineSuspendJCacheTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCacheTest.kt)
- [`JCacheEntryEventListenerTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListenerTest.kt)
- [`JCacheReadWriteThroughExample`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheReadWriteThroughExample.kt)
- [`JCacheSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../cache/cache-core/README.md)
- [Module build](../../../../cache/cache-core/build.gradle.kts)
- [`Cache2kSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt)
- [`CaffeineSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`EhcacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt)
- [`CaffeineSuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCache.kt)
- [`JCacheEntryEventListener`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheEntryEventListener.kt)
- [`JCacheSupport`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`JCacheType`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheType.kt)
- [`JCaching`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCaching.kt)
- [`SuspendJCache`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCache.kt)
- [`SuspendJCacheEntry`](../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntry.kt)
- [`Cache2kSupportExtTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportExtTest.kt)
- [`Cache2kSupportTest`](../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupportTest.kt)
