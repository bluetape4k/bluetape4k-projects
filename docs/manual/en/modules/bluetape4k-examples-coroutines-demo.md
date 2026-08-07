---
manualId: bluetape4k-examples-coroutines-demo
title: "Kotlin Coroutine Examples"
description: "A collection of examples for learning the features and usage patterns of Kotlin Coroutines."
kind: example
group: examples
learningOrder: 1400
---

# Kotlin Coroutine Examples

## Problem {#problem}

A collection of examples for learning the features and usage patterns of Kotlin Coroutines. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-examples-coroutines-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:bluetape4k-examples-coroutines-demo`. Source directory: `examples/coroutines-demo`.

## Concepts {#concepts}

The module is configuration or platform metadata and has no Kotlin/Java source type to index.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

No Kotlin/Java source file is registered for this module. Use the build model and README as its public surface.

## Patterns {#patterns}

The README evidence is organized around **Examples**, **Basics (guide/)**, **Flow Examples (flow/)**, **Channel Examples (channels/)**, **Cancellation (cancellation/)**, **Coroutine Context (context/)**, **Builders (builders/)**, **Dispatchers (dispatchers/)**, **Exception Handling (exceptions/)**, and **Scope (scope/)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The module build declares no direct `api`, `implementation`, `compileOnly`, or `runtimeOnly` dependency line. Inspect plugins and generated metadata in the build file.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-examples-coroutines-demo:test --no-configuration-cache
```

Representative test anchors:

- [`TestSupport`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/TestSupport.kt)
- [`CoroutineBuilderExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/builders/CoroutineBuilderExamples.kt)
- [`CoroutineContextBuilderExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/builders/CoroutineContextBuilderExamples.kt)
- [`CancellationExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/cancellation/CancellationExamples.kt)
- [`ActorExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/channels/ActorExamples.kt)
- [`ChannelExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/channels/ChannelExamples.kt)
- [`MutexExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/concurrency/MutexExamples.kt)
- [`CoroutineContextExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/context/CoroutineContextExamples.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Coroutines demo learning map

[![Coroutines demo learning map](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-coroutines-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-coroutines-demo-diagram-01.svg)

_Release README: [`examples/coroutines-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/coroutines-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/coroutines-demo/README.md)
- [Module build](../../../../examples/coroutines-demo/build.gradle.kts)
- [`TestSupport`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/TestSupport.kt)
- [`CoroutineBuilderExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/builders/CoroutineBuilderExamples.kt)
- [`CoroutineContextBuilderExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/builders/CoroutineContextBuilderExamples.kt)
- [`CancellationExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/cancellation/CancellationExamples.kt)
- [`ActorExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/channels/ActorExamples.kt)
- [`ChannelExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/channels/ChannelExamples.kt)
- [`MutexExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/concurrency/MutexExamples.kt)
- [`CoroutineContextExamples`](../../../../examples/coroutines-demo/src/test/kotlin/io/bluetape4k/examples/coroutines/context/CoroutineContextExamples.kt)
