# Issue 587 Ktor CIO Fair Benchmark

## Context

The io/http benchmark originally added Ktor CIO as a bounded one-thread row
because full class concurrency exhausted local ephemeral ports. That made the
result hard to compare with HC5, OkHttp, JDK, and Vert.x rows.

## Decision

Use `BluetapeWebfluxServer` for both HTTP client benchmark classes and keep the
same JMH thread count for every row. Shorten the class-level benchmark window to
1 warmup second and 1 measurement second so Ktor CIO can run with the same
concurrency without per-row overrides.

## Outcome

The full benchmark now completes with Ktor CIO included in the same tables:

- `HttpClientBenchmark.ktorCioCoroutines`: 2,052.281 ops/s at `@Threads(8)`.
- `HttpClientLatencyBenchmark.ktorCioCoroutines`: 1,515.026 ops/s at `@Threads(100)`.

Ktor CIO is still slower in the base `/ping` workload because CIO 3.5 opens
dedicated HTTP/1 connections when its pipeline path is disabled.

## Verification

- `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild :bluetape4k-http:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache`

## Future Guidance

Do not hide Ktor CIO behind per-row thread overrides. If long-run CIO capacity
matters, add a dedicated CIO fixture or investigate the Ktor CIO pipelining
EOF/hang behavior directly.
