# Lesson: CPU/GC Profiling for kotlinx-benchmark JMH Benchmarks

**Date**: 2026-05-24  
**Issue**: #585 — perf(io-http): add CPU and GC profiling to HTTP client benchmarks  
**Branch**: perf/http-benchmark-profiling-20260524

---

## Root Cause / Background

kotlinx-benchmark 0.4.x does not expose JMH's `OptionsBuilder.addProfiler()` through its
Gradle DSL. The `BenchmarkConfiguration.advanced()` map is parsed at runtime by
`JvmBenchmarkRunner`, but only the `jvmForks` key is acted upon — all other keys are
written to a parameters file and silently ignored by the runner.

Therefore, JMH built-in profilers (`gc`, `jfr`, `stack`) cannot be activated through the
`advanced()` DSL. The correct approach is to configure JVM args on the generated `JavaExec`
benchmark exec task via `tasks.withType<JavaExec>().configureEach { ... }`.

---

## Decision

Use JVM-level profiling flags injected via Gradle task configuration:

| Mode | JVM flag |
|------|----------|
| `gc` | `-Xlog:gc*,safepoint:file=...` |
| `jfr` | `-XX:StartFlightRecording=filename=...,dumponexit=true,settings=profile` |
| `async` | `-agentpath:libasyncProfiler.so=start,event=cpu,file=...,flamegraph` |

Activated by `-PbenchmarkProfile=<gc|jfr|async>` Gradle property.

---

## Critical Pitfall: kotlinx-benchmark task naming

The kotlinx-benchmark plugin generates the benchmark exec task with name:

```
"${target.name}${config.capitalizedName()}${BENCHMARK_EXEC_SUFFIX}"
```

where:
- `capitalizedName()` returns `""` when config name is `"main"` (the default)
- `BENCHMARK_EXEC_SUFFIX = "Benchmark"`

So for `targets { register("test") }` + default `configurations { named("main") }`:
- **Task name = `"testBenchmark"`** (NOT `"testMainBenchmarkExec"`)

A filter of `name.endsWith("Exec")` will never match and the profiling block becomes a
silent no-op. The correct filter is `name.endsWith("Benchmark")` combined with
`tasks.withType<JavaExec>()` (to exclude lifecycle DefaultTask instances also named
with "Benchmark").

---

## Verification Evidence

- Build script compiles cleanly with and without `-PbenchmarkProfile=gc`
- `./gradlew :bluetape4k-http:tasks --all` confirms `testBenchmark` is the JavaExec exec task
- 8 existing smoke tests pass: `HttpClientBenchmarkTest` — 8 tests, 0 failures

---

## Future Guidance

- Always verify the actual task name via `./gradlew :module:tasks --all` before writing
  task name filters in `configureEach { }` blocks.
- If kotlinx-benchmark adds native profiler support in a future release, migrate to that
  DSL and remove the `tasks.withType<JavaExec>` workaround.
- For JFR output: open `build/benchmark-profiling/benchmark.jfr` with JDK Mission Control
  (`jmc`) or IntelliJ IDEA's JFR viewer.
- async-profiler requires the native agent library; kotlinx-benchmark runtime auto-detects
  `libasyncProfiler` in JVM args and sets forks=0 (needed for in-process profiling).
