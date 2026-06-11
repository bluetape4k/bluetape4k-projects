# Lessons - Issue #746 Same-Condition Compressor Benchmark

## Context

Issue #746 needed a durable `bluetape4k-io` compressor-only matrix that can be compared with `bluetape-go` and `bluetape-rs`.

## Lessons

- Keep the normalized table to compressor families that exist across the target ecosystems. BZip2 is useful JVM context, but it should not be mixed into the common table.
- For `kotlinx-benchmark`, verify generated task names before documenting commands. This module exposes `testBenchmark`, `testBenchmarkCompile`, and `testBenchmarkJar`.
- `testBenchmark --args` is not a JMH include-filter escape hatch in this setup; it treats the first argument as a kotlinx runner input file. For focused smoke runs, generated JMH jar execution is acceptable when documented as an exception.
- Benchmark fat jars can include signed dependency metadata. Exclude `META-INF/*.RSA`, `META-INF/*.DSA`, and `META-INF/*.SF` from benchmark jar tasks so direct JMH execution does not fail signature verification.

## Guard

When adding benchmark harnesses, prove both the Gradle-generated benchmark compile path and at least one runnable benchmark command. If the runnable path needs direct JMH jar execution, record why the Gradle task is insufficient and keep the direct run narrow.
