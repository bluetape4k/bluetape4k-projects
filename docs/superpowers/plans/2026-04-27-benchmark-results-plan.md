# Benchmark Results Documentation Implementation Plan — Issue #184

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `infra/lettuce`, `infra/redisson`, `infra/cache-lettuce` 세 모듈에 동일 품질의 JMH 벤치마크 결과 문서 (`Benchmark.md` + `Benchmark.ko.md`)를 작성하고, `infra/cache-lettuce` 에는 NearCache JMH 벤치마크 코드 (sourceset + 클래스)를 신규 추가한다.

**Architecture:**

- `infra/lettuce`, `infra/redisson` — 기존 `LettuceCodecBenchmark.kt` / `RedisonCodecBenchmark.kt` 를 그대로 사용해 결과 캡처 + 문서 작성.
- `infra/cache-lettuce` — `kotlinx_benchmark` plugin + `benchmark` sourceset 를 `infra/lettuce/build.gradle.kts` 패턴으로 추가한 뒤 `NearCacheBenchmark.kt` (단일 클래스, l1Hit/l2Hit/l2Miss/putSingle/putAll/removeSingle 6 시나리오) 작성. Redis 부트스트랩은 Testcontainers `RedisServer.Launcher.LettuceLib.getRedisURI(host, port)` 재사용.
- 문서 스타일: `utils/idgenerators/Benchmark.md` colored `<span>` bar chart + Summary table + raw JMH + Analysis + Recommendations + Conclusion + Environment.

**Tech
Stack:** Kotlin 2.3, JDK 21, kotlinx-benchmark (JMH), Lettuce 6.8, Redisson 3.x, Caffeine, Testcontainers Redis 7+ (RESP3), Mermaid (README only — Benchmark.md 본문은 colored span bars only)

**Spec:** `docs/superpowers/specs/2026-04-27-benchmark-results-design.md`

**Worktree:** `.worktrees/docs-benchmark-results/` · **Branch:** `docs/benchmark-results`

> **Commit
policy:** 사용자가 명시적으로 요청할 때만 git commit. 본 plan 은 commit step 미포함 — Group D 의 EN+KO 페어는 동일 commit 에 들어가야 함을 명세상 요구함 (D-task 가 끝난 시점에 사용자 확인 후 commit).

---

## Task Overview

| ID     | Title                                                                           | Complexity | Depends on | 병렬 가능            |
|--------|---------------------------------------------------------------------------------|------------|------------|----------------------|
| **A1** | `infra/cache-lettuce/build.gradle.kts` 에 benchmark sourceset 추가              | medium     | —          | A1, C1, C2 동시 가능 |
| **B1** | `NearCacheBenchmark.kt` 작성 (6 시나리오)                                       | high       | A1         | —                    |
| **C1** | `:bluetape4k-lettuce:benchmark` 실행 + raw JMH 캡처                             | medium     | —          | A1, B1, C2 와 병렬   |
| **C2** | `:bluetape4k-redisson:benchmark` 실행 + raw JMH 캡처                            | medium     | —          | A1, B1, C1 과 병렬   |
| **C3** | `:bluetape4k-cache-lettuce:benchmark` 실행 (Docker 필요) + 캡처                 | high       | B1         | C1/C2 종료 후 권장   |
| **D1** | `infra/lettuce/Benchmark.md` + `Benchmark.ko.md` 작성 (페어, 동일 commit)       | medium     | C1         | D2, D3 와 병렬       |
| **D2** | `infra/redisson/Benchmark.md` + `Benchmark.ko.md` 작성 (Cross-Module Note 포함) | medium     | C2, D1     | D3 와 병렬           |
| **D3** | `infra/cache-lettuce/Benchmark.md` + `Benchmark.ko.md` 작성 (Future Work 포함)  | medium     | C3         | D1, D2 와 병렬       |
| **E1** | `infra/lettuce/README.md`(+ko) Performance 섹션 링크 추가                       | low        | D1         | E2, E3 와 병렬       |
| **E2** | `infra/redisson/README.md`(+ko) Performance 섹션 링크 추가                      | low        | D2         | E1, E3 와 병렬       |
| **E3** | `infra/cache-lettuce/README.md`(+ko) Performance 링크 + Mermaid 갱신            | low        | D3         | E1, E2 와 병렬       |
| **F1** | DoD 검증 (compile/test/detekt/code-reviewer)                                    | medium     | A1~E3      | —                    |

병렬화 권장 흐름:

1. **Wave 1**: A1 + C1 + C2 동시 실행 (서로 독립, 약 5–15분 소요).
2. **Wave 2**: A1 종료 후 B1 시작. C1/C2 종료 후 D1/D2 시작.
3. **Wave 3**: B1 종료 후 C3 실행. C3 종료 후 D3 시작.
4. **Wave 4**: D1/D2/D3 종료 후 E1/E2/E3 동시 실행.
5. **Wave 5**: F1 검증.

---

## File Structure

```text
infra/lettuce/
├── Benchmark.md                                       # NEW (D1)
├── Benchmark.ko.md                                    # NEW (D1)
├── README.md                                          # MOD (E1) — Performance 링크
├── README.ko.md                                       # MOD (E1)
└── src/benchmark/kotlin/.../LettuceCodecBenchmark.kt  # 기존 (변경 없음)

infra/redisson/
├── Benchmark.md                                       # NEW (D2)
├── Benchmark.ko.md                                    # NEW (D2)
├── README.md                                          # MOD (E2)
├── README.ko.md                                       # MOD (E2)
└── src/benchmark/kotlin/.../RedissonCodecBenchmark.kt # 기존 (변경 없음)

infra/cache-lettuce/
├── Benchmark.md                                       # NEW (D3)
├── Benchmark.ko.md                                    # NEW (D3)
├── README.md                                          # MOD (E3) — Mermaid 갱신
├── README.ko.md                                       # MOD (E3)
├── build.gradle.kts                                   # MOD (A1)
└── src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/
    └── NearCacheBenchmark.kt                          # NEW (B1)
```

---

## Group A — Build Infrastructure

### Task A1: `infra/cache-lettuce/build.gradle.kts` 에 benchmark sourceset 추가

**Files:**

- Modify: `infra/cache-lettuce/build.gradle.kts`

**Template:** `infra/lettuce/build.gradle.kts` (그대로 복제하되, dependencies 는 cache-lettuce 의 NearCache 요건에 맞춰 합산).

**Steps:**

- [ ] **Step 1: plugins 블록 추가**
  ```kotlin
  plugins {
      kotlin("plugin.allopen")
      id(Plugins.kotlinx_benchmark)
  }

  allOpen {
      annotation("org.openjdk.jmh.annotations.State")
  }
  ```
- [ ] **Step 2: sourceSets / kotlin compilations 블록 추가**
  ```kotlin
  sourceSets {
      create("benchmark")
  }

  kotlin {
      target {
          compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
      }
  }
  ```
- [ ] **Step 3: `benchmark { targets { register("benchmark") { ... } } }` 블록 추가** (lettuce build.gradle 23–29행과 동일).
- [ ] **Step 4: configurations 블록 확장** — 기존 `testImplementation.get().extendsFrom(...)` 위에 다음 추가:
  ```kotlin
  named("benchmarkImplementation") {
      extendsFrom(
          configurations.getByName("implementation"),
          configurations.getByName("compileOnly"),
          configurations.getByName("testImplementation"),
      )
  }
  named("benchmarkRuntimeOnly") {
      extendsFrom(
          configurations.getByName("runtimeOnly"),
          configurations.getByName("testRuntimeOnly"),
      )
  }
  ```
- [ ] **Step 5: dependencies 블록 끝에 benchmark dependency 추가**
  ```kotlin
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
  add("benchmarkImplementation", Libs.jmh_core)
  ```
- [ ] **Step 6: `testImplementation(project(":bluetape4k-testcontainers"))`
  확인** — `infra/cache-lettuce/build.gradle.kts` 에 이미 `testImplementation` 으로 존재하면 그대로 유지 (benchmarkImplementation 이 testImplementation extendsFrom 으로 상속). `testFixtures()` 래핑 금지 (publish 미지원).
- [ ] **Step 7: `src/benchmark/resources/logback-test.xml`
  추가** (Review #4) — `infra/lettuce/src/benchmark/resources/` 에 `logback-test.xml` 이 있으면 그대로 복사, 없으면 `src/test/resources/logback-test.xml` 복사. Testcontainers/Netty/Lettuce DEBUG 로그 억제 필수.

**Done criteria:**

- [ ] `./gradlew :bluetape4k-cache-lettuce:compileBenchmarkKotlin` 성공 (B1 코드 작성 후에 통과 확인 가능).
- [ ] `./gradlew :bluetape4k-cache-lettuce:tasks --all | rg benchmark` 에 `benchmark` task 노출됨.
- [ ] 기존 `:bluetape4k-cache-lettuce:test` 가 회귀 없이 통과.
- [ ] `src/benchmark/resources/logback-test.xml` 존재.

**Complexity rationale:** medium. 템플릿 그대로 복제이지만 configurations / dependencies 합산 시 누락 위험.

---

## Group B — NearCache Benchmark Code

### Task B1: `NearCacheBenchmark.kt` 작성

**Files:**

- Create: `infra/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearCacheBenchmark.kt`

**API 사실관계 (사전 확인 완료):**

- `LettuceNearCache<V: Any>(redisClient, codec, config)` — 3-인자 생성자. codec 기본값 `LettuceBinaryCodecs.default()`.
- `LettuceNearCacheConfig<K: Any, V: Any>` — **2개
  제네릭**. 필드: `cacheName`, `maxLocalSize`(Long), `frontExpireAfterWrite`(Duration), `frontExpireAfterAccess`(Duration?), `redisTtl`(Duration?), `useRespProtocol3`(Boolean = true), `recordStats`(Boolean = false).
- `LettuceNearCache` 의 public 메서드: `get(key)`, `put(key, value)`, `putAll(map)`, `remove(key)`, `clearLocal()`, `close()`.
  **`localInvalidate(key)` 없음 → `clearLocal()` 만 사용 가능**.
- Testcontainers: `RedisServer.Launcher.LettuceLib.getRedisURI(host, port)` 또는 `getRedisClient()`. `RedisServer` 인스턴스화 패턴은 `AbstractLettuceNearCacheTest` 참조.

**Steps:**

- [ ] **Step 1: 사전 확인 — 실제 API 재검증**
    - `infra/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt` 의 public method 목록을 다시 확인 (Read tool). `clearLocal()` 시그니처 확인.
    - `infra/cache-lettuce/src/test/kotlin/.../AbstractLettuceNearCacheTest.kt` (또는 유사) 에서 RedisServer 부트스트랩 패턴 복사.
    - 만약 spec 의 `LettuceNearCacheConfig(cacheName=..., maxLocalSize=..., recordStats=...)` 시그니처와 실제가 다르면 실제 API 에 맞춰 조정.
- [ ] **Step 2: 클래스 골격
  작성** — 단일 `NearCacheBenchmark` 클래스, `@State(Scope.Benchmark)` + `@Threads(1)` + `@BenchmarkMode(Mode.Throughput)` + `@OutputTimeUnit(MILLISECONDS)` + `@Warmup(3, 2s)` + `@Measurement(5, 3s)` + `@Fork(1)`.
- [ ] **Step 3: `@Param` 정의**
    - `@Param("100") var batchSize: Int = 100`
    - `@Param("512", "4096", "16384") var payloadSize: Int = 512`
- [ ] **Step 4: 필드 선언**
    - `lateinit var redisServer: RedisServer`
    - `lateinit var redisClient: RedisClient`
    - `lateinit var cache: LettuceNearCache<String>`
    - `val counter = AtomicLong()`
    - `lateinit var warmKey, l2WarmKey, warmValue: String`
- [ ] **Step
  5: `@Setup(Level.Trial)`** — Redis 기동, RedisClient (RESP3 ClientOptions), `LettuceNearCache(redisClient, StringCodec.UTF8, config)` 생성 (`maxLocalSize = 100_000`, `recordStats = true`, `useRespProtocol3 = true`). warmKey/l2WarmKey 사전 적재 (spec §3.3).
- [ ] **Step 6: `@Setup(Level.Iteration)`** — `warmKey` L1 보충 (안전망).
- [ ] **Step
  7: `@TearDown(Level.Trial)`** — `cache.close()`, `redisClient.shutdown()` (RedisServer 는 ShutdownQueue 자동 정리).
- [ ] **Step 8: 벤치마크 메서드 6 개 작성**
    - `@Benchmark fun l1Hit(): String? = cache.get(warmKey)`
    - `@Benchmark fun l2Hit(): String? { cache.clearLocal(); return cache.get(l2WarmKey) }` (clearLocal 호출 자체가 측정에 포함됨 — Benchmark.md 의 Analysis 섹션에서 명시).
    - `@Benchmark fun l2Miss(): String? = cache.get("miss-${counter.incrementAndGet()}")`
    - `@Benchmark fun putSingle() { cache.put("k-${counter.incrementAndGet()}", warmValue) }`
    - `@Benchmark fun putAll() { ... batchSize 만큼 map 생성 후 cache.putAll(map) }`
    - `removeSingle` — **spec §3.3 Review M1 준수 / Review #2
      수정**: `@State(Scope.Thread)` inner class `owner!!` 패턴은 JMH auto-injection 미지원으로 컴파일 실패. **단순 패턴 사용**:
      ```kotlin
      private lateinit var currentRemoveKey: String
  
      @Setup(Level.Invocation)
      fun prepareRemoveKey() {
          currentRemoveKey = "r-${counter.incrementAndGet()}"
          cache.put(currentRemoveKey, warmValue)   // Setup — JMH 측정값 제외
      }
  
      @Benchmark
      fun removeSingle() = cache.remove(currentRemoveKey)  // remove 경로만 측정
      ```
      JMH `@Setup(Level.Invocation)` 비용은 측정값에서 자동 제외됨. inline put + remove 패턴 금지.
- [ ] **Step 9: KDoc + KLogging
  추가** — 클래스 상단에 한국어 KDoc (시나리오, Docker 사전조건, JMH 옵션 명시). `companion object : KLogging()` 추가 (bluetape4k-patterns 필수). `LettuceCodecBenchmark` 에도 companion object 패턴 존재하는지 확인 후 일치시킴.
- [ ] **Step 10: ide_diagnostics 통과** — import 누락/Deprecated 경고 0.

**Done criteria:**

- [ ] `./gradlew :bluetape4k-cache-lettuce:compileBenchmarkKotlin` 성공 — 0 errors / 0 warnings.
- [ ] `lsp_diagnostics` 클린.
- [ ] `removeSingle` 의 inline put 금지 규칙 준수 (Spec §3.3 Review M1).
- [ ] `localInvalidate` 호출 없음 (Spec §3.3 Review C1 — public API 미존재).
- [ ] `LettuceNearCacheConfig` 시그니처 실제 코드 기준 (`<K, V>` 2-제네릭, `maxLocalSize`/`recordStats`/`useRespProtocol3`).

**Complexity
rationale:** high. JMH state lifecycle (Trial/Iteration/Invocation) 정확성, RESP3 RedisClient ClientOptions, removeSingle pre-put 분리, 실 API 재확인.

---

## Group C — Run Benchmarks and Capture Results

### Task C1: `:bluetape4k-lettuce:benchmark` 실행

**Files:** (실행 산출물만, 커밋 대상 아님)

- Output capture: `.omc/research/lettuce-codec-benchmark-2026-04-27.txt` (or similar)

**Pre-conditions:**

- 로컬 머신에 충분한 CPU/RAM 확보 (백그라운드 프로세스 최소화).
- 측정 환경 정보 사전 캡처: `sw_vers`, `system_profiler SPHardwareDataType | rg "Chip\|Memory"`, `java -version`, gradle 버전.

**Steps:**

- [ ] **Step 1: 환경 정보 수집** (D1 의 "Benchmark Environment" 섹션용)
  ```bash
  sw_vers; system_profiler SPHardwareDataType | rg -i "chip|memory|cores"
  java -version 2>&1
  ./gradlew --version
  ```
- [ ] **Step 2: 벤치마크 실행 (run_in_background, 약 10–20 분)**
  ```bash
  ./gradlew :bluetape4k-lettuce:benchmark 2>&1 | tee .omc/research/lettuce-codec-benchmark-2026-04-27.txt
  ```
- [ ] **Step 3: JMH 결과 표 식별** — 출력 끝부분의 `Benchmark ... Mode Cnt Score Error Units` 표 그대로 D1 에서 사용.
- [ ] **Step 4: 13 codec 모두 결과가 있는지
  확인** (jackson3, fastjson2, fory, kryo, jdk, lz4Fory, lz4Kryo, zstdFory, zstdKryo, fastFory, lz4FastFory, zstdFastFory, gzipFastFory).

**Done criteria:**

- [ ] JMH raw 출력 파일 저장 완료.
- [ ] 13 codec 시나리오 결과 누락 없음.
- [ ] 환경 정보 캡처 완료.

**Complexity rationale:** medium. 단순 실행이지만 시간 소요 (10–20분) + 결과 누락 검사 필요.

---

### Task C2: `:bluetape4k-redisson:benchmark` 실행

**Files:**

- Output: `.omc/research/redisson-codec-benchmark-2026-04-27.txt`

**Steps:**

- [ ] **Step 1: 벤치마크 실행 (run_in_background)**
  ```bash
  ./gradlew :bluetape4k-redisson:benchmark 2>&1 | tee .omc/research/redisson-codec-benchmark-2026-04-27.txt
  ```
- [ ] **Step 2: 결과 표 식별 + 13 codec 확인** (lettuce 와 동일 codec 셋).

**Done criteria:**

- [ ] JMH raw 출력 파일 저장 완료.
- [ ] 13 codec 결과 모두 존재.

**Complexity rationale:** medium. C1 과 동일 사유.

**병렬 실행 노트:** C1 과 C2 를 동시에 돌리면 측정값에 상호 간섭이 생기므로 **순차 실행 권장**. 단 측정 환경 (머신) 자체가 동일해야 cross-module 비교가 가능.

---

### Task C3: `:bluetape4k-cache-lettuce:benchmark` 실행 (Docker 필요)

**Pre-conditions:**

- Docker Desktop 실행 중.
- B1 완료 (NearCacheBenchmark.kt 컴파일 성공).
- A1 완료 (benchmark sourceset 활성).

**Files:**

- Output: `.omc/research/cache-lettuce-nearcache-benchmark-2026-04-27.txt`

**Steps:**

- [ ] **Step 1: Docker 데몬 확인**
  ```bash
  docker info 2>&1 | rg -i "server version" || echo "Docker not running"
  ```
- [ ] **Step 2: 벤치마크 실행 (Param 조합 = 6 시나리오 × 3 payloadSize × 1 batchSize = 18 측정)**
  ```bash
  ./gradlew :bluetape4k-cache-lettuce:benchmark 2>&1 | tee .omc/research/cache-lettuce-nearcache-benchmark-2026-04-27.txt
  ```
  예상 소요 시간: 18 × (3×2s warmup + 5×3s measurement) ≈ 약 7분 + Trial setup × 18 (Redis 컨테이너 1개 재사용이면 단발) ≈ 총 10–15분.
- [ ] **Step 3: 결과 표 식별** — `Benchmark (batchSize) (payloadSize) Mode Cnt Score Error Units` 컬럼 포함 표.
- [ ] **Step 4: 모든 시나리오 + payload 조합 결과 확인** (l1Hit/l2Hit/l2Miss/putSingle/putAll/removeSingle × 512/4096/16384 = 18 행).

**Done criteria:**

- [ ] JMH raw 출력 파일 저장 완료.
- [ ] 18 측정 행 누락 없음.
- [ ] Redis 컨테이너 정상 종료 (gradle daemon log 에 ShutdownQueue cleanup 메시지).

**Complexity rationale:** high. Docker 의존성, JMH state lifecycle 검증, 측정 무결성 (l2Hit 가 실제로 Redis 왕복 수행되었는지 latency 합리성 검증).

---

## Group D — Write Benchmark.md Files

각 D-task 는 **EN (.md) + KO (.ko.md) 페어를 동일 commit 에 작성** (Spec §3.5).

### Task D1: `infra/lettuce/Benchmark.md` + `Benchmark.ko.md`

**Pre-conditions:** C1 완료.

**Reference style:** `utils/idgenerators/Benchmark.md` + `Benchmark.ko.md`.

**Files:**

- Create: `infra/lettuce/Benchmark.md`
- Create: `infra/lettuce/Benchmark.ko.md`

**Steps:**

- [ ] **Step 1: idgenerators 템플릿
  분석** — Read `utils/idgenerators/Benchmark.md` 와 `Benchmark.ko.md` 둘 다 읽고 섹션 골격 / colored span 인코딩 / 표 헤더를 확인.
- [ ] **Step 2: 결과 테이블 데이터 추출** — C1 raw output 에서 13 codec 의 `Score` 값을 ops/ms 단위로 추출 → 정렬 (빠른 순).
- [ ] **Step 3: Performance Chart 생성** — 1위 = 40 블록 기준 비례 정수 반올림. 색상 8종 순환 (idgenerators 와 동일):
    - `#0EA5E9 sky`, `#EC4899 pink`, `#10B981 emerald`, `#F97316 orange`, `#EAB308 yellow`(black text), `#8B5CF6 violet`, `#EF4444 red`, `#6366F1 indigo`.
    - **codec 별 색상 매핑은 D2 와 동일하게
      유지** (예: `fory → sky`, `kryo → pink`, `jackson3 → emerald` 등) → 공유 매핑 표를 plan 작업 노트에 기록.
- [ ] **Step 4: 섹션 작성 (영문)**
    1. Title + bilingual switch link `[한국어](./Benchmark.ko.md) | English`
    2. Measurement Overview — 13 codec, payload 데이터 종류, JMH 옵션
    3. How to Run — `./gradlew :bluetape4k-lettuce:benchmark`
    4. Results
        - 4.1 Summary Table (codec / score / units / 상대 비율)
        - 4.2 Detailed Results — JMH raw 코드 블록
        - 4.3 Performance Chart — colored `<span>` 바 차트
    5. Performance Analysis — 3–5 Key Findings:
        - Binary vs JSON (fory 계열 vs jackson3/fastjson2)
        - 압축 효과 (lz4 / zstd / none) 처리량 vs 사이즈 trade-off
        - FastFory (SCHEMA_CONSISTENT) vs 일반 Fory
        - Kryo vs Fory
    6. Recommendations — 시나리오 / 추천 codec / 근거 표
    7. Conclusion — 1단락 + 권장 default codec
    8. Benchmark Environment — JVM/Kotlin/JMH 버전, Hardware (C1 Step 1 캡처본)
- [ ] **Step 5: KO 1:1 번역** — 동일 표/숫자/색상 그대로, 본문만 한국어. 상단 switch link 는 `한국어 | [English](./Benchmark.md)`.
- [ ] **Step 6: GitHub 미리보기 + IntelliJ Markdown preview 양쪽 렌더 확인** (스크린샷 또는 육안 확인).
- [ ] **Step 7: Vega-Lite 미사용 확인** (CLAUDE.md 규칙).

**Done criteria:**

- [ ] EN + KO 두 파일 모두 작성 완료, 동일 commit 단위.
- [ ] 13 codec 모두 표/차트에 포함.
- [ ] colored span 막대 길이 = round (40 × score / max_score).
- [ ] bilingual switch link 양방향 동작.

**Complexity rationale:** medium. 데이터 추출 + 색상 매핑 + 영/한 동시 작성.

---

### Task D2: `infra/redisson/Benchmark.md` + `Benchmark.ko.md`

**Pre-conditions:** C2 완료, D1 완료 (codec 색상 매핑 재사용).

**Files:**

- Create: `infra/redisson/Benchmark.md`
- Create: `infra/redisson/Benchmark.ko.md`

**Steps:**

- [ ] **Step 1: D1 의 codec 색상 매핑 재사용** — 동일 codec 은 동일 색상.
- [ ] **Step 2: Performance Chart + Summary Table 작성** (D1 과 동일 구조).
- [ ] **Step 3: 추가 섹션 — "Notes on ByteBuf vs ByteBuffer"** — Redisson 은 Netty `ByteBuf` 직접 사용, GC 압력 차이.
- [ ] **Step 4: "Cross-Module
  Note"** 1단락 — 동일 codec 임에도 lettuce vs redisson 사이 미세 격차 발생 가능 사유 (buffer 구현, allocation 패턴, Netty 의존성).
- [ ] **Step 5: KO 1:1 번역.**

**Done criteria:**

- [ ] EN + KO 두 파일 작성 완료.
- [ ] D1 과 codec 색상 매핑 일치 (독자 간 비교 가능).
- [ ] Cross-Module Note 단락 포함.

**Complexity rationale:** medium. D1 과 동일하나 cross-module note 추가.

---

### Task D3: `infra/cache-lettuce/Benchmark.md` + `Benchmark.ko.md`

**Pre-conditions:** C3 완료.

**Files:**

- Create: `infra/cache-lettuce/Benchmark.md`
- Create: `infra/cache-lettuce/Benchmark.ko.md`

**Steps:**

- [ ] **Step 1: 6 시나리오 × 3 payload Summary Table 작성** — 행 = 시나리오, 열 = payload 512/4096/16384.
- [ ] **Step 2: Performance Chart** — payloadSize=4096 기준으로 6 시나리오 colored bar (가독성). 다른 payload 는 별도 표 또는 별도 차트.
- [ ] **Step 3: Performance Analysis 핵심 축**
    - L1 vs L2 latency (Caffeine 메모리 hit vs Redis 왕복) — 수치 비율 (예: L1 이 L2 보다 N× 빠름)
    - Read vs Write 비대칭 (l1Hit vs putSingle)
    - L2 miss 의 양쪽-부정 비용 (negative cache 영향)
    - putAll 의 amortized per-entry 처리량 (batchSize=100 기준 entry/ms 환산표)
    - payloadSize 변화의 L2 처리량 영향 (16KB → RTT + serialization 비용)
- [ ] **Step 4: Recommendations** — hit ratio 임계값 이상일 때 Caffeine-only 대비 NearCache 의 장점.
- [ ] **Step 5: Future Work 섹션**
    - "LettuceSuspendNearCache 별도 측정" (본 issue 제외, 별도 issue 필요)
    - "더 큰 페이로드 (64KB+) / 다양 batchSize / multi-thread @Threads 측정"
    - "L1 hit ratio 자동 측정 (recordStats Caffeine API 활용)"
- [ ] **Step 6: How to Run — Docker 사전조건 명시**
  ```
  Prerequisites: Docker Desktop running (Testcontainers Redis 7+)
  ./gradlew :bluetape4k-cache-lettuce:benchmark
  ```
- [ ] **Step 7: Benchmark Environment — Docker / Redis 이미지 버전 추가.**
- [ ] **Step 8: KO 1:1 번역.**

**Done criteria:**

- [ ] EN + KO 두 파일 작성 완료.
- [ ] 6 시나리오 × 3 payload 모두 표에 포함.
- [ ] Future Work 섹션에 LettuceSuspendNearCache 명시 (Spec §3.4).
- [ ] How to Run 에 Docker 요구사항 명시 (Spec §3.1 운영 메모).

**Complexity rationale:** medium. 다차원 (시나리오 × payload) 데이터 + Future Work + Docker 사전조건.

---

## Group E — README Updates

### Task E1: `infra/lettuce/README.md` + `README.ko.md` Performance 링크

**Pre-conditions:** D1 완료.

**Files:**

- Modify: `infra/lettuce/README.md`
- Modify: `infra/lettuce/README.ko.md`

**Steps:**

- [ ] **Step 1:** README 끝부분 또는 적절한 섹션 (Features 다음 등)에 다음 추가:
  ```markdown
  ## Performance

  See [Benchmark.md](./Benchmark.md) for codec throughput measurements.
  ```
  KO 버전:
  ```markdown
  ## 성능

  코덱 처리량 측정값은 [Benchmark.ko.md](./Benchmark.ko.md) 를 참조하세요.
  ```
- [ ] **Step 2:** 페어 일관성 확인.

**Done criteria:**

- [ ] 두 README 모두 Benchmark.md 링크 추가.
- [ ] 링크 동작 확인.

**Complexity rationale:** low.

---

### Task E2: `infra/redisson/README.md` + `README.ko.md`

E1 과 동일 패턴. **Pre-conditions:** D2 완료.

**Done criteria:**

- [ ] 두 README 에 Benchmark 링크 추가.

---

### Task E3: `infra/cache-lettuce/README.md` + `README.ko.md` + Mermaid 갱신

**Pre-conditions:** D3 완료.

**Files:**

- Modify: `infra/cache-lettuce/README.md`
- Modify: `infra/cache-lettuce/README.ko.md`

**Steps:**

- [ ] **Step 1:** Performance 섹션 추가 (E1 패턴).
- [ ] **Step
  2:** 기존 NearCache 아키텍처 Mermaid 다이어그램 점검 — L1/L2 invalidation 경로 (RESP3 CLIENT TRACKING → push → local invalidate) 가 명시되어 있는지 확인.
    - 누락 시 Mermaid sequence/flowchart 갱신.
    - 영문/한국어 모두 동기화.

**Done criteria:**

- [ ] 두 README Performance 링크 추가.
- [ ] Mermaid 다이어그램에 invalidation 경로 표현.
- [ ] EN/KO 다이어그램 동기화.

**Complexity rationale:** low. (단 Mermaid 갱신 필요 시 medium 으로 상승).

---

## Group F — Validation

### Task F1: DoD 검증

**Pre-conditions:** A1~E3 완료.

**Steps:**

- [ ] **Step 1: 컴파일 검증**
  ```bash
  ./gradlew :bluetape4k-cache-lettuce:compileBenchmarkKotlin
  ./gradlew :bluetape4k-cache-lettuce:test
  ./gradlew :bluetape4k-lettuce:test
  ./gradlew :bluetape4k-redisson:test
  ```
- [ ] **Step 2: detekt 통과 확인**
  ```bash
  ./gradlew :bluetape4k-cache-lettuce:detekt :bluetape4k-lettuce:detekt :bluetape4k-redisson:detekt
  ```
- [ ] **Step 3: code-reviewer 에이전트 1회
  실행** — `oh-my-claudecode:code-reviewer` 또는 `pr-review-toolkit:code-reviewer`. HIGH/CRITICAL 0 까지 반복.
- [ ] **Step 4: bilingual switch link 양방향 동작 확인** — 6 개 .md 파일.
- [ ] **Step 5: GitHub 미리보기 시뮬레이션** — colored span 바 차트가 IntelliJ + GitHub 양쪽에서 렌더되는지 확인.
- [ ] **Step 6: Vega-Lite 미사용 grep 확인**
  ```bash
  rg -l "vega|vegalite|vega-lite" infra/lettuce/Benchmark.md infra/redisson/Benchmark.md infra/cache-lettuce/Benchmark.md
  # 결과: 0 hits (CLAUDE.md 규칙)
  ```
- [ ] **Step 7: `/wiki-update` 1회 실행** — spec/plan 신규 작성에 의함 (Spec §6 PR DoD).
- [ ] **Step 8: 작업 위치 확인** — 모든 변경이 `.worktrees/docs-benchmark-results/` 안에서 이루어졌는지.

**Done criteria:** Spec §6 의 DoD Checklist 모든 항목 통과.

**Complexity rationale:** medium. 다항목 점검.

---

## DoD Checklist (from Spec §6)

### 코드

- [ ] `infra/cache-lettuce/build.gradle.kts` 수정 (A1)
- [ ] `infra/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearCacheBenchmark.kt` 작성 (B1)
- [ ] `./gradlew :bluetape4k-cache-lettuce:benchmark` 로컬 1회 성공 (C3, Docker 필요)
- [ ] JMH raw 출력 캡처 → 세 모듈 `Benchmark.md` 의 "Detailed Results" 에 붙여넣기 (D1/D2/D3)
- [ ] `:bluetape4k-lettuce:benchmark` · `:bluetape4k-redisson:benchmark` 재실행 (C1/C2)

### 문서

- [ ] `infra/lettuce/Benchmark.md` + `Benchmark.ko.md` (D1, 페어 동일 commit)
- [ ] `infra/redisson/Benchmark.md` + `Benchmark.ko.md` (D2, 페어 동일 commit)
- [ ] `infra/cache-lettuce/Benchmark.md` + `Benchmark.ko.md` (D3, 페어 동일 commit)
- [ ] bilingual switch link 검증 (F1)
- [ ] colored span 바 차트 GitHub + IntelliJ 양쪽 렌더 확인 (F1)
- [ ] **Vega-Lite 미사용** (F1 grep 검증)
- [ ] `infra/cache-lettuce/README.md` + `README.ko.md` Mermaid 다이어그램 최신화 (E3)

### 모듈 README 동기화

- [ ] `infra/lettuce/README.md`(+ko) Performance 섹션 (E1)
- [ ] `infra/redisson/README.md`(+ko) Performance 섹션 (E2)
- [ ] `infra/cache-lettuce/README.md`(+ko) Performance 섹션 (E3)

### 빌드 / 검증

- [ ] `./gradlew :bluetape4k-cache-lettuce:compileBenchmarkKotlin` 성공 (F1)
- [ ] `./gradlew :bluetape4k-cache-lettuce:test` 회귀 없음 (F1)
- [ ] `./gradlew detekt` 통과 (F1)
- [ ] `code-reviewer` HIGH/CRITICAL 0 (F1)

### PR

- [ ] `feat: docs/benchmark-results — issue #184 벤치마크 결과 문서화` (Korean prefix)
- [ ] PR 본문에 측정 환경 (JVM/Kotlin/Hardware), Docker 요구사항, 재실행 명령 명시
- [ ] `/wiki-update` 1회 실행 (F1)
- [ ] `.worktrees/docs-benchmark-results/` 안에서 작업

---

## Risks & Mitigations (from Spec §7)

| 위험                                          | 완화                                                                                                     |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Docker 미설치 → C3 실행 불가                  | C3 실행 전 `docker info` 사전 확인. "How to Run" 에 Docker 사전조건 명시. CI 자동 실행 본 issue 범위 외. |
| `localInvalidate` 미존재                      | B1 Step 1 에서 실 API 재확인. `clearLocal()` 만 사용.                                                    |
| 측정값 머신 의존성                            | Benchmark Environment 섹션에 CPU/RAM/OS 정확히 기재. 본문에 "절대값보다 모듈 내 상대 순위" 명시.         |
| C1/C2 측정 머신 차이                          | C1, C2, C3 모두 동일 머신 + 동일 시점 측정. 백그라운드 프로세스 최소화                                   |
| `LettuceNearCacheConfig` 시그니처 변경 가능성 | B1 Step 1 에서 실 API 재확인. spec 의 의사코드는 참고용일 뿐.                                            |
| C1/C2 결과 codec 격차 해석 부담               | D2 의 "Cross-Module Note" 단락으로 ByteBuf vs ByteBuffer 차이 사전 설명.                                 |
| removeSingle inline put 잔존                  | B1 Step 8 에서 `@State(Scope.Thread) RemoveCarrier` 패턴으로 분리. Spec §3.3 Review M1 명시.             |

---

## Implementation Notes

- **CLAUDE.md
  준수:** Korean commit prefix, README ko/en 동기화, Mermaid 만 README 본문 (Benchmark.md 본문은 colored span only), no Vega-Lite, worktree 작업.
- **bluetape4k-patterns:** KLogging companion, immutable, requireNotBlank, etc.
- **단위 일관:** 모든 벤치마크 `Mode.Throughput` + `MILLISECONDS` (Spec §3.2).
- **codec 색상 매핑 공유:** D1 에서 결정한 매핑을 D2 가 그대로 재사용 (독자 간 비교 가능).
- **EN+KO 동시 commit:** Spec §3.5 — drift 방지.
