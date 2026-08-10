---
manualId: bluetape4k-junit5
title: "JUnit 5 Test Support"
description: "An extension library that reduces repetitive boilerplate in JUnit 5 tests."
kind: library
group: testing
learningOrder: 1100
---

# JUnit 5 Test Support

## Problem {#problem}

An extension library that reduces repetitive boilerplate in JUnit 5 tests. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-junit5` when the application needs fixture ownership, isolation, deterministic cleanup, and failure diagnostics. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-junit5")
}
```

Gradle project path: `:bluetape4k-junit5`. Source directory: `testing/junit5`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ExtensionContext`, `AwaitilityConfigurationExtension`, `AwaitilityCoroutines`, `MultithreadingTester`, `StructuredTaskScopeTester`, `TestingExecutors`, `CancellationContracts`, and `CoroutineSupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AwaitilityConfigurationExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityConfigurationExtension.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`AwaitilityCoroutines`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutines.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MultithreadingTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTester.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`StructuredTaskScopeTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTester.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`TestingExecutors`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/TestingExecutors.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CancellationContracts`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CoroutineSupport`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`SuspendedJobTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTester.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FakeValueExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/faker/FakeValueExtension.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Extension Component Overview**, **Class Diagram**, **Key Features**, **Usage Examples**, **StopwatchExtension**, **TempFolderExtension**, **OutputCapture**, **FakeValue / Fakers**, and **Stress Testing**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.junit.bom))
api(project(":bluetape4k-logging"))
api(project(":bluetape4k-virtualthread-api"))
testRuntimeOnly(project(":bluetape4k-virtualthread-jdk21"))
api(libs.kotlin.test.junit5)
api(libs.junit.jupiter)
api(libs.junit.jupiter.engine)
api(libs.junit.jupiter.params)
api(libs.junit.platform.launcher)
api(project(":bluetape4k-assertions"))
api(libs.mockk)
api(libs.awaitility.kotlin)
```

The module's own tests use the JDK 21 provider, but published consumers must add the
`bluetape4k-virtualthread-jdk21` or `bluetape4k-virtualthread-jdk25` provider that
matches their runtime JDK. The JUnit 5 helper must not force a provider on consumers.
Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`org.junit.jupiter.api.extension.Extension`](../../../../testing/junit5/src/main/resources/META-INF/services/org.junit.jupiter.api.extension.Extension)
- [`org.junit.platform.launcher.TestExecutionListener`](../../../../testing/junit5/src/main/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Keep fixtures isolated, bound resource use, expose diagnostics, and close shared services deterministically. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-junit5:test --no-configuration-cache
```

Representative test anchors:

- [`ExtensionContextSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/ExtensionContextSupportTest.kt)
- [`AwaitilityCoroutinesTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutinesTest.kt)
- [`MultithreadingTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTesterTest.kt)
- [`StructuredTaskScopeTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTesterTest.kt)
- [`CancellationContractsTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/CancellationContractsTest.kt)
- [`CoroutineSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupportTest.kt)
- [`SuspendedJobTesterTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTesterTest.kt)
- [`DataFakerExamples`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/faker/DataFakerExamples.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Extension Component Overview diagram

[![Extension Component Overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-01.svg)

_Release README: [`testing/junit5/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.md)_

### JUnit5 Class Structure diagram

[![JUnit5 Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-02.svg)

_Release README: [`testing/junit5/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.md)_

### Mermaid report sequence

[![Mermaid report sequence](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/testing-junit5-diagram-03.svg)

_Release README: [`testing/junit5/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/testing/junit5/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../testing/junit5/README.md)
- [Module build](../../../../testing/junit5/build.gradle.kts)
- [`ExtensionContext`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/ExtensionContext.kt)
- [`AwaitilityConfigurationExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityConfigurationExtension.kt)
- [`AwaitilityCoroutines`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutines.kt)
- [`MultithreadingTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/MultithreadingTester.kt)
- [`StructuredTaskScopeTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/StructuredTaskScopeTester.kt)
- [`TestingExecutors`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/concurrency/TestingExecutors.kt)
- [`CancellationContracts`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CancellationContracts.kt)
- [`CoroutineSupport`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/CoroutineSupport.kt)
- [`SuspendedJobTester`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/coroutines/SuspendedJobTester.kt)
- [`FakeValueExtension`](../../../../testing/junit5/src/main/kotlin/io/bluetape4k/junit5/faker/FakeValueExtension.kt)
- [`ExtensionContextSupportTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/ExtensionContextSupportTest.kt)
- [`AwaitilityCoroutinesTest`](../../../../testing/junit5/src/test/kotlin/io/bluetape4k/junit5/awaitility/AwaitilityCoroutinesTest.kt)
