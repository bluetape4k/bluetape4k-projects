# kotlinx-benchmark JMH benchmark의 CPU/GC profiling

**날짜**: 2026-05-24
**이슈**: #585 — perf(io-http): add CPU and GC profiling to HTTP client benchmarks
**브랜치**: perf/http-benchmark-profiling-20260524

---

## 근본 원인 / 배경

kotlinx-benchmark 0.4.x는 Gradle DSL을 통해 JMH의 `OptionsBuilder.addProfiler()`를 expose하지 않는다.
`BenchmarkConfiguration.advanced()` map은 runtime에 `JvmBenchmarkRunner`가 parse하지만 `jvmForks` key만
실제로 사용한다. 다른 key는 parameters file에 쓰인 뒤 runner에서 조용히 무시된다.

따라서 JMH built-in profiler(`gc`, `jfr`, `stack`)는 `advanced()` DSL로 활성화할 수 없다. 올바른 접근은
`tasks.withType<JavaExec>().configureEach { ... }`로 generated `JavaExec` benchmark exec task에 JVM args를
설정하는 것이다.

---

## 결정

Gradle task configuration을 통해 주입한 JVM-level profiling flag를 사용한다:

| Mode | JVM flag |
|------|----------|
| `gc` | `-Xlog:gc*,safepoint:file=...` |
| `jfr` | `-XX:StartFlightRecording=filename=...,dumponexit=true,settings=profile` |
| `async` | `-agentpath:libasyncProfiler.so=start,event=cpu,file=...,flamegraph` |

Gradle property `-PbenchmarkProfile=<gc|jfr|async>`로 활성화한다.

---

## 주요 함정: kotlinx-benchmark task naming

kotlinx-benchmark plugin은 benchmark exec task 이름을 다음 방식으로 생성한다:

```
"${target.name}${config.capitalizedName()}${BENCHMARK_EXEC_SUFFIX}"
```

여기서:

- `capitalizedName()`은 config name이 default인 `"main"`이면 `""`를 반환한다.
- `BENCHMARK_EXEC_SUFFIX = "Benchmark"`다.

따라서 `targets { register("test") }` + default `configurations { named("main") }`에서는
**task name이 `"testBenchmark"`**다(`"testMainBenchmarkExec"`가 아님).

`name.endsWith("Exec")` filter는 절대 match되지 않고 profiling block은 silent no-op이 된다. 올바른 filter는
`name.endsWith("Benchmark")`와 `tasks.withType<JavaExec>()`의 조합이다. 이렇게 해야 이름에 "Benchmark"가
들어간 lifecycle `DefaultTask` instance를 제외할 수 있다.

---

## 검증 증거

- Build script는 `-PbenchmarkProfile=gc` 유무 모두에서 clean compile.
- `./gradlew :bluetape4k-http:tasks --all`로 `testBenchmark`가 JavaExec exec task임을 확인.
- 기존 smoke test 8개 통과: `HttpClientBenchmarkTest` — 8 tests, failure 0.

---

## 향후 가이드

- `configureEach { }` block에 task name filter를 쓰기 전에 실제 task name을
  `./gradlew :module:tasks --all`로 항상 확인한다.
- kotlinx-benchmark가 future release에서 native profiler support를 추가하면 그 DSL로 migration하고
  `tasks.withType<JavaExec>` workaround를 제거한다.
- JFR output은 JDK Mission Control(`jmc`) 또는 IntelliJ IDEA JFR viewer로
  `build/benchmark-profiling/benchmark.jfr`를 연다.
- async-profiler는 native agent library가 필요하다. kotlinx-benchmark runtime은 JVM args에서
  `libasyncProfiler`를 auto-detect하고 in-process profiling에 필요한 forks=0을 설정한다.
