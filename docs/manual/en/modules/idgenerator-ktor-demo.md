---
manualId: idgenerator-ktor-demo
title: "Distributed ID Generation with Ktor"
description: "Runnable Ktor application that exposes bluetape4k idgenerators through HTTP endpoints."
kind: example
group: examples
learningOrder: 1470
---

# Distributed ID Generation with Ktor

## Problem {#problem}

Runnable Ktor application that exposes bluetape4k idgenerators through HTTP endpoints. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `idgenerator-ktor-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:idgenerator-ktor-demo`. Source directory: `examples/ktor/idgenerator-ktor-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `IdGeneratorKtorApplication`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :idgenerator-ktor-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`IdGeneratorKtorApplication`](../../../../examples/ktor/idgenerator-ktor-demo/src/main/kotlin/io/bluetape4k/examples/ktor/idgenerator/IdGeneratorKtorApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Run**, **Test**, **Endpoints**, **Explicit Routes**, **Explicit Batch Routes**, **Generic Routes**, **Metadata**, and **Generator Choice**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(project(":bluetape4k-idgenerators"))
implementation(project(":bluetape4k-ktor-core"))
implementation(project(":bluetape4k-ktor-observability"))
implementation(libs.ktor.server.core)
implementation(libs.ktor.server.cio)
runtimeOnly(libs.logback.classic)
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
./gradlew :idgenerator-ktor-demo:test --no-configuration-cache
```

Representative test anchors:

- [`IdGeneratorKtorApplicationTest`](../../../../examples/ktor/idgenerator-ktor-demo/src/test/kotlin/io/bluetape4k/examples/ktor/idgenerator/IdGeneratorKtorApplicationTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### idgenerator ktor demo Architecture diagram

[![idgenerator ktor demo Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-idgenerator-ktor-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-ktor-idgenerator-ktor-demo-diagram-01.svg)

_Release README: [`examples/ktor/idgenerator-ktor-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/ktor/idgenerator-ktor-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/ktor/idgenerator-ktor-demo/README.md)
- [Module build](../../../../examples/ktor/idgenerator-ktor-demo/build.gradle.kts)
- [`IdGeneratorKtorApplication`](../../../../examples/ktor/idgenerator-ktor-demo/src/main/kotlin/io/bluetape4k/examples/ktor/idgenerator/IdGeneratorKtorApplication.kt)
- [`IdGeneratorKtorApplicationTest`](../../../../examples/ktor/idgenerator-ktor-demo/src/test/kotlin/io/bluetape4k/examples/ktor/idgenerator/IdGeneratorKtorApplicationTest.kt)
