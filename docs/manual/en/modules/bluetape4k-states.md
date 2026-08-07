---
manualId: bluetape4k-states
title: "State Machine Utilities"
description: "A Kotlin DSL-based finite state machine (FSM) library for JVM backend and library code."
kind: library
group: utilities
learningOrder: 1290
---

# State Machine Utilities

## Problem {#problem}

A Kotlin DSL-based finite state machine (FSM) library for JVM backend and library code. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-states` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-states")
}
```

Gradle project path: `:bluetape4k-states`. Source directory: `utils/states`.

## Concepts {#concepts}

The first source-level concepts to inspect are `BaseStateMachine`, `StateMachine`, `StateMachineException`, `SuspendStateMachineInterface`, `TransitionResult`, `DefaultStateMachine`, `ParentTransitionKey`, and `StateMachineDsl`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`BaseStateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/BaseStateMachine.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`BaseStateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/BaseStateMachine.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/StateMachine.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StateMachineException`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/StateMachineException.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendStateMachineInterface`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/SuspendStateMachineInterface.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TransitionResult`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/TransitionResult.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`DefaultStateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/DefaultStateMachine.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`ParentTransitionKey`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/ParentTransitionKey.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StateMachineDsl`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/StateMachineDsl.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TransitionKey`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/TransitionKey.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TransitionRegistry`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/TransitionRegistry.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Concept Overview**, **Class Diagram**, **DSL Builder Structure**, **Key Features**, **Module Positioning**, **Example State Diagrams**, **1. Turnstile — Simple FSM**, **2. Order — One-Way FSM**, and **3. Appointment — Complex FSM (clinic-appointment)**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
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
./gradlew :bluetape4k-states:test --no-configuration-cache
```

Representative test anchors:

- [`DefaultStateMachineTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/DefaultStateMachineTest.kt)
- [`GuardedTransitionTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/GuardedTransitionTest.kt)
- [`NestedStateMachineDslTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/NestedStateMachineDslTest.kt)
- [`StateMachineDslTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/StateMachineDslTest.kt)
- [`SuspendStateMachineTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/coroutines/SuspendStateMachineTest.kt)
- [`AppointmentExampleTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/examples/AppointmentExampleTest.kt)
- [`OrderExampleTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/examples/OrderExampleTest.kt)
- [`TurnstileExampleTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/examples/TurnstileExampleTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Concept Overview diagram

[![Concept Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-01.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### States Class Structure diagram

[![States Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-02.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### DSL Builder Structure diagram

[![DSL Builder Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-03.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### 1. Turnstile — Simple FSM diagram

[![1. Turnstile — Simple FSM diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-04.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### 2. Order — One-Way FSM diagram

[![2. Order — One-Way FSM diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-05.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-05.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### 3. Appointment — Complex FSM (clinic-appointment) diagram

[![3. Appointment — Complex FSM (clinic-appointment) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-06.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-diagram-06.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### Synchronous FSM Transition Flow diagram

[![Synchronous FSM Transition Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-sequence-01.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

### Coroutine FSM Transition Flow (SuspendStateMachine) diagram

[![Coroutine FSM Transition Flow (SuspendStateMachine) diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-sequence-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-states-sequence-02.svg)

_Release README: [`utils/states/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/states/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/states/README.md)
- [Module build](../../../../utils/states/build.gradle.kts)
- [`BaseStateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/BaseStateMachine.kt)
- [`StateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/StateMachine.kt)
- [`StateMachineException`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/StateMachineException.kt)
- [`SuspendStateMachineInterface`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/SuspendStateMachineInterface.kt)
- [`TransitionResult`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/api/TransitionResult.kt)
- [`DefaultStateMachine`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/DefaultStateMachine.kt)
- [`ParentTransitionKey`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/ParentTransitionKey.kt)
- [`StateMachineDsl`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/StateMachineDsl.kt)
- [`TransitionKey`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/TransitionKey.kt)
- [`TransitionRegistry`](../../../../utils/states/src/main/kotlin/io/bluetape4k/states/core/TransitionRegistry.kt)
- [`DefaultStateMachineTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/DefaultStateMachineTest.kt)
- [`GuardedTransitionTest`](../../../../utils/states/src/test/kotlin/io/bluetape4k/states/core/GuardedTransitionTest.kt)
