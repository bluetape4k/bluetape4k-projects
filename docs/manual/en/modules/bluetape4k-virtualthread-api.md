---
manualId: bluetape4k-virtualthread-api
title: "Virtual Thread Abstractions"
description: "An API module that abstracts virtual-thread features so they can be used independently of the concrete JDK version."
kind: library
group: concurrency
learningOrder: 220
---

# Virtual Thread Abstractions

## Problem {#problem}

An API module that abstracts virtual-thread features so they can be used independently of the concrete JDK version. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-virtualthread-api` when the application needs scope ownership, cancellation, executor lifecycle, blocking boundaries, and shutdown. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-virtualthread-api")
}
```

Gradle project path: `:bluetape4k-virtualthread-api`. Source directory: `virtualthread/api`.

## Concepts {#concepts}

The first source-level concepts to inspect are `StructuredScopes`, `TaskContext`, `VirtualThreadRuntime`, and `VirtualThreads`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TaskContext`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContext.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VirtualThreadRuntime`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadRuntime.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`VirtualThreads`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreads.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

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

- [`StructuredScopesTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopesTest.kt)
- [`TaskContextTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContextTest.kt)
- [`VirtualThreadsTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadsTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### virtualthread-api class structure for runtime facades, SPI contracts, StructuredTaskScope contracts, and TaskContext helpers

[![virtualthread-api class structure for runtime facades, SPI contracts, StructuredTaskScope contracts, and TaskContext helpers](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-01.svg)

_Release README: [`virtualthread/api/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/api/README.md)_

### ServiceLoader-based runtime selection flow for lazy provider discovery, supported-provider filtering, priority sorting, and fallback delegat

[![ServiceLoader-based runtime selection flow for lazy provider discovery, supported-provider filtering, priority sorting, and fallback delegat](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-api-diagram-02.svg)

_Release README: [`virtualthread/api/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/api/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../virtualthread/api/README.md)
- [Module build](../../../../virtualthread/api/build.gradle.kts)
- [`StructuredScopes`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopes.kt)
- [`TaskContext`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContext.kt)
- [`VirtualThreadRuntime`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadRuntime.kt)
- [`VirtualThreads`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreads.kt)
- [`StructuredScopesTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/StructuredScopesTest.kt)
- [`TaskContextTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/TaskContextTest.kt)
- [`VirtualThreadsTest`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/api/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/api/VirtualThreadsTest.kt)
