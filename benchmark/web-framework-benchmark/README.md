# Web Framework Benchmark

This module compares equivalent embedded HTTP workloads implemented with Ktor CIO and Spring WebFlux. The benchmark uses JDK `HttpClient` against local loopback servers so the comparison includes framework routing, JSON response serialization, request handling, and minimal server lifecycle cost.

![Web framework throughput](../../docs/images/readme-charts/benchmark-web-framework-throughput-chart-01.png)

![Web framework latency](../../docs/images/readme-charts/benchmark-web-framework-latency-chart-01.png)

## What It Measures

The benchmark source is
`io.bluetape4k.benchmark.webframework.WebFrameworkBenchmark`.

Both frameworks expose the same logical endpoints:

| Endpoint                       | Purpose                                                               |
|--------------------------------|-----------------------------------------------------------------------|
| `/health`                      | Small JSON health response.                                           |
| `/idgen/uuid-v7`               | Generate one UUIDv7 identifier through the shared generator registry. |
| `/idgen/uuid-v7/batch?size=10` | Generate a small batch of UUIDv7 identifiers.                         |
| `/idgen/unknown`               | Exercise the bad-request path for an unknown generator.               |

The generator registry also wires UUIDv4, UUIDv7, ULID, KSUID, Snowflake, and Flake generators so the HTTP layer uses the same ID-generation surface that the library modules expose.

## Latest Local Results

Throughput and latency snapshot: 2026-06-19 on GraalVM JDK 21.0.11. Startup and
memory lifecycle snapshot: 2026-08-03 on GraalVM JDK 21.0.12. Each benchmark uses
one fork and three measured iterations unless the Gradle configuration says otherwise.

### Throughput

Command:

```bash
./gradlew :web-framework-benchmark:throughputBenchmark --rerun-tasks --no-parallel
```

Mode: JMH throughput, `ops/s`, higher is better.

| Endpoint    |    Ktor CIO | Spring WebFlux |
|-------------|------------:|---------------:|
| Health      | 4,006 ops/s |    6,112 ops/s |
| Single ID   | 3,727 ops/s |    6,441 ops/s |
| Batch IDs   | 3,719 ops/s |    6,047 ops/s |
| Bad request | 3,697 ops/s |    6,218 ops/s |

### Latency

Command:

```bash
./gradlew :web-framework-benchmark:latencyBenchmark --rerun-tasks --no-parallel
```

Mode: JMH average time, `us/op`, lower is better.

| Endpoint    |      Ktor CIO | Spring WebFlux |
|-------------|--------------:|---------------:|
| Health      | 267.492 us/op |  172.170 us/op |
| Single ID   | 309.815 us/op |  179.457 us/op |
| Batch IDs   | 274.638 us/op |  167.923 us/op |
| Bad request | 255.166 us/op |  164.028 us/op |

### Ready-to-serve startup

Command:

```bash
./gradlew :web-framework-benchmark:startupBenchmark
```

Mode: JMH average time, `ms/op`, lower is better. The benchmark measures server
construction through connector/application readiness. `@TearDown(Level.Invocation)`
closes each server after the invocation, so shutdown is excluded from this metric.

| Framework      | Benchmark                   | Score | Error |
|----------------|-----------------------------|------:|------:|
| Ktor CIO       | `ktorReadyStartup`          | 0.675 ms/op | ±0.316 |
| Spring WebFlux | `springWebFluxReadyStartup` | 77.821 ms/op | ±146.638 |

### JVM used-heap snapshot

Command:

```bash
./gradlew :web-framework-benchmark:memoryBenchmark
```

The memory benchmark records `jvm.used_heap` after each server is ready and
before invocation teardown. It uses `Runtime.totalMemory() - freeMemory()`, so
the value is JVM used heap rather than process RSS. The generated JMH JSON keeps
the raw event samples; `memory-metrics.json` normalizes them with the explicit
`bytes` unit, sample count, and `after_ready_before_shutdown` sampling point.
The normalized byte table is authoritative; the memory benchmark's primary
`ms/op` result is only the JMH invocation timing for the sampling harness.

| Framework      | Benchmark                      | Average bytes | Samples |
|----------------|--------------------------------|--------------:|--------:|
| Ktor CIO       | `ktorReadyUsedHeap`            |    39,887,232 |       3 |
| Spring WebFlux | `springWebFluxReadyUsedHeap`   |    53,612,200 |       3 |

These numbers are local comparison snapshots, not release guarantees. The throughput and latency runs use short one-second measurement windows, so use longer JMH runs before making a product-level performance claim.

## Run

```bash
./gradlew :web-framework-benchmark:throughputBenchmark
./gradlew :web-framework-benchmark:latencyBenchmark
./gradlew :web-framework-benchmark:startupBenchmark
./gradlew :web-framework-benchmark:memoryBenchmark
```

The raw JMH JSON reports are written under
`benchmark/web-framework-benchmark/build/reports/benchmarks/`. The memory run
also writes `memory-metrics.json` beside its JMH `benchmark.json` report.
