# Ktor CIO vs Spring WebFlux Benchmark - 2026-05-29

## Scope

Issue: #667

Module: `:web-framework-benchmark`

This benchmark compares equivalent in-memory ID generator HTTP APIs implemented
with Ktor CIO and Spring Boot WebFlux/Netty. The workload intentionally mirrors
the existing `idgenerator` example domain so the result can guide the Ktor module
family design in #609 without adding benchmark-only code to production modules.

## Commands

```bash
./gradlew :web-framework-benchmark:compileBenchmarkKotlin --no-configuration-cache
./gradlew :web-framework-benchmark:benchmarkStartupBenchmark --no-configuration-cache
./gradlew :web-framework-benchmark:benchmarkThroughputBenchmark :web-framework-benchmark:benchmarkLatencyBenchmark --no-configuration-cache
./scripts/web-framework-benchmark.sh
```

## Run Conditions

| Field | Value |
|---|---|
| Date | 2026-05-29 |
| OS | macOS Darwin 25.5.0 arm64 |
| CPU | Apple M4 Pro, 12 logical CPUs |
| Memory | 48 GiB |
| JDK | Oracle GraalVM 21.0.11 |
| Kotlin | 2.3.21 |
| Spring Boot | 4.0.6 |
| Ktor | 3.5.0 |
| Serialization | Ktor kotlinx.serialization JSON, Spring Jackson 3 Kotlin module |
| Client | JDK `HttpClient`, blocking round trip per operation |
| JMH shape | fork=1, warmup=2x1s, measurement=3x1s for request benchmarks |

Raw artifacts:

- `docs/benchmarks/raw/2026-05-29-web-framework-startup.json`
- `docs/benchmarks/raw/2026-05-29-web-framework-throughput.json`
- `docs/benchmarks/raw/2026-05-29-web-framework-latency.json`

## Startup Snapshot

Lower is better.

| Benchmark | ms/op |
|---|---:|
| `ktorStartup` | 0.741 |
| `springWebFluxStartup` | 2,087.926 |

The Ktor startup number measures embedded CIO binding and connector resolution
only. The Spring number includes Spring Boot reactive application context
creation and Netty binding. Treat this as a framework bootstrap shape, not an
end-user cold-start SLA.

## Throughput

Higher is better.

| Benchmark | ops/s |
|---|---:|
| `ktorBadRequest` | 4,145.680 |
| `ktorBatchIds` | 3,726.603 |
| `ktorHealth` | 3,797.255 |
| `ktorSingleId` | 3,952.156 |
| `springWebFluxBadRequest` | 6,045.469 |
| `springWebFluxBatchIds` | 5,807.421 |
| `springWebFluxHealth` | 4,418.503 |
| `springWebFluxSingleId` | 5,625.438 |

## Average Latency

Lower is better.

| Benchmark | us/op |
|---|---:|
| `ktorBadRequest` | 245.501 |
| `ktorBatchIds` | 288.347 |
| `ktorHealth` | 281.856 |
| `ktorSingleId` | 256.205 |
| `springWebFluxBadRequest` | 162.982 |
| `springWebFluxBatchIds` | 164.854 |
| `springWebFluxHealth` | 166.158 |
| `springWebFluxSingleId` | 166.324 |

## Interpretation

- Ktor CIO starts much faster in this embedded benchmark because it avoids Spring
  Boot context creation.
- Spring WebFlux is faster for steady-state request throughput and average
  request latency in this short-window local fixture.
- The current Ktor library design should stay thin and explicit: keep shared
  domain behavior in core modules, and add Ktor helpers only where they remove
  repeated integration code.
- Do not choose a Ktor server abstraction only because startup is fast. For
  request-path helpers, compare handler ergonomics, cancellation behavior, and
  steady-state performance together.

## Follow-Up Issues

- #643 should first decide whether Ktor client support belongs in `io/http`,
  `ktor/client`, or a thin bridge.
- #644 should keep Resilience4j helpers coroutine-safe and benchmark timeout /
  retry overhead after route helpers exist.
- #645 should remain documentation/metadata focused until route contracts settle.
- A future benchmark can add p50/p95/p99 histograms with an external load tool
  such as `wrk`, `oha`, or Gatling. The current JMH fixture records throughput
  and average latency, not percentile latency.
