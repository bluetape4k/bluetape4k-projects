# Benchmark Results Documentation — Design Spec

- **Issue**: #184
- **Branch / Worktree**: `docs/benchmark-results` → `.worktrees/docs-benchmark-results/`
- **Date**: 2026-04-27
- **Style Reference**: `utils/idgenerators/Benchmark.md` + `Benchmark.ko.md` (bilingual paired)

---

## 1. Problem Statement

`infra/lettuce` 와 `infra/redisson` 모듈에는 codec (직렬화/압축) 처리량을 측정하는 JMH 벤치마크 코드 (`LettuceCodecBenchmark`, `RedissonCodecBenchmark`)가 존재하지만, 측정 결과를 정리한 문서가 없다. 또한 `infra/cache-lettuce` 의 핵심 기능인 NearCache (L1=Caffeine + L2=Redis RESP3 invalidation)는 벤치마크 sourceset 자체가 부재하여, 사용자가 NearCache 채택 여부를 판단할 수 있는 정량 지표가 없다. 본 spec 은 (1) 두 codec 벤치마크의 결과 문서를 영/한 페어 스타일로 작성하고, (2) cache-lettuce 에 NearCache 벤치마크 코드와 결과 문서를 신규 추가하여 세 모듈의 벤치마크 정보를 동등한 품질로 정렬한다.

---

## 2. Scope

| Module                | Deliverable                                                                                 | 상태               |
|-----------------------|---------------------------------------------------------------------------------------------|--------------------|
| `infra/lettuce`       | `Benchmark.md` + `Benchmark.ko.md` (코드는 이미 존재)                                       | 신규               |
| `infra/redisson`      | `Benchmark.md` + `Benchmark.ko.md` (코드는 이미 존재)                                       | 신규               |
| `infra/cache-lettuce` | `build.gradle.kts` 수정 + `NearCacheBenchmark.kt` 신규 + `Benchmark.md` + `Benchmark.ko.md` | 신규 (코드 + 문서) |

총 산출물: `.md` 6개 · `.kt` 1개 · `build.gradle.kts` 1개 수정

비범위 (Out of scope):

- `LettuceSuspendNearCache` 벤치마크는 **본 issue 제외** (설계 결정 4 참조)
- 다른 cache 모듈 (`cache-redisson`, `cache-hazelcast`)의 NearCache 벤치마크
- JMH 옵션 튜닝 (현재 lettuce/redisson 의 `Warmup 3×2s, Measurement 5×3s, Fork 1` 설정 그대로 사용)
- CI 자동 실행 (현재 nightly 워크플로우에 codec 벤치마크 미포함, 본 spec 도 추가하지 않음)

---

## 3. Design Decisions

### 3.1 NearCache 벤치마크의 Redis bootstrap 전략

**선택 대안 비교:**

| 대안                                                            | 장점                                                                                                                                                   | 단점                                                                                                          |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| **(A) Testcontainers `RedisServer`** (선택)                     | 기존 cache-lettuce 테스트와 동일한 부트스트랩(`AbstractLettuceNearCacheTest`)을 그대로 재사용. Redis 7+ RESP3 정식 지원. ShutdownQueue 정리 패턴 일관. | Docker 데몬 필요 → 로컬 Docker 미설치 환경에서는 실행 불가. JMH `@Setup(Level.Trial)` 1회 기동에 5–10초 추가. |
| (B) embedded redis (`com.github.codemonstur:embedded-redis` 등) | Docker 불필요, 빠른 기동                                                                                                                               | RESP3 CLIENT TRACKING 미지원/불완전 → NearCache 무효화 경로 미동작. 실측이 의미 없음.                         |

**결정: (A) Testcontainers `RedisServer.Launcher.LettuceLib.getRedisURI(...)` 재사용.**

근거: NearCache 의 핵심 동작 (L2 hit 시 invalidation)은 RESP3 `CLIENT TRACKING` 에 의존한다. Embedded Redis 는 이 경로를 신뢰성 있게 재현하지 못해 측정값 자체가 무의미해진다. 실행 비용 (Docker 기동)은 codec 벤치마크와 달리 Redis 의존이 본질적이므로 수용 가능하다. JMH 의 `@Setup(Level.Trial)` 1회만 기동하면 전체 벤치마크 동안 재사용된다.

운영 메모: `Benchmark.md` 의 "How to Run" 섹션에 Docker Desktop 필요성을 명시한다.

### 3.2 NearCache 측정 모드 (BenchmarkMode)

**결정: 모든 시나리오 `Mode.Throughput`, `OutputTimeUnit(MILLISECONDS)` 통일** — codec 벤치마크와 일관.

근거:

- `getAll`/`putAll` 같은 묶음 연산은 항목 수가 일정하므로 throughput 으로도 비교 가능 (파라미터 `batchSize` 로 명시).
- AverageTime 모드를 섞으면 단위 (ops/ms vs μs/op)가 달라져 colored bar chart 비교가 어렵다.
- idgenerators 벤치마크와 동일 모드를 유지하면 향후 통합 비교 표 작성이 쉽다.

### 3.3 NearCache L1 hit 시나리오의 사전 워밍 및 L2 hit 강제

**결정:**

- `l1Hit`: `@Setup(Level.Iteration)` 에서 `warmKey` 를 put + get 으로 L1 채움 → 반복 `cache.get(warmKey)` 는 L1 적중.
- `l2Hit`: `@Setup(Level.Invocation)` 에서 `cache.clearLocal()` 호출 → 전체 L1 을 비운 뒤 `cache.get(l2WarmKey)` → L2 적중. `l2WarmKey` 는 Trial 셋업에서 단 1회 put 해 Redis 에만 존재하게 한다 (Trial 셋업 이후 clearLocal 로 L1 에서만 제거).
  **`localInvalidate(key)` 가 public API 에 없으므로 `clearLocal()` 을
  사용한다** — 측정 전 오버헤드는 `@Setup(Level.Invocation)` 범위라 JMH 가 제외함.
- L2 miss: 매 invocation 마다 새 키 (`"miss-${counter++}"`).
- put/putAll/remove: 매 invocation 마다 새 키 → RESP3 invalidation self-loop 방지.
- `removeSingle`: `@Setup(Level.Invocation)` 에서 `removeKey` 를 미리 put → `@Benchmark` 는 `cache.remove(removeKey)` 만 측정. 오염 방지.

근거:

- `LettuceNearCache.localInvalidate(key)` public API 미존재 (Review C1). `clearLocal()` 대안 사용.
- `removeSingle` 의 inline put 제거로 remove 경로만 측정 (Review M1).
- `@Setup(Level.Invocation)` 는 JMH 가 측정값에서 제외하므로 clearLocal/pre-put 비용이 결과에 안 들어간다.
- L1 캐시 크기 `maxLocalSize = 100_000` 으로 eviction 없이 워밍 키 유지 보장.

### 3.4 측정 범위: 동기 vs 코루틴

**결정: `LettuceNearCache`(blocking) 만 측정. `LettuceSuspendNearCache` 는 본 issue 에서 제외.**

근거:

- JMH 에서 suspend fun 측정은 `runBlocking` 래핑을 강제하며, 그 자체가 nontrivial overhead 를 추가해 측정값 해석이 흐려진다.
- 코루틴 벤치마크는 `kotlinx-benchmark` 의 suspend 지원 (`@State` + suspend `@Benchmark`) 또는 `kotlinx-coroutines-test` 통합이 필요하며, 본 issue 의 목적 (문서화)을 넘어선 R&D 가 든다.
- 추후 별도 issue 로 분리 — `Benchmark.md` 의 "Future Work" 섹션에 명시한다.

### 3.5 이중 언어 파일 작성 순서

**결정: EN (`Benchmark.md`) 을 먼저 작성·확정 → 그 직후 동일 PR 에서 KO (`Benchmark.ko.md`) 1:1 번역 → 두 파일을 같은 commit 에 포함.**

근거:

- 동시 commit 은 README.md / README.ko.md 동기화 규칙 (`CLAUDE.md`)과 일관.
- 측정값은 양쪽 파일에서 동일한 표/숫자/색상이어야 하므로 KO 가 EN 의 후행 번역으로 작성되는 편이 drift 위험을 줄인다.
- 파일 두 개를 다른 commit 에 두면 중간 상태에서 문서 불일치가 노출된다.

---

## 4. NearCache Benchmark Class Outline (의사코드)

`infra/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearCacheBenchmark.kt`:

```kotlin
package io.bluetape4k.cache.nearcache.benchmark

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.ProtocolVersion
import io.lettuce.core.ClientOptions
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * LettuceNearCache(L1=Caffeine, L2=Redis RESP3) 처리량 벤치마크.
 *
 * 시나리오:
 *  - l1Hit        : 사전 워밍된 키 단건 get → L1 적중
 *  - l2Hit        : clearLocal() 후 get → L2 적중 + L1 재충전
 *  - l2Miss       : 미존재 키 get → null (양쪽 음성)
 *  - putSingle    : write-through put 1건
 *  - putAll       : batchSize 건 묶음 put
 *  - removeSingle : 사전 put 된 키 remove (L1+L2)
 *
 * 주의: L2 hit/miss 측정은 Testcontainers Redis 필요 (Docker 데몬 필수)
 */
@Threads(1)   // 단일 스레드 - NearCache 는 L1 공유 캐시, 멀티 스레드 시 l1/l2 측정 상태 오염
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class NearCacheBenchmark {

    @Param("100")      // putAll batchSize
    var batchSize: Int = 100

    @Param("512", "4096", "16384")   // 페이로드 크기 (bytes) — L2 RTT 영향 가시화
    var payloadSize: Int = 512

    private lateinit var redisServer: RedisServer
    private lateinit var redisClient: RedisClient
    private lateinit var cache: LettuceNearCache<String>

    private val counter = AtomicLong()
    private lateinit var warmKey: String           // l1Hit 용 안정 키 (Trial 동안 불변)
    private lateinit var l2WarmKey: String         // l2Hit 용 Redis-only 사전 적재 키
    private lateinit var warmValue: String
    private lateinit var removeKey: String         // removeSingle 용 pre-put 키

    @Setup(Level.Trial)
    fun setupTrial() {
        warmValue = "A".repeat(payloadSize)
        redisServer = RedisServer.Launcher.redis   // Testcontainers, ShutdownQueue 에 등록
        redisClient = RedisClient.create(
            RedisServer.Launcher.LettuceLib.getRedisURI(redisServer.host, redisServer.port)
        ).apply {
            options = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build()
        }
        cache = LettuceNearCache(
            redisClient = redisClient,
            codec = StringCodec.UTF8,
            config = LettuceNearCacheConfig(
                cacheName = "bench-near-cache",
                maxLocalSize = 100_000,    // eviction 없이 warmKey 유지
                recordStats = true,        // L1 hit/miss 비율 수집 필수
            )
        )
        // l1Hit 용 warmKey: Trial 전체 동안 L1+L2 에 존재
        warmKey = "warm-stable"
        cache.put(warmKey, warmValue)

        // l2Hit 용 l2WarmKey: Redis(L2) 에만 존재하도록 put 후 L1 클리어
        l2WarmKey = "l2warm-stable"
        cache.put(l2WarmKey, warmValue)
        cache.clearLocal()                // L1 비움 → l2WarmKey 는 Redis 에만 남음
        // l1Hit warmKey 도 L1 에서 사라졌으므로 다시 채움
        cache.put(warmKey, warmValue)
        cache.get(warmKey)                // L1 채우기
    }

    // l1Hit: L1 에 있는 warmKey 는 Iteration 간에도 evict 안 됨 (maxLocalSize 충분)
    @Setup(Level.Iteration)
    fun warmupIterationL1() {
        // warmKey 가 L1 에서 혹시 사라졌을 경우 보충 (안전망)
        if (cache.get(warmKey) == null) {
            cache.put(warmKey, warmValue)
        }
    }

    // l2Hit: 매 invocation 전 clearLocal() 로 L1 비움 → get(l2WarmKey) 는 L2 hit
    @Setup(Level.Invocation)
    fun clearL1ForL2Hit() {
        // NOTE: 이 Setup 은 모든 @Benchmark 메서드 전에 실행됨.
        // l2Hit 가 아닌 메서드는 이 overhead 를 받지만 @Setup 비용은 측정 외.
        // l1Hit 는 clearL1 후 @Setup(Level.Iteration) 의 warmupIterationL1 재보충을 기대하지 않으므로
        // 별도 benchmark 클래스로 분리하거나 l2Hit 전용 @State 캐리어를 쓰는 것이 이상적.
        // 실용적 절충: clearL1ForL2Hit 를 l1Hit 에서는 @TearDown(Level.Invocation) 으로 L1 재보충.
        // → 구현 시 선택: (a) 단일 클래스+Invocation 재보충, (b) l1Hit/l2Hit 분리 클래스.
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        cache.close()
        redisClient.shutdown()
        // redisServer 는 ShutdownQueue 가 자동 정리
    }

    // ---- 벤치마크 메서드 ----

    @Benchmark
    fun l1Hit(): String? = cache.get(warmKey)   // L1 적중: Caffeine 메모리 get

    @Benchmark
    fun l2Hit(): String? {
        cache.clearLocal()                       // L1 비움 (Level.Invocation Setup 에서 처리 가능)
        return cache.get(l2WarmKey)             // L2 hit: Redis 왕복 + L1 재충전
    }

    @Benchmark
    fun l2Miss(): String? =
        cache.get("miss-${'$'}{counter.incrementAndGet()}")   // 미존재 키

    @Benchmark
    fun putSingle() {
        cache.put("k-${'$'}{counter.incrementAndGet()}", warmValue)
    }

    @Benchmark
    fun putAll() {
        val n = counter.addAndGet(batchSize.toLong())
        val batch = (0 until batchSize).associate { i -> "b-${'$'}{n - i}" to warmValue }
        cache.putAll(batch)
    }

    // removeSingle: @Setup(Level.Invocation) 에서 미리 put → @Benchmark 는 remove 만
    @State(Scope.Thread)
    open class RemoveState {
        lateinit var key: String
    }

    @Benchmark
    fun removeSingle(@SuppressWarnings("unused") state: RemoveState): Unit {
        // 실제 구현 시 @State 분리 또는 setupInvocation 에서 put 후 remove 만 측정
        // 의사코드: cache.remove(prePutKey)
        val k = "r-${'$'}{counter.incrementAndGet()}"
        cache.put(k, warmValue)     // NOTE: @Setup(Level.Invocation) 으로 분리 권장 (Review M1)
        cache.remove(k)
    }
}
```

> **구현 시 주의사항:**
> - `l1Hit` 과 `l2Hit` 가 `clearLocal()` 을 공유하는 문제 → 분리 benchmark class 또는 `@State(Scope.Thread)` 캐리어로 해결.
> - `removeSingle` 의 inline put 은 `@Setup(Level.Invocation)` 으로 분리하여 remove 경로만 측정.
> - `LettuceNearCacheConfig` 생성자 시그니처는 실 API (`maxLocalSize`, `recordStats`) 로 확인 후 조정.
> - `RedisServer.Launcher.redis` 대신 실제 launcher 경로는 `AbstractLettuceNearCacheTest` 에서 패턴 재사용.

`build.gradle.kts` 변경: `infra/lettuce/build.gradle.kts` 의 benchmark sourceset 블록·plugins·configurations 를 그대로 cache-lettuce 에 복제하고, dependencies 에 `testFixtures(project(":bluetape4k-testcontainers"))` 와 NearCache 가 요구하는 cache-core/lettuce/coroutines compileOnly 를 합산한다.

---

## 5. Benchmark.md 구조 개요 (모듈별)

세 모듈 모두 동일한 섹션 골격을 따른다 (idgenerators 템플릿).

### 5.1 공통 골격

```
# {Module} Benchmark
[English](./Benchmark.md) | [한국어](./Benchmark.ko.md)        ← KO 파일은 반대 방향

1. Measurement Overview      대상 codec/시나리오·페이로드 크기·반복 옵션
2. How to Run                gradle 명령 + 사전조건(예: Docker)
3. Results
   3.1 Summary Table         측정값 한눈에
   3.2 Detailed Results      JMH raw 출력(코드 블록)
   3.3 Performance Chart     colored <span> 바 차트 (Top→Bottom)
4. Performance Analysis      Key Findings (3–5개)
5. Recommendations           시나리오별 추천 표
6. Conclusion                1문단 + 권장 default
7. Future Work               (선택) suspend/coroutine 측정, 다른 페이로드 크기 등
8. Benchmark Environment     JVM/Kotlin/JMH 버전, warmup/measurement/fork
```

### 5.2 모듈별 차이

**`infra/lettuce/Benchmark.md`** — 13 codec 한 표.

- Summary Table 행: jackson3, fastjson2, fory, kryo, jdk, lz4Fory, lz4Kryo, zstdFory, zstdKryo, fastFory, lz4FastFory, zstdFastFory, gzipFastFory.
- Performance Analysis 핵심 축:
    - Binary vs JSON (fory 계열 vs jackson3/fastjson2)
    - 압축 효과 (lz4 vs zstd vs none) — 처리량 vs 사이즈 trade-off 정성 언급
    - FastFory (SCHEMA_CONSISTENT) 의 일반 Fory 대비 우위
- Recommendations 표 컬럼: 시나리오 / 추천 codec / 근거.

**`infra/redisson/Benchmark.md`** — lettuce 와 동일 13 codec, ByteBuf 기반 차이만 명시.

- "Notes on ByteBuf vs ByteBuffer" 짧은 박스 — Redisson 은 Netty `ByteBuf` 를 직접 사용해 GC 압력이 다름.
- Lettuce 결과와 비교한 "Cross-Module Note" 1단락 — 동일 코덱이라도 buffer 종류 차이로 미세 격차가 발생할 수 있다.

**`infra/cache-lettuce/Benchmark.md`** — 6 시나리오.

- Summary Table 행: l1Hit, l2Hit, l2Miss, putSingle, putAll (batch=100), removeSingle.
- Performance Analysis 핵심 축:
    - L1 vs L2 latency (Caffeine 메모리 hit vs Redis 왕복)
    - Read vs Write 비대칭
    - L2 miss 의 양쪽-부정 비용
    - putAll 의 amortized 처리량 (per-entry 환산 보너스 표)
- Recommendations: hit ratio 가 임계값 이상일 때 Caffeine-only 대비 NearCache 의 장점.
- Future Work 항목에 "LettuceSuspendNearCache 별도 측정", "더 큰 페이로드 (4KB/16KB)" 명시.

### 5.3 Colored bar 인코딩 규칙

idgenerators 와 동일.

- 색상 8종 순환: `#0EA5E9 sky` · `#EC4899 pink` · `#10B981 emerald` · `#F97316 orange` · `#EAB308 yellow(black text)` · `#8B5CF6 violet` · `#EF4444 red` · `#6366F1 indigo` (8번째는 codec 13개를 위해 추가).
- 막대 길이는 1위 = 40 블록 기준 비례 정수 반올림 (idgenerators 와 동일 알고리즘).
- 색상은 동일 codec 이면 lettuce/redisson 두 페이지에서 같은 색을 유지 (독자 비교 편의).

---

## 6. DoD Checklist

### 코드

- [ ] `infra/cache-lettuce/build.gradle.kts` — `kotlinx_benchmark` plugin · benchmark sourceset · benchmarkImplementation/RuntimeOnly configuration · `register("benchmark")` 추가
- [ ] `infra/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearCacheBenchmark.kt` 작성
- [ ] `./gradlew :bluetape4k-cache-lettuce:benchmark` 로컬 1회 성공 (Docker 필요)
- [ ] JMH raw 출력 캡처 → 세 모듈 `Benchmark.md` 의 "Detailed Results" 섹션에 그대로 붙여넣기
- [ ] `./gradlew :bluetape4k-lettuce:benchmark` · `:bluetape4k-redisson:benchmark` 도 재실행하여 최신 수치 확보

### 문서

- [ ] `infra/lettuce/Benchmark.md` + `Benchmark.ko.md` (페어, 동일 commit)
- [ ] `infra/redisson/Benchmark.md` + `Benchmark.ko.md` (페어, 동일 commit)
- [ ] `infra/cache-lettuce/Benchmark.md` + `Benchmark.ko.md` (페어, 동일 commit)
- [ ] 각 파일 상단 bilingual switch 링크 검증 (`[한국어](./Benchmark.ko.md) | English` / 반대)
- [ ] colored span 바 차트가 GitHub 미리보기 + IntelliJ Markdown preview 양쪽에서 정상 렌더 확인
- [ ] **Vega-Lite 사용
  금지** (CLAUDE.md 규칙). Mermaid 는 Benchmark.md 본문에서 제외 — Benchmark.md 는 colored span 바 차트 전용; Mermaid 다이어그램은 README.md 에만.
- [ ] `infra/cache-lettuce/README.md` + `README.ko.md` 의 NearCache 아키텍처 Mermaid 다이어그램 최신화 (L1/L2 invalidation 경로 포함)

### 모듈 README 동기화

- [ ] `infra/lettuce/README.md` + `README.ko.md` 의 "Performance" 섹션에서 `Benchmark.md` 로 링크
- [ ] `infra/redisson/README.md` + `README.ko.md` 동일 처리
- [ ] `infra/cache-lettuce/README.md` + `README.ko.md` 동일 처리

### 빌드 / 검증

- [ ] `./gradlew :bluetape4k-cache-lettuce:compileBenchmarkKotlin` 성공
- [ ] `./gradlew :bluetape4k-cache-lettuce:test` 회귀 없음 (코드 추가가 main/test 영향 없음 확인)
- [ ] `./gradlew detekt` (해당 모듈) 통과
- [ ] `code-reviewer` 에이전트 1회 실행 → HIGH/CRITICAL 이슈 0

### PR

- [ ] `feat: docs/benchmark-results — issue #184 벤치마크 결과 문서화` (Korean prefix)
- [ ] PR 본문에 측정 환경 (JVM/Kotlin/Hardware), Docker 요구사항, 재실행 명령 명시
- [ ] `/wiki-update` 1회 실행 (spec/plan 신규 작성에 의해)
- [ ] 작업이 `.worktrees/docs-benchmark-results/` 안에서 이루어졌는지 확인

---

## 7. Risks & Mitigations

| 위험                                                             | 완화                                                                                                                                    |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Docker 미설치 환경에서 cache-lettuce 벤치마크 실행 불가          | "How to Run" 에 Docker 사전조건 명시. CI 자동 실행 본 issue 범위 외.                                                                    |
| `localInvalidate` 미존재(Review C1 해소) → `clearLocal()` 사용   | 설계 결정 3.3 에서 `clearLocal()` 방식으로 재설계 완료. l1Hit/l2Hit 분리 클래스 권장.                                                   |
| 측정값이 머신 의존적                                             | `Benchmark Environment` 섹션에 측정 머신 사양(CPU/RAM/OS) 정확히 기재. 절대값보다 모듈 내 상대 순위가 의사결정 단위라는 점 본문에 명시. |
| codec 결과가 lettuce vs redisson 사이에 크게 다를 경우 해석 부담 | "Cross-Module Note" 박스로 ByteBuf vs ByteBuffer 차이를 한 단락으로 사전 설명.                                                          |
