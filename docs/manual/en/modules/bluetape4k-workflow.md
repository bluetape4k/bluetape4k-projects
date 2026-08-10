---
manualId: bluetape4k-workflow
title: "Workflow Engine"
description: "A Kotlin DSL-based workflow orchestration library with support for sync, coroutine-based, and Virtual Thread execution models. Define complex workflows declaratively using composable flow builders."
kind: library
group: utilities
learningOrder: 1300
---

# Workflow Engine

## Problem {#problem}

A Kotlin DSL-based workflow orchestration library with support for sync, coroutine-based, and Virtual Thread execution models. Define complex workflows declaratively using composable flow builders. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-workflow` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-workflow")
}
```

Gradle project path: `:bluetape4k-workflow`. Source directory: `utils/workflow`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ErrorStrategy`, `NamedSuspendWork`, `NamedWork`, `ParallelPolicy`, `RetryPolicy`, `SuspendWork`, `SuspendWorkFlow`, and `Work`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ErrorStrategy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/ErrorStrategy.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ErrorStrategy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/ErrorStrategy.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`NamedSuspendWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/NamedSuspendWork.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`NamedWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/NamedWork.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ParallelPolicy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/ParallelPolicy.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`RetryPolicy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/RetryPolicy.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/SuspendWork.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendWorkFlow`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/SuspendWorkFlow.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`Work`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/Work.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`WorkAdapters`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/WorkAdapters.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`WorkContext`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/WorkContext.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Concept Overview**, **WorkReport States**, **Execution Model**, **Key Features**, **WorkStatus & WorkReport**, **Control Flow Analogy**, **Core API**, **WorkContext**, and **Work & SuspendWork**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
implementation(project(":bluetape4k-virtualthread-api"))
runtimeOnly(project(":bluetape4k-virtualthread-jdk25"))
implementation(project(":bluetape4k-coroutines"))
implementation(libs.kotlinx.coroutines.core)
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
./gradlew :bluetape4k-workflow:test --no-configuration-cache
```

Representative test anchors:

- [`AbstractWorkflowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/AbstractWorkflowTest.kt)
- [`WorkAdapterTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/WorkAdapterTest.kt)
- [`WorkContextTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/WorkContextTest.kt)
- [`WorkReportTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/WorkReportTest.kt)
- [`ConditionalWorkFlowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/core/ConditionalWorkFlowTest.kt)
- [`ParallelWorkFlowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/core/ParallelWorkFlowTest.kt)
- [`RepeatWorkFlowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/core/RepeatWorkFlowTest.kt)
- [`RetryWorkFlowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/core/RetryWorkFlowTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Concept Overview diagram

[![Concept Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-01.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### WorkReport States diagram

[![WorkReport States diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-02.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Execution Model diagram

[![Execution Model diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-03.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Sequential Flow diagram

[![Sequential Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-04.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Parallel Flow diagram

[![Parallel Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-05.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Conditional Flow diagram

[![Conditional Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-06.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Repeat Flow diagram

[![Repeat Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-07.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-07.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

### Retry Flow diagram

[![Retry Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-08.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-workflow-diagram-08.svg)

_Release README: [`utils/workflow/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/workflow/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/workflow/README.md)
- [Module build](../../../../utils/workflow/build.gradle.kts)
- [`ErrorStrategy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/ErrorStrategy.kt)
- [`NamedSuspendWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/NamedSuspendWork.kt)
- [`NamedWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/NamedWork.kt)
- [`ParallelPolicy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/ParallelPolicy.kt)
- [`RetryPolicy`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/RetryPolicy.kt)
- [`SuspendWork`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/SuspendWork.kt)
- [`SuspendWorkFlow`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/SuspendWorkFlow.kt)
- [`Work`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/Work.kt)
- [`WorkAdapters`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/WorkAdapters.kt)
- [`WorkContext`](../../../../utils/workflow/src/main/kotlin/io/bluetape4k/workflow/api/WorkContext.kt)
- [`AbstractWorkflowTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/AbstractWorkflowTest.kt)
- [`WorkAdapterTest`](../../../../utils/workflow/src/test/kotlin/io/bluetape4k/workflow/api/WorkAdapterTest.kt)
