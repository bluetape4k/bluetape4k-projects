# Web Framework Benchmark

[English](./README.md) | 한국어

이 모듈은 Ktor CIO와 Spring WebFlux로 같은 HTTP workload를 구현하고 비교합니다. JDK `HttpClient`로 local loopback server에 요청을 보내므로, framework routing, JSON 응답 직렬화, 요청 처리, 최소 server lifecycle 비용이 함께 측정됩니다.

![Web framework throughput](../../docs/images/readme-charts/benchmark-web-framework-throughput-chart-01.png)

![Web framework latency](../../docs/images/readme-charts/benchmark-web-framework-latency-chart-01.png)

## 무엇을 측정하나

Benchmark 소스는
`io.bluetape4k.benchmark.webframework.WebFrameworkBenchmark`입니다.

두 framework는 같은 논리 endpoint를 제공합니다.

| Endpoint                       | 목적                                                       |
|--------------------------------|------------------------------------------------------------|
| `/health`                      | 작은 JSON health 응답을 반환합니다.                        |
| `/idgen/uuid-v7`               | 공유 generator registry를 통해 UUIDv7 하나를 생성합니다.   |
| `/idgen/uuid-v7/batch?size=10` | 작은 UUIDv7 batch를 생성합니다.                            |
| `/idgen/unknown`               | 알 수 없는 generator 요청의 bad-request 경로를 측정합니다. |

Generator registry에는 UUIDv4, UUIDv7, ULID, KSUID, Snowflake, Flake가 함께 등록되어 있어, HTTP layer가 실제 library module과 같은 ID 생성 표면을 사용합니다.

## 최근 로컬 결과

Throughput과 latency snapshot은 2026-06-19 GraalVM JDK 21.0.11에서,
startup과 memory lifecycle snapshot은 2026-08-03 GraalVM JDK 21.0.12에서
실행했습니다. 각 benchmark는 Gradle configuration에서 다르게 지정하지 않는 한
fork 1회, measurement 3회로 실행합니다.

### Throughput

```bash
./gradlew :web-framework-benchmark:throughputBenchmark --rerun-tasks --no-parallel
```

Mode: JMH throughput, `ops/s`, 높을수록 좋습니다.

| Endpoint    |    Ktor CIO | Spring WebFlux |
|-------------|------------:|---------------:|
| Health      | 4,006 ops/s |    6,112 ops/s |
| Single ID   | 3,727 ops/s |    6,441 ops/s |
| Batch IDs   | 3,719 ops/s |    6,047 ops/s |
| Bad request | 3,697 ops/s |    6,218 ops/s |

### Latency

```bash
./gradlew :web-framework-benchmark:latencyBenchmark --rerun-tasks --no-parallel
```

Mode: JMH average time, `us/op`, 낮을수록 좋습니다.

| Endpoint    |      Ktor CIO | Spring WebFlux |
|-------------|--------------:|---------------:|
| Health      | 267.492 us/op |  172.170 us/op |
| Single ID   | 309.815 us/op |  179.457 us/op |
| Batch IDs   | 274.638 us/op |  167.923 us/op |
| Bad request | 255.166 us/op |  164.028 us/op |

### Ready-to-serve startup

```bash
./gradlew :web-framework-benchmark:startupBenchmark
```

Mode: JMH average time, `ms/op`, 낮을수록 좋습니다. Server construction이
connector/application readiness에 도달할 때까지를 측정하며,
`@TearDown(Level.Invocation)`이 invocation 뒤에 server를 닫습니다. 따라서
shutdown은 이 metric에 포함되지 않습니다.

| Framework      | Benchmark                   | Score | Error |
|----------------|-----------------------------|------:|------:|
| Ktor CIO       | `ktorReadyStartup`          | 0.675 ms/op | ±0.316 |
| Spring WebFlux | `springWebFluxReadyStartup` | 77.821 ms/op | ±146.638 |

### JVM used-heap snapshot

```bash
./gradlew :web-framework-benchmark:memoryBenchmark
```

Memory benchmark는 각 server가 ready가 된 직후, invocation teardown 전에
`jvm.used_heap`를 기록합니다. `Runtime.totalMemory() - freeMemory()`를
사용하므로 process RSS가 아니라 JVM used heap입니다. JMH JSON에는 raw event
sample이 남고, 같은 디렉터리의 `memory-metrics.json`은 명시적인 `bytes` 단위,
sample 수, `after_ready_before_shutdown` sampling point를 기록합니다.
정규화된 byte 표가 memory 결과의 source of truth이며, memory benchmark의
primary `ms/op` 값은 sampling harness invocation 시간일 뿐 memory 값이 아닙니다.

| Framework      | Benchmark                    | Average bytes | Samples |
|----------------|------------------------------|--------------:|--------:|
| Ktor CIO       | `ktorReadyUsedHeap`          |    39,887,232 |       3 |
| Spring WebFlux | `springWebFluxReadyUsedHeap` |    53,612,200 |       3 |

이 숫자는 로컬 비교 snapshot이며, release 보증값이 아닙니다. Throughput과 latency는 1초 measurement window로 짧게 측정했으므로, 제품 수준의 성능 주장을 하려면 더 긴 JMH 실행으로 다시 측정해야 합니다.

## 실행

```bash
./gradlew :web-framework-benchmark:throughputBenchmark
./gradlew :web-framework-benchmark:latencyBenchmark
./gradlew :web-framework-benchmark:startupBenchmark
./gradlew :web-framework-benchmark:memoryBenchmark
```

원본 JMH JSON report는
`benchmark/web-framework-benchmark/build/reports/benchmarks/` 아래에 생성됩니다.
Memory 실행은 JMH `benchmark.json`과 함께 `memory-metrics.json`도 생성합니다.
