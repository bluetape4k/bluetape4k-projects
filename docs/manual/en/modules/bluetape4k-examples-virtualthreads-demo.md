---
manualId: bluetape4k-examples-virtualthreads-demo
title: "Java Virtual Thread Examples"
description: "A collection of examples covering best practices and rules for using Java 25 Virtual Threads and Structured Concurrency effectively."
kind: example
group: examples
learningOrder: 1410
---

# Java Virtual Thread Examples

## Problem {#problem}

A collection of examples covering best practices and rules for using Java 25 Virtual Threads and Structured Concurrency effectively. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-examples-virtualthreads-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:bluetape4k-examples-virtualthreads-demo`. Source directory: `examples/virtualthreads-demo`.

## Concepts {#concepts}

The module is configuration or platform metadata and has no Kotlin/Java source type to index.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :bluetape4k-examples-virtualthreads-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

No Kotlin/Java source file is registered for this module. Use the build model and README as its public surface.

## Patterns {#patterns}

The README evidence is organized around **Examples**, **Virtual Thread Usage Rules**, **Key Learning Points**, **Rule 2: Choosing How to Run Synchronous Code**, **Rule 3: Never Pool Virtual Threads**, **Rule 4: Control Concurrency with Semaphore**, **Rule 5: ThreadLocal Caution**, **Rule 6: Synchronized Block Caution**, **How to Run**, and **Requirements**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(project(":bluetape4k-core"))
runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-examples-virtualthreads-demo:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractVirtualThreadTest`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/AbstractVirtualThreadTest.kt)
- [`Example1_PlatformAndVirtualThread`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example1_PlatformAndVirtualThread.kt)
- [`Example2_PlatformAndVirtualThreadBuilder`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example2_PlatformAndVirtualThreadBuilder.kt)
- [`Example3_CreateStartedAndUnstartedVirtualThread`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example3_CreateStartedAndUnstartedVirtualThread.kt)
- [`Example4_VirtualThreadFactory`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example4_VirtualThreadFactory.kt)
- [`Example5_VirtualThreadPerTaskExecutor`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example5_VirtualThreadPerTaskExecutor.kt)
- [`Rule2RunBlockingSynchronousCode`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part2/Rule2RunBlockingSynchronousCode.kt)
- [`Rule3DoNotPooledVirtualThreads`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part2/Rule3DoNotPooledVirtualThreads.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Virtual threads demo decision map

[![Virtual threads demo decision map](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-virtualthreads-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-virtualthreads-demo-diagram-01.svg)

_Release README: [`examples/virtualthreads-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/virtualthreads-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/virtualthreads-demo/README.md)
- [Module build](../../../../examples/virtualthreads-demo/build.gradle.kts)
- [`AbstractVirtualThreadTest`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/AbstractVirtualThreadTest.kt)
- [`Example1_PlatformAndVirtualThread`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example1_PlatformAndVirtualThread.kt)
- [`Example2_PlatformAndVirtualThreadBuilder`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example2_PlatformAndVirtualThreadBuilder.kt)
- [`Example3_CreateStartedAndUnstartedVirtualThread`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example3_CreateStartedAndUnstartedVirtualThread.kt)
- [`Example4_VirtualThreadFactory`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example4_VirtualThreadFactory.kt)
- [`Example5_VirtualThreadPerTaskExecutor`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part1/Example5_VirtualThreadPerTaskExecutor.kt)
- [`Rule2RunBlockingSynchronousCode`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part2/Rule2RunBlockingSynchronousCode.kt)
- [`Rule3DoNotPooledVirtualThreads`](../../../../examples/virtualthreads-demo/src/test/kotlin/io/bluetape4k/examples/virtualthreads/part2/Rule3DoNotPooledVirtualThreads.kt)
