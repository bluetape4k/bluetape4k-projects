# Issue 652 R2DBC Pool Guidance Plan

## Steps

1. Inspect prior PR #98, current benchmark classes, pool config conversion, and
   README guidance.
2. Extend benchmark result reporting without changing production pool defaults.
3. Add default versus high-throughput H2 contention coverage with explicit
   acquired/failed counters.
4. Run compile, targeted pool tests, H2 acquire/contention, and sequential
   PostgreSQL/MySQL Testcontainers acquire benchmarks.
5. Update README tables, charts, and issue evidence with benchmark results.
6. Capture the overload interpretation rule in a short lesson.
