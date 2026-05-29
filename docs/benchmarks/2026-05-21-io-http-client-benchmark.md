# io/http HTTP Client Benchmark - 2026-05-21

## Scope

This report records benchmark-driven work for GitHub epic #589 and sub-issues #590 and #587.
Related existing issues included in the epic: #582, #583, #584, #585, and #586.

Target module: `:bluetape4k-http`.

Raw artifacts: Not retained; this report preserves the reduced JMH tables from
the local run.

Chart artifact: Not produced. The report compares many client rows where the
table remains the clearer source of truth.

## Commands

```bash
./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild :bluetape4k-http:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache
```

Environment: local Colima Docker, `bluetape4k/mock-webflux-server:latest`, Docker server 29.2.1, 3905 MB Docker memory.

## Changes Tested

- Configured Vert.x `WebClient` with explicit `PoolOptions` in both HTTP client benchmarks.
- Added Ktor CIO coroutine rows to the benchmark harness.
- Switched the benchmark fixture from `BluetapeHttpServer` to `BluetapeWebfluxServer` so every client targets the same WebFlux/Reactor Netty mock server.
- Removed the one-thread Ktor CIO exception. Every row now uses the class-level JMH thread count.
- Shortened the class-level benchmark window to 1 warmup second and 1 measurement second. This is the comparable local snapshot that avoids ephemeral-port exhaustion while preserving equal concurrency.

## Base Throughput

`HttpClientBenchmark`, `GET /ping`, `@Threads(8)`, warmup 1x1s, measurement 1x1s, ops/s.

| Benchmark | ops/s |
|-----------|------:|
| `javaHttpSync` | 7,276.492 |
| `hc5ClassicVirtualThread` | 7,246.690 |
| `okhttp3VirtualThread` | 6,955.796 |
| `javaHttpVirtualThread` | 6,562.497 |
| `hc5Classic` | 6,490.422 |
| `javaHttpH2VirtualThread` | 6,275.262 |
| `hc5ClassicCoroutines` | 6,230.735 |
| `vertxWebClientCoroutines` | 6,043.906 |
| `javaHttpH2Sync` | 6,027.618 |
| `okhttp3Sync` | 5,771.310 |
| `okhttp3Coroutines` | 5,752.350 |
| `hc5AsyncCoroutines` | 5,520.183 |
| `javaHttpH2Coroutines` | 5,481.592 |
| `javaHttpCoroutines` | 4,739.894 |
| `ktorCioCoroutines` | 2,052.281 |

The `/ping` benchmark showed high local variance and is sensitive to connection reuse. Treat it as a same-fixture snapshot, not a production ranking.

## High-Latency Throughput

`HttpClientLatencyBenchmark`, `GET /httpbin/delay/0.05`, `@Threads(100)`, warmup 1x1s, measurement 1x1s, ops/s.

| Benchmark | ops/s |
|-----------|------:|
| `okhttp3VirtualThread` | 1,902.171 |
| `hc5ClassicVirtualThread` | 1,888.018 |
| `javaHttpVirtualThread` | 1,883.634 |
| `hc5Classic` | 1,880.023 |
| `okhttp3Sync` | 1,870.124 |
| `javaHttpSync` | 1,865.997 |
| `javaHttpCoroutines` | 1,863.948 |
| `hc5AsyncCoroutines` | 1,860.655 |
| `vertxWebClientCoroutines` | 1,859.003 |
| `okhttp3Coroutines` | 1,856.895 |
| `ktorCioCoroutines` | 1,515.026 |
| `hc5ClassicCoroutines` | 1,216.306 |

## Rejected Approaches

- Do not keep Ktor CIO as a one-thread row. It avoids the failure but does not compare against the same JMH concurrency as the other clients.
- Do not force Ktor CIO HTTP/1 pipelining in this fixture. `pipelining=true` with `pipelineMaxSize=1` produced `ClosedReadChannelException: unexpected EOF` or hung against both the MVC and WebFlux mock fixtures.
- Do not keep the previous MVC fixture for this rerun. Moving every row to `BluetapeWebfluxServer` addresses issue #584 and removes a server-side mismatch from the client comparison.

## Decisions

- Keep the Vert.x pool configuration in the benchmark harness. The previous high-latency row measured the Vert.x 5 default HTTP/1 pool cap instead of comparable client behavior.
- Include Ktor CIO in the equal-thread tables. Its default CIO path is materially slower in the base `/ping` workload because it opens dedicated HTTP/1 connections when the pipeline path is disabled.
- Treat this report as a short-window comparable snapshot. It is fairer than per-row thread overrides, but it is not a substitute for profiler-backed long-run capacity testing.

## Follow-Up

- Issue #585 should add CPU, allocation, and GC profiler runs before converting local benchmark rankings into production defaults.
- If long-run CIO capacity matters, add a dedicated CIO fixture or upstream bug investigation for the pipelining EOF/hang behavior instead of hiding it with per-row JMH settings.
