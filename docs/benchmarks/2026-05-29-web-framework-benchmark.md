# Ktor CIO vs Spring WebFlux Benchmark - 2026-05-29

## 범위

Issue: #667

Module: `:web-framework-benchmark`

이 benchmark는 Ktor CIO와 Spring Boot WebFlux/Netty로 구현한 동등한 in-memory ID
generator HTTP API를 비교한다. Workload는 기존 `idgenerator` example domain을 의도적으로
반영한다. 따라서 production module에 benchmark-only code를 추가하지 않고도 #609의 Ktor
module family design을 안내할 수 있다.

## 명령

```bash
./gradlew :web-framework-benchmark:compileBenchmarkKotlin --no-configuration-cache
./gradlew :web-framework-benchmark:benchmarkStartupBenchmark --no-configuration-cache
./gradlew :web-framework-benchmark:benchmarkThroughputBenchmark :web-framework-benchmark:benchmarkLatencyBenchmark --no-configuration-cache
./scripts/web-framework-benchmark.sh
```

## 실행 조건

| 항목 | 값 |
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

Raw artifact:

- `docs/benchmarks/raw/2026-05-29-web-framework-startup.json`
- `docs/benchmarks/raw/2026-05-29-web-framework-throughput.json`
- `docs/benchmarks/raw/2026-05-29-web-framework-latency.json`

Chart artifact: 생성하지 않았다. Startup, throughput, average latency는 서로 다른 단위를
사용하므로 별도 table로 유지한다.

## Startup Snapshot

낮을수록 좋다.

| Benchmark | ms/op |
|---|---:|
| `ktorStartup` | 0.741 |
| `springWebFluxStartup` | 2,087.926 |

Ktor startup 수치는 embedded CIO binding과 connector resolution만 측정한다. Spring
수치는 Spring Boot reactive application context 생성과 Netty binding을 포함한다. 이를
end-user cold-start SLA가 아니라 framework bootstrap shape로 다룬다.

## Throughput

높을수록 좋다.

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

## 평균 지연 시간

낮을수록 좋다.

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

## 해석

- Ktor CIO는 Spring Boot context creation을 피하므로 이 embedded benchmark에서 훨씬 빠르게
  시작한다.
- Spring WebFlux는 이 short-window local fixture에서 steady-state request throughput과
  average request latency가 더 빠르다.
- 현재 Ktor library design은 얇고 명시적으로 유지한다. Shared domain behavior는 core
  module에 두고, 반복되는 integration code를 제거할 때만 Ktor helper를 추가한다.
- Startup이 빠르다는 이유만으로 Ktor server abstraction을 선택하지 않는다. Request-path
  helper에서는 handler ergonomics, cancellation behavior, steady-state performance를
  함께 비교한다.

## 후속 Issue

- #643은 Ktor client support가 `io/http`, `ktor/client`, thin bridge 중 어디에 속하는지
  먼저 결정해야 한다.
- #644는 Resilience4j helper를 coroutine-safe로 유지하고 route helper가 생긴 뒤 timeout /
  retry overhead를 benchmark해야 한다.
- #645는 route contract가 안정될 때까지 documentation/metadata 중심으로 남긴다.
- 향후 benchmark는 `wrk`, `oha`, Gatling 같은 external load tool로 p50/p95/p99 histogram을
  추가할 수 있다. 현재 JMH fixture는 percentile latency가 아니라 throughput과 average
  latency를 기록한다.
