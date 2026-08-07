---
manualId: observability-spring-boot-demo
title: "Spring Boot Observability Example"
description: "Runnable Spring Boot 4 application that shows how to use bluetape4k observation helpers with Spring Boot Actuator Prometheus metrics and application-owned OTLP tracing configuration."
kind: example
group: examples
learningOrder: 1500
---

# Spring Boot Observability Example

## Problem {#problem}

Runnable Spring Boot 4 application that shows how to use bluetape4k observation helpers with Spring Boot Actuator Prometheus metrics and application-owned OTLP tracing configuration. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `observability-spring-boot-demo` when the application needs the runnable entry point, required services, expected behavior, and the production pattern demonstrated. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This example project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:observability-spring-boot-demo`. Source directory: `examples/spring-boot/observability-spring-boot-demo`.

## Concepts {#concepts}

The first source-level concepts to inspect are `ObservabilitySpringBootDemoApplication`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :observability-spring-boot-demo:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`ObservabilitySpringBootDemoApplication`](../../../../examples/spring-boot/observability-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/spring/observability/ObservabilitySpringBootDemoApplication.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Example Scenario**, **Architecture**, **Sequence Diagram**, **Dependencies**, **Configuration**, **Run**, **Verify**, and **Test**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-micrometer"))
implementation(project(":bluetape4k-spring-boot-core"))
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-actuator")
runtimeOnly(libs.micrometer.registry.prometheus)
runtimeOnly(libs.micrometer.tracing.bridge.otel)
runtimeOnly(libs.opentelemetry.exporter.otlp)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

Configuration resources found in the module:

- [`application.yaml`](../../../../examples/spring-boot/observability-spring-boot-demo/src/main/resources/application.yaml)

Read property names and defaults from these resources and the binding source before overriding them.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Run the example in an isolated environment and observe startup, dependency health, requests, and shutdown. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :observability-spring-boot-demo:test --no-configuration-cache
```

Representative test anchors:

- [`ObservabilitySpringBootDemoApplicationTest`](../../../../examples/spring-boot/observability-spring-boot-demo/src/test/kotlin/io/bluetape4k/examples/spring/observability/ObservabilitySpringBootDemoApplicationTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Spring Boot observability demo architecture

[![Spring Boot observability demo architecture](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-architecture-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-architecture-01.svg)

_Release README: [`examples/spring-boot/observability-spring-boot-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/spring-boot/observability-spring-boot-demo/README.md)_

### Spring Boot observability demo sequence

[![Spring Boot observability demo sequence](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/examples-spring-boot-observability-spring-boot-demo-sequence-01.svg)

_Release README: [`examples/spring-boot/observability-spring-boot-demo/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/examples/spring-boot/observability-spring-boot-demo/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../examples/spring-boot/observability-spring-boot-demo/README.md)
- [Module build](../../../../examples/spring-boot/observability-spring-boot-demo/build.gradle.kts)
- [`ObservabilitySpringBootDemoApplication`](../../../../examples/spring-boot/observability-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/spring/observability/ObservabilitySpringBootDemoApplication.kt)
- [`ObservabilitySpringBootDemoApplicationTest`](../../../../examples/spring-boot/observability-spring-boot-demo/src/test/kotlin/io/bluetape4k/examples/spring/observability/ObservabilitySpringBootDemoApplicationTest.kt)
