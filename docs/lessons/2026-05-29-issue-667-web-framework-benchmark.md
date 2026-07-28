# Lesson: Web Framework Benchmark Baseline

**날짜**: 2026-05-29
**이슈**: #667

## 배경

Ktor module family는 #609의 넓은 API work 전에 최신 evidence가 필요했다.

## 결정

`kotlinx-benchmark`를 사용하는 non-published `benchmark/web-framework-benchmark`
module을 추가한다. 동등한 Ktor CIO와 Spring WebFlux benchmark application은 production
module이 아니라 benchmark module 안에 둔다.

## 결과

benchmark는 이제 동등한 health, single-ID, batch-ID, bad-request path에 대한 startup,
throughput, average latency를 다룬다.

## 검증

- `./gradlew :web-framework-benchmark:compileBenchmarkKotlin --no-configuration-cache`
- `./gradlew :web-framework-benchmark:benchmarkStartupBenchmark --no-configuration-cache`
- `./gradlew :web-framework-benchmark:benchmarkThroughputBenchmark :web-framework-benchmark:benchmarkLatencyBenchmark --no-configuration-cache`

## 향후 가드

`server.port=0`을 사용하고 실제 bound port는 framework runtime에서 읽는다.
`ServerSocket(0)`로 local port를 미리 고르면 반복되는 JMH startup iteration 중 race가
발생할 수 있다.
