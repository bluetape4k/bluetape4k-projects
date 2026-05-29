# Lesson: Web Framework Benchmark Baseline

**Date**: 2026-05-29
**Issue**: #667

## Context

The Ktor module family needed current evidence before broad API work in #609.

## Decision

Add a non-published `benchmark/web-framework-benchmark` module using
`kotlinx-benchmark`. Keep equivalent Ktor CIO and Spring WebFlux benchmark
applications inside the benchmark module, not production modules.

## Outcome

The benchmark now covers startup, throughput, and average latency for equivalent
health, single-ID, batch-ID, and bad-request paths.

## Verification

- `./gradlew :web-framework-benchmark:compileBenchmarkKotlin --no-configuration-cache`
- `./gradlew :web-framework-benchmark:benchmarkStartupBenchmark --no-configuration-cache`
- `./gradlew :web-framework-benchmark:benchmarkThroughputBenchmark :web-framework-benchmark:benchmarkLatencyBenchmark --no-configuration-cache`

## Future Guard

Use `server.port=0` and read actual bound ports from the framework runtime.
Preselecting a local port with `ServerSocket(0)` can race during repeated JMH
startup iterations.
