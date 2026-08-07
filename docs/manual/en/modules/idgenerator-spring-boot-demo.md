---
manualId: idgenerator-spring-boot-demo
title: "Distributed ID Generation with Spring Boot"
description: "This example shows how to expose bluetape4k-idgenerators through a Spring Boot REST application. The Ktor version is tracked separately in issue #419."
kind: example
group: examples
learningOrder: 1480
---

# Distributed ID Generation with Spring Boot

## Problem {#problem}

This example shows how to expose bluetape4k-idgenerators through a Spring Boot REST application. The Ktor version is tracked separately in issue #419. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `idgenerator-spring-boot-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:idgenerator-spring-boot-demo`. Source directory: `examples/spring-boot/idgenerator-spring-boot-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `IdGeneratorDemoApplication`, `IdGeneratorConfiguration`, `IdGeneratorProperties`, `IdGeneratorController`, `IdGeneratorExceptionHandler`, `IdGeneratorResponses`, `IdGeneratorRegistry`, and `IdGeneratorService`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :idgenerator-spring-boot-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`IdGeneratorDemoApplication`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/IdGeneratorDemoApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorConfiguration`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/config/IdGeneratorConfiguration.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorProperties`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/config/IdGeneratorProperties.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorController`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorController.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorExceptionHandler`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorExceptionHandler.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorResponses`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorResponses.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorRegistry`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/service/IdGeneratorRegistry.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`IdGeneratorService`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/service/IdGeneratorService.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Architecture**, **Configuration**, **Endpoints**, **Usage**, and **Tests**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-idgenerators"))
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation(libs.jackson3.module.kotlin)
implementation(libs.jackson3.module.blackbird)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`application.yaml`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/resources/application.yaml)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :idgenerator-spring-boot-demo:test --no-configuration-cache
```

Representative test anchors:

- [`IdGeneratorDemoApplicationTest`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/test/kotlin/io/bluetape4k/examples/idgenerator/IdGeneratorDemoApplicationTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### idgenerator spring boot demo Architecture diagram

[![idgenerator spring boot demo Architecture diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-idgenerator-spring-boot-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-idgenerator-spring-boot-demo-diagram-01.svg)

_Release README: [`examples/spring-boot/idgenerator-spring-boot-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/spring-boot/idgenerator-spring-boot-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/spring-boot/idgenerator-spring-boot-demo/README.md)
- [Module build](../../../../examples/spring-boot/idgenerator-spring-boot-demo/build.gradle.kts)
- [`IdGeneratorDemoApplication`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/IdGeneratorDemoApplication.kt)
- [`IdGeneratorConfiguration`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/config/IdGeneratorConfiguration.kt)
- [`IdGeneratorProperties`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/config/IdGeneratorProperties.kt)
- [`IdGeneratorController`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorController.kt)
- [`IdGeneratorExceptionHandler`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorExceptionHandler.kt)
- [`IdGeneratorResponses`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/controller/IdGeneratorResponses.kt)
- [`IdGeneratorRegistry`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/service/IdGeneratorRegistry.kt)
- [`IdGeneratorService`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/idgenerator/service/IdGeneratorService.kt)
- [`IdGeneratorDemoApplicationTest`](../../../../examples/spring-boot/idgenerator-spring-boot-demo/src/test/kotlin/io/bluetape4k/examples/idgenerator/IdGeneratorDemoApplicationTest.kt)
