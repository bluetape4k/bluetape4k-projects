# 스킬 성장 맵

**날짜**: 2026-05-25
**자동화**: 스킬 성장 맵 (`automation-2`)

---

## 배경

최근 merged PR과 lessons/design/benchmark 문서를 기준으로 다음에 더 깊게 익힐 역량을 정리했다.

검토한 최근 PR:

- PR #634 — `build: move default catalog ref to 2026-05-25-01`
- PR #632/#631 — HTTP benchmark profiling
- PR #630 — HTTP benchmark chart
- PR #629 — HC5 primary recommendation
- PR #628 — HC5 cache config DSL and cache metrics helpers
- PR #627 — HC5 production tuning defaults
- PR #625 — reusable DynamoDB Local launcher

최근 PR의 GitHub review/comment 객체는 비어 있었다. 따라서 추천 근거는 PR 주제와 `docs/lessons`, `docs/design`, `docs/benchmarks`에 남은 review findings, future guidance, 반복 이슈에서 도출했다.

---

## 발견

### 1. JMH/JFR/GC 프로파일링 운영 역량

근거:

- PR #631/#632, issue #585
- `docs/lessons/2026-05-24-http-benchmark-cpu-gc-profiling.md`
- `kotlinx-benchmark` 0.4.x Gradle DSL의 `advanced()` profiler 설정이 조용히 무시됨.
- 실제 benchmark exec task 이름은 `testBenchmark`였고, `name.endsWith("Exec")` 필터는 no-op이었다.

실행 과제:

- `:bluetape4k-http:testBenchmark`에 대해 `gc`, `jfr`, `async` profiling 모드를 각각 실행한다.
- 결과 파일 위치, 열람 도구, 해석 체크리스트를 benchmark 문서에 고정한다.
- benchmark task filter 작성 전 `./gradlew :module:tasks --all`로 실제 task 이름을 확인하는 습관을 체크리스트화한다.

### 2. HC5 API/lifecycle 계약 검증

근거:

- PR #627/#628
- `docs/lessons/2026-05-24-hc5-production-tuning-defaults.md`
- `docs/lessons/2026-05-24-hc5-cache-dsl-and-okhttp3-cache-support.md`
- `productionHttpAsyncClientOf` 테스트 누락이 review에서 CRITICAL로 잡힘.
- HC5 5.x는 connection manager builder에 `setThreadFactory`가 없어서 `VirtualThread` 이름이 실제 내부 thread factory wiring을 뜻하지 않음.
- `memoryCachingHttpAsyncClientOf` overload 간 started/unstated lifecycle 계약이 달라질 뻔함.

실행 과제:

- HC5 factory 추가 전 `javap`로 API 위치를 확인한다.
- factory overload마다 started/unstated lifecycle 계약 테스트를 추가한다.
- `VirtualThread`, `Async`, `Cache` 같은 이름이 실제 wiring을 과장하지 않는지 KDoc review를 추가한다.

### 3. HTTP adapter conformance와 cancellation 모델링

근거:

- issue #496
- `docs/lessons/2026-05-20-issue-496-http-adapter-conformance.md`
- Retrofit/Feign/HC5/Vert.x 간 cancellation, timeout, delayed body cleanup, request tag 동작이 backend별로 drift될 수 있어 공유 conformance suite로 승격했다.

실행 과제:

- 새 HTTP backend나 wrapper 작업 전 shared conformance suite를 먼저 확장한다.
- 최소 보장 항목은 cancel-before-enqueue, in-flight cancel, delayed body cleanup, timeout exposure, request tag propagation으로 둔다.
- Feign async cancellation은 public `CompletableFuture` 상태만 검증 중이므로, stable request handle이 생기면 underlying-request cancellation coverage를 추가한다.

### 4. 의존성 카탈로그/ABI 드리프트 디버깅

근거:

- PR #634
- `docs/lessons/2026-05-23-bt4k-version-catalog-consumption.md`
- `docs/lessons/2026-05-25-catalog-ref-2026-05-25-01.md`
- catalog update가 Hibernate 7.3.4 test KAPT/ANTLR runtime 충돌과 Redisson/Fory ABI 문제를 노출했다.

실행 과제:

- catalog bump PR에는 `dependencyInsight`, KAPT processor isolation, smoke-test codec 목적성 확인을 묶은 dependency train checklist를 붙인다.
- `bluetape4k-dependencies`가 관리하는 버전은 local `libs.versions.toml`에 직접 추가하기보다 `bt4k` catalog에서 읽는다.
- generic Testcontainers smoke test에서는 Redisson/Fory ABI 자체가 목적이 아니라면 Redisson built-in Fory codec을 사용하지 않는다.

### 5. Testcontainers CI 결정성

근거:

- PR #625
- issue #595
- `docs/lessons/2026-05-24-dynamodb-local-server.md`
- `docs/lessons/2026-05-22-issue-595-nightly-failures.md`
- private container 중복, Elasticsearch reuse로 인한 stale credentials, Memgraph bind address 누락이 CI 실패를 만들었다.

실행 과제:

- 새 container fixture는 SDK-neutral launcher, explicit endpoint/credential property, singleton reuse 정책을 포함한다.
- downstream test base에 private `GenericContainer`를 재도입하지 않고 shared launcher를 소비하게 한다.
- image tag나 command-line flag를 바꿀 때 listener bind address와 host port mapping을 함께 검증한다.

### 6. 벤치마크 근거를 제품 추천으로 바꾸는 판단력

근거:

- PR #629/#630
- `docs/design/2026-05-24-hc5-first-http-client-recommendation.md`
- `docs/benchmarks/2026-05-21-io-http-client-benchmark.md`
- HC5 primary recommendation은 성능 수치와 기능 matrix를 근거로 했지만, local Colima snapshot, P95/P99 부재, CI-gated threshold 부재를 명시했다.

실행 과제:

- 성능 기반 recommendation PR은 측정 환경, 제외한 해석, follow-up issue, use-case matrix를 필수 산출물로 둔다.
- local short-window benchmark는 production ranking이 아니라 comparable snapshot으로 표현한다.
- README 추천 표는 benchmark raw table보다 상단에 두고, detail은 design note로 분리한다.

---

## 결과

다음 학습 우선순위는 HTTP 성능 측정과 HC5 운영 계약을 1순위로 두고, 그 다음으로 conformance/cancellation, dependency train, Testcontainers 결정성, benchmark-to-doc decision hygiene을 강화하는 것이다.

---

## 검증

- 최근 merged PR 목록을 확인했다.
- 최근 PR들의 GitHub review/comment 객체가 비어 있음을 확인했다.
- `docs/lessons`, `docs/design`, `docs/benchmarks`의 관련 문서를 근거로 추천 항목을 구성했다.

---

## 향후 지침

다음 실행에서는 이 파일과 자동화 메모리를 먼저 읽고, 이미 추천한 6개 축 중 실제 PR/review에서 새로 반복된 축을 상향 조정한다. 새 추천은 반드시 PR, issue, review finding, lesson 문서 중 하나 이상의 구체 근거에 연결한다.
