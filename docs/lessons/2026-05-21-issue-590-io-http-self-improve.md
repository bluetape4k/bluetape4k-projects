# Issue 590 io/http Self-Improve

## Context

`io/http` needed benchmark-driven comparison across HC5, OkHttp3, Vert.x, JDK, and Ktor CIO.

## Decision

Vert.x WebClient benchmark setup now uses explicit `PoolOptions` so the high-latency benchmark compares client behavior instead of the Vert.x 5 default HTTP/1 pool cap.

Ktor CIO is included as a bounded benchmark row. Full class concurrency exhausted local ephemeral ports, and CIO HTTP/1 pipelining caused unexpected EOFs against the Docker mock server.

## Outcome

High-latency Vert.x throughput moved from 87.844 ops/s to 1,818.508 ops/s in the local JMH run. Ktor CIO has clean bounded rows only: 659.071 ops/s for `/ping`, 16.501 ops/s for the 50 ms delay endpoint.

## Verification

- `./gradlew :bluetape4k-http:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache`

## Future Agents

Do not treat bounded Ktor CIO rows as equal-thread comparisons. Rework the fixture or port reuse behavior before using Ktor CIO numbers for production defaults.
