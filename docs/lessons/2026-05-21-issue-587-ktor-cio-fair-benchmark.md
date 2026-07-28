# 이슈 587 Ktor CIO Fair Benchmark

## 배경

`io/http` benchmark는 원래 Ktor CIO를 bounded one-thread row로 추가했다. Full class concurrency가 local
ephemeral port를 고갈시켰기 때문이다. 하지만 이 방식은 HC5, OkHttp, JDK, Vert.x row와 비교하기 어렵다.

## 결정

두 HTTP client benchmark class 모두 `BluetapeWebfluxServer`를 사용하고 모든 row에 같은 JMH thread count를
유지한다. Ktor CIO가 per-row override 없이 같은 concurrency로 실행될 수 있도록 class-level benchmark
window를 warmup 1초, measurement 1초로 줄인다.

## 결과

Full benchmark는 같은 table에 Ktor CIO를 포함해 완료된다:

- `HttpClientBenchmark.ktorCioCoroutines`: `@Threads(8)`에서 2,052.281 ops/s.
- `HttpClientLatencyBenchmark.ktorCioCoroutines`: `@Threads(100)`에서 1,515.026 ops/s.

Ktor CIO 3.5가 pipeline path disabled 상태에서 dedicated HTTP/1 connection을 열기 때문에 기본 `/ping`
workload에서는 여전히 느리다.

## 검증

- `./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild :bluetape4k-http:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache`
- `./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache`

## 향후 가이드

Ktor CIO를 per-row thread override 뒤에 숨기지 않는다. Long-run CIO capacity가 중요하면 전용 CIO fixture를
추가하거나 Ktor CIO pipelining EOF/hang behavior를 직접 조사한다.
