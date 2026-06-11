# Issue 751 IO Compressor Self-Improve Lessons

## Context

PR #750 added the same-condition compressor benchmark. Issue #751 used that benchmark as a self-improvement target and required at least 30% throughput improvement with `kotlinx-benchmark`.

## What Worked

- A fresh Gradle `kotlinx-benchmark` baseline made small changes easy to reject quickly.
- Reusing the exact same benchmark fixture and sealing the fixture files prevented accidental benchmark drift.
- The first repeated-input LZ4 cache result was rejected because it optimized the benchmark shape instead of actual compression throughput.
- A fair primary metric needs to cover the affected compressor family, not only one repeated small LZ4 payload. The final gate uses the geometric mean for GZip, Deflate, Zstd, and LZ4 across all same-condition payload kinds and sizes.
- Default compression-level changes must be treated as configuration tradeoffs, not implementation optimizations. They improved throughput but were rejected because they changed the default compression-ratio contract.
- After restoring default compression levels, the reviewed candidate did not meet the 30% target.

## Guardrails

- Do not count memoization or content caching as compressor throughput improvement unless the feature is explicitly a cache.
- Do not use `ThreadLocal` state in shared compressor singletons as a performance shortcut; it is a poor fit for virtual-thread-heavy usage.
- Do not claim default compression-level reductions as throughput implementation wins. Preserve default compression-ratio behavior unless the issue explicitly asks for a profile or configuration change.
- Keep benchmark summaries tied to raw `kotlinx-benchmark` JSON and document the aggregation rule.
- Use both `MultithreadingTester` and `StructuredTaskScopeTester` when shared compressor instances are reviewed for platform-thread and virtual-thread concurrency.

## Result

- Baseline: 19,757.605809808367 ops/s
- Reviewed candidate: 19,739.301242587328 ops/s
- Improvement: -0.09264125834021463%
- Gate: FAIL
