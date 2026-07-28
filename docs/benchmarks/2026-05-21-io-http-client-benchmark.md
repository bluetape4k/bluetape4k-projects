# io/http HTTP Client Benchmark - 2026-05-21

## 범위

이 보고서는 GitHub epic #589와 sub-issue #590, #587의 benchmark-driven 작업을
기록한다. Epic에 포함된 기존 관련 issue는 #582, #583, #584, #585, #586이다.

대상 module: `:bluetape4k-http`.

Raw artifact: 보존하지 않았다. 이 보고서는 local run에서 축약한 JMH table을 보존한다.

Chart artifact: 생성하지 않았다. 많은 client row를 비교하므로 table이 더 명확한
source of truth로 남는다.

## 명령

```bash
./gradlew :bluetape4k-mock-webflux-server:jibDockerBuild :bluetape4k-http:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark.ktorCioCoroutines|HttpClientLatencyBenchmark.ktorCioCoroutines' --no-configuration-cache
./gradlew :bluetape4k-http:testBenchmark -PbenchmarkInclude='HttpClientBenchmark|HttpClientLatencyBenchmark' --no-configuration-cache
```

환경: local Colima Docker, `bluetape4k/mock-webflux-server:latest`, Docker server
29.2.1, Docker memory 3905 MB.

## 테스트한 변경

- 두 HTTP client benchmark에서 Vert.x `WebClient`를 명시적인 `PoolOptions`로 설정했다.
- Benchmark harness에 Ktor CIO coroutine row를 추가했다.
- 모든 client가 같은 WebFlux/Reactor Netty mock server를 대상으로 삼도록 benchmark fixture를
  `BluetapeHttpServer`에서 `BluetapeWebfluxServer`로 전환했다.
- One-thread Ktor CIO exception을 제거했다. 이제 모든 row가 class-level JMH thread count를
  사용한다.
- Class-level benchmark window를 warmup 1초와 measurement 1초로 줄였다. 이는 동일한
  concurrency를 보존하면서 ephemeral-port exhaustion을 피하는 비교 가능한 local snapshot이다.

## 기본 처리량

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

`/ping` benchmark는 local variance가 높고 connection reuse에 민감했다. Production
ranking이 아니라 same-fixture snapshot으로 다룬다.

## High-Latency 처리량

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

## 기각한 접근

- Ktor CIO를 one-thread row로 유지하지 않는다. 실패는 피하지만 다른 client와 같은 JMH
  concurrency로 비교하지 못한다.
- 이 fixture에서 Ktor CIO HTTP/1 pipelining을 강제하지 않는다. `pipelineMaxSize=1`과
  `pipelining=true` 조합은 MVC와 WebFlux mock fixture 양쪽에서
  `ClosedReadChannelException: unexpected EOF`를 만들거나 hang을 일으켰다.
- 이번 재실행에서 이전 MVC fixture를 유지하지 않는다. 모든 row를
  `BluetapeWebfluxServer`로 옮기면 issue #584를 해결하고 client 비교에서 server-side
  mismatch를 제거한다.

## 결정

- Benchmark harness에 Vert.x pool configuration을 유지한다. 이전 high-latency row는
  비교 가능한 client behavior가 아니라 Vert.x 5 default HTTP/1 pool cap을 측정했다.
- Equal-thread table에 Ktor CIO를 포함한다. Pipeline path가 꺼져 있을 때 dedicated
  HTTP/1 connection을 열기 때문에 기본 `/ping` workload에서 default CIO path는 유의미하게
  느리다.
- 이 보고서는 short-window comparable snapshot으로 다룬다. Per-row thread override보다
  공정하지만 profiler-backed long-run capacity testing의 대체물은 아니다.

## 후속 작업

- Issue #585는 local benchmark ranking을 production default로 전환하기 전에 CPU,
  allocation, GC profiler run을 추가해야 한다.
- Long-run CIO capacity가 중요하다면 per-row JMH setting으로 숨기지 말고, dedicated CIO
  fixture 또는 pipelining EOF/hang behavior에 대한 upstream bug investigation을 추가한다.
