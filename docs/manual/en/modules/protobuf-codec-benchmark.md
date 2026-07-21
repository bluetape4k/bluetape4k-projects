---
manualId: protobuf-codec-benchmark
title: "Protobuf Codec Benchmark"
description: "This module measures the protobuf codec paths used by bluetape4k serialization and Redisson integration code."
kind: benchmark
group: examples
learningOrder: 1510
---

# Protobuf Codec Benchmark

## Problem {#problem}

This module measures the protobuf codec paths used by bluetape4k serialization and Redisson integration code. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `protobuf-codec-benchmark` when the application needs the hypothesis, benchmark task, environment, metric direction, and comparison limits. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

This benchmark project is not published as a Maven artifact. Run it from the repository and inspect its Gradle tasks before choosing a command.

Gradle project path: `:protobuf-codec-benchmark`. Source directory: `benchmark/protobuf-codec-benchmark`.

## Concepts {#concepts}

The module is configuration or platform metadata and has no Kotlin/Java source type to index.

## Quick start {#quick-start}

List the project tasks before running the example or benchmark:

```bash
./gradlew :protobuf-codec-benchmark:tasks --all
```

Then use the command documented by the module README and keep required external services isolated.

## API by task {#api-by-task}

No Kotlin/Java source file is registered for this module. Use the build model and README as its public surface.

## Patterns {#patterns}

The exact 17-cell matrix compares serializer, Redisson, and Lettuce paths. Only retained optimized candidates can make
an allocation claim; baselines, compatibility controls, fallback cells, and rolled-back serializer decode cells are
claim-ineligible. The committed issue #757 report contains two canonical runs. It supports the accepted Lettuce
heap/direct allocation result but does not prove zero-copy or a general throughput improvement.

## Integrations {#integrations}

The module build declares no direct `api`, `implementation`, `compileOnly`, or `runtimeOnly` dependency line. Inspect plugins and generated metadata in the build file.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Build one JMH JAR, pin its hash and file identity, run the canonical profile twice without rebuilding, and compare only
equivalent environments. Publish only evidence accepted by the fail-closed runner and regenerate the report from its
delivery manifest.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :protobuf-codec-benchmark:test --no-configuration-cache
```

No Kotlin/Java test file was found in the manifest's test paths. Verify the module build and add a focused contract test when adopting behavior not covered elsewhere.

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

## Sources {#sources}

- [Module README](../../../../benchmark/protobuf-codec-benchmark/README.md)
- [Module build](../../../../benchmark/protobuf-codec-benchmark/build.gradle.kts)
- [Committed issue #757 report](../../../benchmarks/2026-07-18-protobuf-buffer-allocation.md)
