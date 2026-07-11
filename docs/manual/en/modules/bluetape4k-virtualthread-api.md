---
manualId: bluetape4k-virtualthread-api
title: "Module bluetape4k-virtualthread-api"
description: "An API module that abstracts virtual-thread features so they can be used independently of the concrete JDK version."
kind: library
group: concurrency
---

# Module bluetape4k-virtualthread-api

## Problem {#problem}

An API module that abstracts virtual-thread features so they can be used independently of the concrete JDK version. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-virtualthread-api` when the application needs scope ownership, cancellation, executor lifecycle, blocking boundaries, and shutdown. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-virtualthread-api")
}
```

Gradle project path: `:bluetape4k-virtualthread-api`. Source directory: `virtualthread/api`.

## Concepts {#concepts}

The first source-level concepts to inspect are `StructuredScopes`, `TaskContext`, `VirtualThreadRuntime`, and `VirtualThreads`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`StructuredScopes`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`StructuredScopes`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TaskContext`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/TaskContext.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VirtualThreadRuntime`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreadRuntime.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VirtualThreads`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreads.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Key Features**, **1. VirtualThreads - Runtime Selection and Executor Creation**, **2. VirtualThreadRuntime - Implementation Interface**, **3. StructuredTaskScopes - Structured Concurrency**, **Choosing the Right API**, **failFast — All Must Succeed**, **firstSuccess — First Winner Takes All**, **supervised — Partial Failure Tolerance**, and **getOrNull() — Safe Result Access**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(project(":bluetape4k-logging"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track active work, queue depth, cancellation, timeout, executor saturation, and shutdown completion. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-virtualthread-api:test --no-configuration-cache
```

Representative test anchors:

- [`StructuredScopesTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopesTest.kt)
- [`TaskContextTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/TaskContextTest.kt)
- [`VirtualThreadsTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreadsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../virtualthread/api/README.md)
- [Module build](../../../../virtualthread/api/build.gradle.kts)
- [`StructuredScopes`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopes.kt)
- [`TaskContext`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/TaskContext.kt)
- [`VirtualThreadRuntime`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreadRuntime.kt)
- [`VirtualThreads`](../../../../virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreads.kt)
- [`StructuredScopesTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/StructuredScopesTest.kt)
- [`TaskContextTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/TaskContextTest.kt)
- [`VirtualThreadsTest`](../../../../virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/VirtualThreadsTest.kt)
