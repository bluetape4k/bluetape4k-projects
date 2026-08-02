# Issue #1279 Web-framework benchmark lifecycle lesson

## 배경

기존 `WebFrameworkStartupBenchmark`는 startup과 JVM heap snapshot을 한
invocation 안에서 실행하고 `Blackhole`에 memory 값을 소비했다. 따라서 raw JMH
결과에는 startup `ms/op`만 남았고, shutdown 시간과 memory sampling 시점도
구분되지 않았다. `BenchmarkServers.close()`의 `runCatching`은 두 server의
종료 실패를 숨겼으며, Spring factory가 실패하면 이미 생성된 Ktor server를
정리하지 못했다.

## 적용한 규칙

- ready-to-serve startup은 `ktorReadyStartup`과 `springWebFluxReadyStartup`으로
  분리하고 `@TearDown(Level.Invocation)`에서 종료해 shutdown을 timed path에서
  제외한다.
- memory는 별도 `WebFrameworkMemoryBenchmark`에서
  `jvm.used_heap`으로 기록한다. 이 값은 process RSS가 아니라
  `Runtime.totalMemory() - freeMemory()`이며, sampling point는
  `after_ready_before_shutdown`이다.
- JMH `AuxCounters`의 이벤트 raw sample은 `normalize-memory-report.py`가
  `bytes`, sample 수, sampling point를 가진 `memory-metrics.json`으로 변환한다.
  JMH의 보조 metric `#` score를 byte 단위 결과로 직접 해석하지 않는다.
- 두 resource를 닫을 때는 첫 실패를 primary로 유지하고 두 번째 실패를
  suppressed로 추가한다. 부분 startup에서는 이미 생성된 첫 resource를 같은
  규칙으로 정리한다.
- fake factory lifecycle 테스트는 부분 startup과 dual-close failure를 실제
  Ktor/Spring process 없이 고정한다.

## 검증

- `./gradlew :web-framework-benchmark:test --tests 'io.bluetape4k.benchmark.webframework.WebFrameworkBenchmarkLifecycleTest'`
- `./gradlew :web-framework-benchmark:compileBenchmarkKotlin`
- `./gradlew :web-framework-benchmark:startupBenchmark --no-configuration-cache`
- `./gradlew :web-framework-benchmark:memoryBenchmark --no-configuration-cache`
- generated `memory-metrics.json`의 `unit=bytes`,
  `samplingPoint=after_ready_before_shutdown`, `sampleCount=3`

## 남은 한계

이 benchmark는 외부 production Redisson/real deployment가 아니라 local embedded
Ktor와 Spring WebFlux를 비교한다. memory metric도 process RSS가 아니므로 RSS 기반
운영 결론에는 사용할 수 없다. Throughput/latency와 마찬가지로 짧은 local run은
release 성능 보증값이 아니다.
