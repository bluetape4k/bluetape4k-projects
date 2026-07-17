---
manualId: web-framework-benchmark
title: "Web Framework Benchmark"
description: "This module compares equivalent embedded HTTP workloads implemented with Ktor CIO and Spring WebFlux."
kind: benchmark
group: examples
learningOrder: 1520
---

# Web Framework Benchmark

## Problem {#problem}

This module compares equivalent embedded HTTP workloads implemented with Ktor CIO and Spring WebFlux. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `web-framework-benchmark` when the application needs the hypothesis, benchmark task, environment, metric direction, and comparison limits. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This benchmark project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:web-framework-benchmark`. Source directory: `benchmark/web-framework-benchmark`.

## Concepts {#concepts}

The module is configuration or platform metadata and has no Kotlin/Java source type to index.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :web-framework-benchmark:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

No Kotlin/Java source file is registered for this module. Use the build model and README as its public surface.

## Patterns {#patterns}

The README evidence is organized around **What It Measures**, **Latest Local Results**, **Throughput**, **Latency**, **Startup**, and **Run**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The module build declares no direct `api`, `implementation`, `compileOnly`, or `runtimeOnly` dependency line. Inspect plugins and generated metadata in the build file.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Record environment and raw results; compare only equivalent runs and watch variance and allocation. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :web-framework-benchmark:test --no-configuration-cache
```

No Kotlin/Java test file was found in the manifest's test paths. Verify the module build and add a focused contract test when adopting behavior not covered elsewhere.

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../benchmark/web-framework-benchmark/README.md)
- [Module build](../../../../benchmark/web-framework-benchmark/build.gradle.kts)
