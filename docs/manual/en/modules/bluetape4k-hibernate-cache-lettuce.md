---
manualId: bluetape4k-hibernate-cache-lettuce
title: "Module bluetape4k-hibernate-cache-lettuce"
description: "Hibernate 7.2 2nd Level Cache implementation backed by Lettuce Near Cache (Caffeine L1 + Redis L2). Simply configure hibernate.cache.lettuce. properties and Near Cache is automati…"
kind: library
group: caching
---

# Module bluetape4k-hibernate-cache-lettuce

## Problem {#problem}

Hibernate 7.2 2nd Level Cache implementation backed by Lettuce Near Cache (Caffeine L1 + Redis L2). Simply configure hibernate.cache.lettuce. properties and Near Cache is automati… This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-hibernate-cache-lettuce` when the application needs cache key design, consistency, invalidation, and backend ownership. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-cache-lettuce")
}
```

Gradle project path: `:bluetape4k-hibernate-cache-lettuce`. Source directory: `cache/hibernate-cache-lettuce`.

## Concepts {#concepts}

The first source-level concepts to inspect are `LettuceNearCacheProperties`, `LettuceNearCacheRegionFactory`, and `LettuceNearCacheStorageAccess`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`LettuceNearCacheProperties`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`LettuceNearCacheProperties`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceNearCacheRegionFactory`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`LettuceNearCacheStorageAccess`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Package / Import Stability**, **Requirements**, **Architecture**, **Near Cache 2-Tier Structure**, **Layer Structure**, **Recent Changes**, **Dependencies**, **Configuration**, **Hibernate Properties**, and **application.yml (when used without Spring Boot)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-cache-lettuce"))
api(project(":bluetape4k-io"))
implementation(libs.fory.kotlin)
implementation(libs.lz4.java)
implementation(libs.snappy.java)
implementation(libs.zstd.jni)
api(project(":bluetape4k-lettuce"))
api(libs.hibernate.core)
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
./gradlew :bluetape4k-hibernate-cache-lettuce:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractHibernateNearCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/AbstractHibernateNearCacheTest.kt)
- [`HibernateAdvancedKeyCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateCacheContainmentTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheContainmentTest.kt)
- [`HibernateCacheStatisticsTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheStatisticsTest.kt)
- [`HibernateConcurrentWriteTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateConcurrentWriteTest.kt)
- [`HibernateElementCollectionCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateElementCollectionCacheTest.kt)
- [`HibernateEntityCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateFirstLevelCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateFirstLevelCacheTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../cache/hibernate-cache-lettuce/README.md)
- [Module build](../../../../cache/hibernate-cache-lettuce/build.gradle.kts)
- [`LettuceNearCacheProperties`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt)
- [`LettuceNearCacheRegionFactory`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`AbstractHibernateNearCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/AbstractHibernateNearCacheTest.kt)
- [`HibernateAdvancedKeyCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateCacheContainmentTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheContainmentTest.kt)
- [`HibernateCacheStatisticsTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheStatisticsTest.kt)
- [`HibernateConcurrentWriteTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateConcurrentWriteTest.kt)
- [`HibernateElementCollectionCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateElementCollectionCacheTest.kt)
- [`HibernateEntityCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateFirstLevelCacheTest`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateFirstLevelCacheTest.kt)
