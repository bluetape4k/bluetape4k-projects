# 이슈 590 io/http Self-Improve

## 배경

`io/http`는 HC5, OkHttp3, Vert.x, JDK, Ktor CIO 전반의 benchmark-driven comparison이 필요했다.

## 결정

High-latency benchmark가 Vert.x 5 default HTTP/1 pool cap이 아니라 client behavior를 비교하도록
Vert.x WebClient benchmark setup에 explicit `PoolOptions`를 사용한다.

Ktor CIO는 bounded benchmark row로 포함한다. Full class concurrency는 local ephemeral port를 고갈시켰고,
CIO HTTP/1 pipelining은 Docker mock server를 상대로 unexpected EOF를 유발했다.

## 결과

Local JMH run에서 high-latency Vert.x throughput은 87.844 ops/s에서 1,818.508 ops/s로 이동했다.
Ktor CIO는 clean bounded row만 가진다: `/ping` 659.071 ops/s, 50 ms delay endpoint 16.501 ops/s.

## 검증

- `./gradlew :bluetape4k-http:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache`

## 향후 agent 가이드

Bounded Ktor CIO row를 equal-thread comparison으로 취급하지 않는다. Production default에 Ktor CIO number를
사용하기 전에 fixture 또는 port reuse behavior를 재작업한다.
