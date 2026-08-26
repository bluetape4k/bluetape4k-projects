---
manualId: bluetape4k-virtualthread-jdk25
title: "Virtual Threads for JDK 25"
description: "Java 25 virtual-thread implementation module."
kind: library
group: concurrency
learningOrder: 240
---

# Virtual Threads for JDK 25

## Problem {#problem}

Java 25 virtual-thread implementation module. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-virtualthread-jdk25` when the application needs scope ownership, cancellation, executor lifecycle, blocking boundaries, and shutdown. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-virtualthread-jdk25")
}
```

Gradle project path: `:bluetape4k-virtualthread-jdk25`. Source directory: `virtualthread/jdk25`.

## Concepts {#concepts}

The first source-level concepts to inspect are `Jdk25VirtualThreadRuntime`, and `Jdk25StructuredTaskScopeProvider`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`Jdk25VirtualThreadRuntime`](../../../../virtualthread/jdk25/src/main/java/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25VirtualThreadRuntime.java) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`Jdk25VirtualThreadRuntime`](../../../../virtualthread/jdk25/src/main/java/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25VirtualThreadRuntime.java) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Jdk25StructuredTaskScopeProvider`](../../../../virtualthread/jdk25/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProvider.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Implementation Structure**, **Main Implementations**, **Jdk25VirtualThreadRuntime**, **Jdk25StructuredTaskScopeProvider**, **ServiceLoader Configuration**, **Build Configuration**, **Dependencies**, **Gradle Dependencies**, and **Gradle Usage Example**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-virtualthread-api"))
implementation(project(":bluetape4k-logging"))
implementation(libs.kotlinx.coroutines.core)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeProvider`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/jdk25/src/main/resources/META-INF/services/io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeProvider)
- [`io.bluetape4k.concurrent.virtualthread.api.VirtualThreadRuntime`](https://github.com/bluetape4k/bluetape4k-projects/blob/64edee3462c2dcfacde23e8dabaf962c6779921d/virtualthread/jdk25/src/main/resources/META-INF/services/io.bluetape4k.concurrent.virtualthread.api.VirtualThreadRuntime)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Track active work, queue depth, cancellation, timeout, executor saturation, and shutdown completion. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-virtualthread-jdk25:test --no-configuration-cache
```

Representative test anchors:

- [`Jdk25StructuredTaskScopeProviderExtTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProviderExtTest.kt)
- [`Jdk25StructuredTaskScopeProviderTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProviderTest.kt)
- [`Jdk25TaskContextTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25TaskContextTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### JDK 25 virtual-thread runtime and StructuredTaskScope Joiner provider structure

[![JDK 25 virtual-thread runtime and StructuredTaskScope Joiner provider structure](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-jdk25-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-jdk25-diagram-01.svg)

_Release README: [`virtualthread/jdk25/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/jdk25/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../virtualthread/jdk25/README.md)
- [Module build](../../../../virtualthread/jdk25/build.gradle.kts)
- [`Jdk25VirtualThreadRuntime`](../../../../virtualthread/jdk25/src/main/java/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25VirtualThreadRuntime.java)
- [`Jdk25StructuredTaskScopeProvider`](../../../../virtualthread/jdk25/src/main/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProvider.kt)
- [`Jdk25StructuredTaskScopeProviderExtTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProviderExtTest.kt)
- [`Jdk25StructuredTaskScopeProviderTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25StructuredTaskScopeProviderTest.kt)
- [`Jdk25TaskContextTest`](../../../../virtualthread/jdk25/src/test/kotlin/io/bluetape4k/concurrent/virtualthread/jdk25/Jdk25TaskContextTest.kt)
