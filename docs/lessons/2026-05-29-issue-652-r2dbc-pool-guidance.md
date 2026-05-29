# Issue 652 R2DBC Pool Guidance

## Context

R2DBC pool benchmark scores can be misleading when bounded pending acquire makes
overload fail quickly. A high JMH operation score can represent rejected acquire
attempts rather than successful SQL work.

## Decision

Report acquired and failed counts beside throughput for pool contention
benchmarks. Keep production defaults unchanged unless benchmark evidence and
compatibility review justify a runtime contract change.

## Outcome

H2, PostgreSQL, and MySQL acquire benchmarks completed sequentially. H2
contention now compares default and high-throughput profiles and makes bounded
queue overload visible through failure counts.

## Verification

- `./gradlew :bluetape4k-r2dbc:compileBenchmarkKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-r2dbc:benchmarkH2PoolContention --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkPoolConfig --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkH2PoolAcquire --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkPostgresPoolAcquire --no-configuration-cache --quiet`
- `./gradlew :bluetape4k-r2dbc:benchmarkMysql8PoolAcquire --no-configuration-cache --quiet`

## Future Guidance

Run Testcontainers-backed R2DBC benchmarks sequentially. When a benchmark has
bounded pending acquire, publish success/failure counts with the JMH score so
readers do not treat fast rejection as completed database throughput.
