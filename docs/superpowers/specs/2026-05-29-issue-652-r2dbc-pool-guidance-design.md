# Issue 652 R2DBC Pool Guidance Design

## Context

Issue #652 extends the R2DBC pool benchmark work from PR #98. The existing
benchmarks covered default and high-throughput acquire paths, plus H2
contention by pool size. The missing piece was clearer result reporting for
profile comparison and practical tuning guidance that distinguishes successful
acquisition throughput from fast overload rejection.

## Decision

Keep production defaults unchanged. Extend benchmark reporting instead:

- Acquire benchmarks print database, profile, hold time, pool settings, and
  acquired/failed counts.
- H2 contention benchmarks now run both `default` and `highThroughput`
  profiles.
- The contention high-throughput profile uses bounded pending acquire and a
  short `250 ms` acquire timeout so overload appears as failure counts instead
  of an unbounded queue.
- README charts and tables report both JMH operation throughput and
  acquired/failed counts for contention rows.

## Measurement

Local run on 2026-05-29, Java 21, one sequential benchmark lane:

| Benchmark task | Result |
|---|---|
| `benchmarkPoolConfig` | passed |
| `benchmarkH2PoolAcquire` | passed |
| `benchmarkH2PoolContention` | passed |
| `benchmarkPostgresPoolAcquire` | passed |
| `benchmarkMysql8PoolAcquire` | passed |

No Testcontainers environment failure occurred in the PostgreSQL/MySQL lane.

## Compatibility

No runtime defaults or `ConnectionPoolConfiguration` conversion behavior changed.
Therefore no new conversion tests were needed beyond compiling the benchmark
source and running the existing targeted pool tests.
