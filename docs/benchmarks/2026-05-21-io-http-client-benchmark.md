# io/http HTTP Client Benchmark - 2026-05-21

## Scope

This report records benchmark-driven work for GitHub epic #589 and sub-issue #590.
Related existing issues included in the epic: #582, #583, #584, #585, #586, and #587.

Target module: `:bluetape4k-http`.

## Commands

```bash
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache
```

Environment: local Colima Docker, `bluetape4k/mock-web-server:latest`, Docker server 29.2.1, 3905 MB Docker memory.

## Changes Tested

- Configured Vert.x `WebClient` with explicit `PoolOptions` in both HTTP client benchmarks.
- Added Ktor CIO coroutine rows to the benchmark harness.
- Kept Ktor CIO rows bounded because full class concurrency exhausted local ephemeral ports against the Docker fixture.

## Base Throughput

`HttpClientBenchmark`, `GET /ping`, ops/s.

| Benchmark | Before | After | Change |
|-----------|-------:|------:|-------:|
| `hc5AsyncCoroutines` | 11,191.120 | 11,675.259 | +4.3% |
| `hc5Classic` | 17,115.856 | 18,453.421 | +7.8% |
| `hc5ClassicCoroutines` | 16,374.041 | 15,799.529 | -3.5% |
| `hc5ClassicVirtualThread` | 17,425.528 | 18,327.179 | +5.2% |
| `javaHttpCoroutines` | 14,679.410 | 14,459.907 | -1.5% |
| `javaHttpH2Coroutines` | 14,288.272 | 13,240.647 | -7.3% |
| `javaHttpH2Sync` | 19,216.697 | 17,125.597 | -10.9% |
| `javaHttpH2VirtualThread` | 17,158.663 | 17,328.761 | +1.0% |
| `javaHttpSync` | 18,589.051 | 17,224.067 | -7.3% |
| `javaHttpVirtualThread` | 17,736.870 | 18,355.593 | +3.5% |
| `okhttp3Coroutines` | 14,398.746 | 13,172.953 | -8.5% |
| `okhttp3Sync` | 17,195.947 | 17,240.102 | +0.3% |
| `okhttp3VirtualThread` | 17,669.541 | 17,643.375 | -0.1% |
| `vertxWebClientCoroutines` | 12,278.843 | 13,491.266 | +9.9% |
| `ktorCioCoroutines` | n/a | 659.071 | bounded |

The `/ping` benchmark showed high local variance. Treat the Vert.x direction as useful, but not as the primary decision signal.

## High-Latency Throughput

`HttpClientLatencyBenchmark`, `GET /httpbin/delay/0.05`, ops/s.

| Benchmark | Before | After | Change |
|-----------|-------:|------:|-------:|
| `hc5AsyncCoroutines` | 1,826.352 | 1,789.987 | -2.0% |
| `hc5Classic` | 1,751.968 | 1,848.171 | +5.5% |
| `hc5ClassicCoroutines` | 1,161.095 | 1,168.324 | +0.6% |
| `hc5ClassicVirtualThread` | 1,791.481 | 1,848.070 | +3.2% |
| `javaHttpCoroutines` | 376.241 | 383.605 | +2.0% |
| `javaHttpSync` | 376.703 | 383.628 | +1.8% |
| `javaHttpVirtualThread` | 375.674 | 384.459 | +2.3% |
| `okhttp3Coroutines` | 1,831.756 | 1,759.273 | -4.0% |
| `okhttp3Sync` | 1,762.527 | 1,809.275 | +2.7% |
| `okhttp3VirtualThread` | 1,822.840 | 1,784.900 | -2.1% |
| `vertxWebClientCoroutines` | 87.844 | 1,818.508 | +1,970.2% |
| `ktorCioCoroutines` | n/a | 16.501 | bounded |

## Decisions

- Keep the Vert.x pool configuration in the benchmark harness. The previous high-latency row measured the Vert.x 5 default HTTP/1 pool cap instead of comparable client behavior.
- Include Ktor CIO in the harness, but mark it as a bounded comparison row. Equal-thread CIO runs produced `java.net.BindException: Can't assign requested address` in this local fixture.
- Do not enable Ktor CIO HTTP/1 pipelining for this benchmark. It produced unexpected EOFs against the mock server.

## Follow-Up

- Issue #587 should refine the Ktor CIO benchmark surface so it can report a fair high-concurrency comparison or a clearly separate fixture.
- Issue #585 should add CPU, allocation, and GC profiler runs before converting local benchmark rankings into production defaults.
