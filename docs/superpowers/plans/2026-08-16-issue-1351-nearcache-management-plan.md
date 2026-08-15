# Issue #1351 NearJCache Management/Statistics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `NearJCache`의 실제 configuration과 wrapper 연산을 JCache 표준·tier MXBean에 연결하고, explicit JMX lifecycle과 disabled 비용 계약을 검증 가능한 형태로 제공한다.

**Architecture:** construction-time에 immutable configuration snapshot과 final recorder를 선택한다. logical/tier 통계는 generation 단위 recorder를 공유한다. JMX 등록은 caller-owned `MBeanServer`에 transactional하게 수행하되 `NearJCache.close()`와 같은 two-phase lifecycle reservation에서 선형화하고 외부 server 호출은 internal lock 밖에서 실행한다.

**Tech Stack:** Kotlin 2.4, Java 25, JCache (`javax.cache`), JMX (`javax.management`), Caffeine JCache, JUnit 5, MockK, bluetape4k assertions, Kover, Detekt, kotlinx-benchmark/JMH

**Spec:** `docs/superpowers/specs/2026-08-16-issue-1351-nearcache-management-design.md`

**Stack:** `develop` → PR #1432 `fix/1426-nearcache-observation` → PR #1433 `fix/1348-lettuce-entryprocessor-atomicity` @ `513f70e785ea6975fc150844b6b8f23b9238031c` → `feat/1351-nearcache-management`

---

## 실행 경계

- 이 계획은 blocking `NearJCache`와 `cache-core`의 management/statistics만 변경한다.
- `SuspendNearJCache`, `loadAll`, `invoke`, `invokeAll`, provider eviction adapter, runtime flag toggle, 자동 JMX 등록은 변경하지 않는다.
- `NearJCacheConfig`의 primary/legacy constructor, `copy`, component 순서, Java serialization shape는 변경하지 않는다.
- functional proof는 Caffeine JCache, MockK, 독립 `MBeanServer`로 수행한다. Testcontainers는 필요하지 않다.
- benchmark는 기존 `cache-lettuce`의 `kotlinx.benchmark` 소스셋만 재사용하고 production dependency나 새 benchmark module을 추가하지 않는다.
- implementation commit은 아래 순서를 유지한다. #1351은 PR 하나로 만들되 각 commit이 독립적인 RED/GREEN 또는 evidence 경계를 가진다.
- PR 생성 대상은 `bluetape4k/bluetape4k-projects`, base는 `fix/1348-lettuce-entryprocessor-atomicity`, head는 `feat/1351-nearcache-management`다. push와 PR 생성은 모든 구현·검증·review·lesson gate가 끝난 뒤 수행하며 merge는 별도 fresh 승인 전에는 수행하지 않는다.

## 파일 구조

### 신규 production 파일

| 파일 | 책임 |
|---|---|
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt` | 표준 `CacheMXBean`과 type resolution metadata 공개 계약 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt` | front/supplied/back configuration의 immutable pair-level snapshot |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheTierStatisticsMXBean.kt` | 표준 통계와 tier/capability attribute 공개 계약 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorder.kt` | active generation과 singleton NoOp recording context |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt` | explicit registrar, ObjectName, handle state, retry 가능한 cleanup |

### 수정 production 파일

| 파일 | 변경 |
|---|---|
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt` | snapshot/recorder final 선택, operation matrix 계측, registration registry와 close lifecycle |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt` | cache strong reference 제거, snapshot-backed standard/custom getter |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBean.kt` | shared recorder-backed 표준/tier getter와 legacy mutator 호환성 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/EmptyNearJCacheStatisticsMXBean.kt` | 모든 legacy mutator와 `clear()`의 singleton NoOp 계약 |

### 신규·수정 test/benchmark 파일

| 파일 | 책임 |
|---|---|
| `cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt` | 같은 method의 baseline/candidate disabled front-hit 비교 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt` | configuration pair fallback와 flag snapshot |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorderTest.kt` | generation reset, 시간 변환, NoOp 비용 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt` | read/mutation/compound/bulk operation matrix |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt` | flag matrix, ObjectName, collision, rollback, retry, serialization |
| `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeansJavaConsumer.java` | 고정 facade/descriptor의 Java compile consumer |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanLifecycleTest.kt` | registration/close 경합과 resource ownership |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt` | 기존 기본값 test를 snapshot 실제값 test로 교체 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBeanTest.kt` | 기존 public bean과 Empty bean ABI/동작 회귀 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt` | constructor/serialization regression 유지 확인 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt` | JMX cleanup을 포함한 기존 close failure ordering 확장 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheDocumentationTest.kt` | Kotlin/Java caller fixture와 EN/KO marker parity/금지 문구 회귀 |

### 문서·coverage·evidence 파일

| 파일 | 변경 |
|---|---|
| `cache/cache-core/build.gradle.kts` | management package Kover exclusion 제거 |
| `cache/cache-core/README.md` | source-equivalent configuration, explicit JMX, 운영 runbook |
| `cache/cache-core/README.ko.md` | README.md와 동등한 한국어 계약 |
| `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md` | 표준/tier 통계와 lifecycle 설명 |
| `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md` | EN과 동등한 한국어 설명 |
| `docs/cache/near-cache-capability-matrix.md` | wrapper-v1 지원/미지원 operation과 capability |
| `docs/benchmarks/raw/issue-1351/baseline/jmh.json` | instrumentation 전 exact harness baseline |
| `docs/benchmarks/raw/issue-1351/baseline/manifest.json` | exact harness commit/source/jar/environment/profile identity |
| `docs/benchmarks/raw/issue-1351/candidate/jmh.json` | disabled recorder candidate |
| `docs/benchmarks/issue-1351-compare.jq` | method/thread별 metadata, median, allocation uncertainty fail-closed 비교 |
| `docs/benchmarks/2026-08-16-issue-1351-nearcache-statistics.md` | 환경, commit, command, median throughput/allocation 비교 |
| `docs/lessons/2026-08-16-issue-1351-nearcache-management.md` | lifecycle·통계 의미·검증에서 재사용할 결정 |
| `docs/operations/issue-1351-nearcache-management.md` | state classifier, query/alert, rollout/rollback 운영 절차 |
| `docs/operations/templates/issue-1351-nearcache-management.json` | exact identity, observation, cleanup, sign-off evidence schema |

## 의존 순서

```text
Task 1 benchmark baseline
  └─ Task 2 configuration snapshot
       └─ Task 3 recorder/MXBean
            ├─ Task 4 read statistics
            └─ Task 5 mutation statistics
                 └─ Task 6 transactional registrar
                      └─ Task 7 cache lifecycle integration
                           └─ Task 8 ABI/docs/coverage
                                └─ Task 9 candidate benchmark/final verification
                                     └─ Task 10 reviews/lesson/stacked PR
```

## 위험 예측과 복구 지점

| 위험 | 조기 신호 | 완화·검증 | 복구 지점 |
|---|---|---|---|
| disabled hot path에 clock/allocation이 남음 | fake clock count 또는 `gc.alloc.rate.norm` 증가 | singleton NoOp context, baseline-first JMH | Task 3 또는 Task 9 commit만 되돌림 |
| reset 중 이전/new generation 혼합 | percentage/average가 불가능한 조합 반환 | operation 시작 시 context 1회 capture, concurrent reset test | Task 3 recorder commit으로 복귀 |
| registration collision이 다른 owner MBean을 삭제 | collision 이름의 unregister 호출 관찰 | 정상 반환 `ObjectInstance`만 owned set에 추가 | Task 6 registrar commit으로 복귀 |
| 등록 뒤 foreign replacement를 handle이 삭제 | descriptor token 불일치인데 unregister 호출 | token match 확인 + exclusive namespace precondition | Task 6 registrar commit으로 복귀 |
| blocking/reentrant MBeanServer가 lifecycle deadlock 유발 | bounded concurrency test timeout | two-phase reservation, 외부 호출 중 internal lock 0개, reentry guard | Task 7 lifecycle commit으로 복귀 |
| close 실패 뒤 resource가 영구 누수 | `RECOVERY_REQUIRED`가 retry 뒤에도 유지 | resource별 completion state, retry/idempotency test | Task 7 lifecycle commit으로 복귀 |
| async write 실패를 표준 put 성공으로 오해 | MXBean count와 remote completion이 불일치 | caller-return과 `BackCacheWriteCompletion` assertion 분리 | Task 5 계측 경계 수정 |
| public/serialization ABI drift | reflection/Java consumer/round-trip test 실패 | 기존 constructor/mutator 유지, config 무변경 | 실패한 production commit 즉시 되돌림 |
| benchmark 환경 차이가 5% 판정을 왜곡 | baseline/candidate JDK·fork·command hash 불일치 | 동일 머신/JDK/JAR profile, raw JSON 보존, median 비교 | canonical pair 모두 폐기 후 재실행 |

## Task 1: disabled path benchmark harness와 baseline 고정

**Complexity:** 중간

**Depends on:** 이 handoff에서 승인될 v2.3 spec/plan exact HEAD; 실행 시작 시 `git rev-parse HEAD`와 두 문서의 포함 여부를 먼저 기록한다.

**Files:**

- Create: `cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt`
- Create: `docs/benchmarks/raw/issue-1351/baseline/jmh.json`
- Create: `docs/benchmarks/raw/issue-1351/baseline/concurrency.json`
- Create: `docs/benchmarks/raw/issue-1351/baseline/manifest.json`
- Create: `docs/benchmarks/2026-08-16-issue-1351-nearcache-statistics.md`

- [ ] **Step 1: 실제 benchmark task와 exact JMH jar 경로를 재확인한다**

Run:

```bash
./gradlew :bluetape4k-cache-lettuce:tasks --all --console=plain \
  | rg 'benchmarkBenchmark(Compile|Jar)|compileBenchmarkKotlin'
```

Expected: `benchmarkBenchmarkCompile`, `benchmarkBenchmarkJar`, `compileBenchmarkKotlin`이 모두 출력된다. 현재 확인된 jar 경로는 `cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar`다.

- [ ] **Step 2: baseline과 candidate가 공유할 benchmark를 추가한다**

```kotlin
package io.bluetape4k.cache.nearcache.benchmark

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@Threads(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
open class NearJCacheStatisticsBenchmark {
    private lateinit var nearCache: NearJCache<String, String>
    private lateinit var backCache: JCache<String, String>
    private val keys = setOf("key-1", "key-2", "key-3", "key-4")
    private val entries = keys.associateWith { "value" }

    @Param("false")
    var statisticsEnabled: Boolean = false

    @Setup(Level.Trial)
    fun setup() {
        val frontConfig = jcacheConfiguration<String, String> {
            setTypes(String::class.java, String::class.java)
            setStoreByValue(false)
            setStatisticsEnabled(statisticsEnabled)
            setManagementEnabled(false)
        }
        val front = JCaching.Caffeine.getOrCreate("issue-1351-front", frontConfig)
        backCache = JCaching.Caffeine.getOrCreate("issue-1351-back", frontConfig)
        nearCache = NearJCache(
            frontCache = front,
            backCache = backCache,
            config = NearJCacheConfig(frontCacheConfiguration = frontConfig, isSynchronous = true),
        )
        nearCache.putAll(entries)
    }

    @Benchmark
    fun frontHit(): String? = nearCache.get("key-1")

    @Benchmark
    fun bulkHit(): Map<String, String> = nearCache.getAll(keys)

    @Benchmark
    fun put(): Unit = nearCache.put("put-key", "value")

    @Benchmark
    fun putAll(): Unit = nearCache.putAll(entries)

    @Benchmark
    fun getAndPut(): String? = nearCache.getAndPut("compound-key", "value")

    @TearDown(Level.Trial)
    fun tearDown() {
        nearCache.clear()
        nearCache.close()
        backCache.close()
    }
}
```

- [ ] **Step 3: harness를 compile한 뒤 source-only baseline commit을 만든다**

Run:

```bash
./gradlew :bluetape4k-cache-lettuce:benchmarkBenchmarkJar --no-configuration-cache
git add cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt
git commit -m "test: NearJCache 계측 전 benchmark harness를 고정한다" \
  -m "Constraint: baseline과 candidate는 같은 committed source를 사용한다
Confidence: high
Scope-risk: narrow
Directive: 이 commit 뒤 production 계측 전 baseline을 기록한다
Tested: :bluetape4k-cache-lettuce:benchmarkBenchmarkJar
Not-tested: JMH baseline은 다음 step에서 실행"
```

이 commit의 exact 40자 SHA가 baseline harness commit이다.

- [ ] **Step 4: instrumentation 전 baseline을 기록한다**

Run:

```bash
java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
  '.*NearJCacheStatisticsBenchmark.*' \
  -p statisticsEnabled=false -t 1 -f 3 -wi 5 -i 10 -w 1s -r 1s -prof gc -rf json \
  -rff docs/benchmarks/raw/issue-1351/baseline/jmh.json
```

Expected: 대표 read/bulk/mutation/compound benchmark 5개가 성공하고 JSON에 `primaryMetric.rawData`, `scoreError`, `gc.alloc.rate.norm`, JMH/JDK/profile 정보가 들어 있다. Docker/Testcontainers는 시작하지 않는다.

같은 pre-instrumentation commit에서 다음을 실행한다.

```bash
mkdir -p cache/cache-lettuce/build/benchmarks/issue-1351/baseline-concurrency
for threads in 1 2 4 8 16; do
  java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
    '.*NearJCacheStatisticsBenchmark.frontHit.*' -p statisticsEnabled=false \
    -t "$threads" -f 3 -wi 5 -i 10 -w 1s -r 1s -prof gc -rf json \
    -rff "cache/cache-lettuce/build/benchmarks/issue-1351/baseline-concurrency/threads-$threads.json"
done
jq -s 'add' cache/cache-lettuce/build/benchmarks/issue-1351/baseline-concurrency/threads-*.json \
  > docs/benchmarks/raw/issue-1351/baseline/concurrency.json
```

이 concurrency 결과는 disabled baseline이며 contention 관찰 자료로 보존한다.

- [ ] **Step 5: machine-readable manifest와 baseline 문서를 기록한다**

`baseline/manifest.json`에는 `harnessCommit` exact 40자 SHA, benchmark source SHA-256, jar SHA-256, `java -version`, `uname -a`, exact command와 JMH version/JVM/args/mode/threads/forks/warmup/measurement/params를 machine-readable field로 기록한다. 문서에도 같은 값, throughput 단위 `ops/ms`, allocation 단위 `B/op`을 기록한다. “단일 로컬 머신 snapshot이며 production ranking이 아님”과 `0 B/op`은 exact 0 단정이 아니라 profiler uncertainty 안에서 새 allocation이 관찰되지 않는다는 noise 정책임을 명시한다. candidate 수치는 Task 9에서 같은 표에 추가한다.

- [ ] **Step 6: benchmark baseline evidence commit을 만든다**

```bash
git add docs/benchmarks/raw/issue-1351/baseline/jmh.json \
  docs/benchmarks/raw/issue-1351/baseline/concurrency.json \
  docs/benchmarks/raw/issue-1351/baseline/manifest.json \
  docs/benchmarks/2026-08-16-issue-1351-nearcache-statistics.md
git commit -m "test: NearJCache 통계 계측 전 비용 기준을 고정한다" \
  -m "Constraint: disabled 통계 경로는 동일 harness의 이전 동작과 비교해야 한다
Confidence: high
Scope-risk: narrow
Directive: candidate는 같은 JDK, fork, warmup, measurement, GC profiler 조건으로 실행한다
Tested: :bluetape4k-cache-lettuce:benchmarkBenchmarkJar, issue-1351 baseline JMH
Not-tested: production 통계 계측은 아직 구현하지 않음"
```

## Task 2: immutable configuration snapshot과 configuration MXBean

**Complexity:** 중간

**Depends on:** Task 1

**Files:**

- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt`
- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt`

- [ ] **Step 1: actual-front → supplied-front → actual-back → unresolved pair fallback RED test를 작성한다**

```kotlin
@Test
fun `Object pair인 actual front는 supplied front pair로 fallback한다`() {
    val actualFront = MutableConfiguration<Any, Any>().setStoreByValue(false)
    val suppliedFront = MutableConfiguration<String, Long>()
        .setTypes(String::class.java, Long::class.javaObjectType)
        .setStoreByValue(false)
        .setStatisticsEnabled(true)
        .setManagementEnabled(true)

    val snapshot = snapshotOf(actualFront, suppliedFront, MutableConfiguration<Any, Any>())

    snapshot.keyType shouldBeEqualTo "java.lang.String"
    snapshot.valueType shouldBeEqualTo "java.lang.Long"
    snapshot.typeResolutionSource shouldBeEqualTo "SUPPLIED_FRONT"
    snapshot.typeResolutionExact.shouldBeFalse()
}
```

같은 test class에 actual front exact pair, supplied front fallback, actual back fallback, 최종 `Object,Object`, pair source 혼합 금지, `CompleteConfiguration` 미지원 시 false, `storeByValue=false`, construction 이후 manager flag 변경 불변 test를 각각 독립 method로 추가한다. `getConfiguration`이 requested class 미지원으로 `IllegalArgumentException`을 던질 때만 다음 후보로 fallback한다. throwing fake provider의 `IllegalStateException`, `CacheException`, `SecurityException`, 다른 runtime failure는 원인 동일성까지 보존해 construction이 실패하고 fallback하지 않는 test를 추가한다.

- [ ] **Step 2: targeted test를 실행해 RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheConfigurationSnapshotTest' --no-build-cache
```

Expected: `NearJCacheConfigurationSnapshot`, `NearJCacheConfigurationMXBean`이 아직 없어 compile failure가 발생한다.

- [ ] **Step 3: snapshot과 public MXBean interface를 구현한다**

```kotlin
package io.bluetape4k.cache.nearcache.jcache.management

import javax.cache.management.CacheMXBean

interface NearJCacheConfigurationMXBean: CacheMXBean {
    fun getTypeResolutionSource(): String
    fun isTypeResolutionExact(): Boolean
}

internal enum class NearJCacheTypeResolutionSource {
    ACTUAL_FRONT,
    SUPPLIED_FRONT,
    ACTUAL_BACK,
    UNRESOLVED_OBJECT,
}

internal data class NearJCacheConfigurationSnapshot(
    val keyType: String,
    val valueType: String,
    val typeResolutionSource: NearJCacheTypeResolutionSource,
    val typeResolutionExact: Boolean,
    val readThrough: Boolean,
    val writeThrough: Boolean,
    val storeByValue: Boolean,
    val statisticsEnabled: Boolean,
    val managementEnabled: Boolean,
)
```

snapshot factory는 후보별 `Configuration.keyType/valueType`을 pair로 읽고 둘 다 `Object`가 아닐 때만 선택한다. requested configuration class 미지원의 `IllegalArgumentException`만 unavailable로 변환하고 다른 provider/runtime exception은 전파한다. flag는 actual front의 `CompleteConfiguration`에서만 읽고, 해당 view를 제공하지 않아 `IllegalArgumentException`이면 `false`로 고정한다. loader/writer factory는 실행하지 않는다.

- [ ] **Step 4: 기존 public bean constructor를 보존하면서 snapshot-backed getter로 바꾼다**

```kotlin
class NearJCacheManagementMXBean private constructor(
    private val snapshot: NearJCacheConfigurationSnapshot,
): NearJCacheConfigurationMXBean {
    constructor(cache: NearJCache<*, *>) : this(cache.configurationSnapshot)

    companion object {
        @JvmSynthetic
        internal fun fromSnapshot(snapshot: NearJCacheConfigurationSnapshot) =
            NearJCacheManagementMXBean(snapshot)
    }

    override fun getKeyType(): String = snapshot.keyType
    override fun getValueType(): String = snapshot.valueType
    override fun getTypeResolutionSource(): String = snapshot.typeResolutionSource.name
    override fun isTypeResolutionExact(): Boolean = snapshot.typeResolutionExact
    override fun isReadThrough(): Boolean = snapshot.readThrough
    override fun isWriteThrough(): Boolean = snapshot.writeThrough
    override fun isStoreByValue(): Boolean = snapshot.storeByValue
    override fun isStatisticsEnabled(): Boolean = snapshot.statisticsEnabled
    override fun isManagementEnabled(): Boolean = snapshot.managementEnabled
}
```

`NearJCache`는 constructor에서 snapshot을 한 번 만들고 `internal val configurationSnapshot`으로 보유한다. bean은 snapshot만 보유해 cache/front/back strong reference를 남기지 않는다. reflection/`javap -public`은 management bean의 public constructor가 기존 `(NearJCache)` 하나뿐이고 snapshot factory는 synthetic인지 고정한다.

- [ ] **Step 5: configuration test와 existing contract test를 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheConfigurationSnapshotTest' \
  --tests '*NearJCacheManagementMXBeanTest' \
  --tests '*NearJCacheContractTest' --no-build-cache
```

Expected: actual/fallback source와 flag test가 모두 통과하고 기존 front store-by-reference invariant도 유지된다.

- [ ] **Step 6: configuration commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt
git commit -m "feat: NearJCache 설정을 생성 시점 스냅숏으로 노출한다" \
  -m "Constraint: generic pair는 서로 다른 configuration source에서 조합하지 않는다
Rejected: provider unwrap | provider 종속성과 lifecycle 노출을 피한다
Confidence: high
Scope-risk: moderate
Directive: runtime flag 변경은 기존 instance에 반영하지 않는다
Tested: NearJCacheConfigurationSnapshotTest, NearJCacheManagementMXBeanTest, NearJCacheContractTest
Not-tested: JMX 등록은 후속 task에서 검증"
```

## Task 3: generation recorder, tier MXBean, disabled NoOp

**Complexity:** 높음

**Depends on:** Task 2

**Files:**

- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheTierStatisticsMXBean.kt`
- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorder.kt`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorderTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBean.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/EmptyNearJCacheStatisticsMXBean.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBeanTest.kt`

- [ ] **Step 1: generation/reset/time/NoOp RED test를 작성한다**

```kotlin
@Test
fun `operation은 시작 때 얻은 generation에만 기록한다`() {
    val time = CountingTimeSource(10L, 1_010L)
    val recorder = ActiveNearJCacheStatisticsRecorder(time)
    val old = recorder.current()
    val startedAt = old.startTimeNanos()

    recorder.clear()
    old.recordGet(startedAt, hits = 1, misses = 0, frontHits = 1, frontMisses = 0, backHits = 0, backMisses = 0)

    recorder.current().cacheGets shouldBeEqualTo 0L
}

@Test
fun `NoOp context는 clock을 읽지 않고 모든 값이 0이다`() {
    val time = CountingTimeSource(10L)
    val fixture = disabledStatisticsFixture(timeSource = time)

    fixture.cache.get("key")
    fixture.cache.getAll(setOf("key", "missing"))
    fixture.cache.put("put", "value")
    fixture.cache.putAll(mapOf("one" to "1", "two" to "2"))
    fixture.cache.getAndPut("compound", "value")

    time.invocations shouldBeEqualTo 0
    fixture.statistics.cacheGets shouldBeEqualTo 0L
    fixture.statistics.cachePuts shouldBeEqualTo 0L
}
```

`@JvmSynthetic internal` companion test factory는 public constructor descriptor를 바꾸지 않고 `NearJCacheTimeSource`를 실제 recorder 선택에 주입하며 production constructor는 private recorder constructor에 위임한다. 새 JVM constructor를 만들지 않는다. `javap`에서 기존 public constructor만 남고 test factory는 synthetic인지 확인한다. 1ns 미만 절삭을 피하는 microsecond `Float` 변환, zero count `0F`, hit/miss percentage 한 generation 읽기, concurrent update/reset, stable capabilities, unsupported eviction `0 + false`, Empty bean의 `addRemovals` 포함 모든 mutator no-op도 추가한다. `supportedOperations`는 매 호출 `copyOf()`를 반환하고 caller가 첫 배열을 변조한 뒤에도 다음 getter/JMX proxy 값이 같은지 검증한다.

- [ ] **Step 2: targeted test의 RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheStatisticsRecorderTest' \
  --tests '*NearJCacheStatisticsMXBeanTest' --no-build-cache
```

Expected: 새 recorder/interface가 없어 compile failure가 발생하거나 기존 `EmptyNearJCacheStatisticsMXBean.addRemovals` test가 실패한다.

- [ ] **Step 3: allocation-free operation context와 generation swap을 구현한다**

```kotlin
internal fun interface NearJCacheTimeSource {
    fun nanoTime(): Long
}

internal interface NearJCacheRecordingContext {
    fun startTimeNanos(): Long
    fun recordGet(
        startedAt: Long,
        hits: Long,
        misses: Long,
        frontHits: Long,
        frontMisses: Long,
        backHits: Long,
        backMisses: Long,
    )
    fun recordPut(startedAt: Long, count: Long)
    fun recordRemove(startedAt: Long, count: Long)
    val cacheHits: Long
    val cacheMisses: Long
    val cacheGets: Long
    val cachePuts: Long
    val cacheRemovals: Long
    val cacheEvictions: Long
    val frontHits: Long
    val frontMisses: Long
    val backHits: Long
    val backMisses: Long
    val totalGetTimeNanos: Long
    val totalPutTimeNanos: Long
    val totalRemoveTimeNanos: Long
}

internal interface NearJCacheStatisticsRecorder {
    fun current(): NearJCacheRecordingContext
    fun clear()
}
```

`ActiveNearJCacheStatisticsRecorder`는 `AtomicReference<ActiveGeneration>`을 사용하고 `clear()`에서 새 generation으로 `getAndSet`한다. `NoOpNearJCacheStatisticsRecorder.current()`는 singleton context를 반환하고 clock/adder/lambda를 만들지 않는다. operation은 `current()`를 한 번만 읽고 그 context로 시작 시간과 완료 기록을 수행한다. test seam은 `@JvmSynthetic internal` factory 한 곳에만 두며 public/serialization ABI에 포함하지 않는다.

- [ ] **Step 4: 표준/tier MXBean과 legacy mutator 호환성을 구현한다**

```kotlin
interface NearJCacheTierStatisticsMXBean: CacheStatisticsMXBean {
    fun getFrontHits(): Long
    fun getFrontMisses(): Long
    fun getBackHits(): Long
    fun getBackMisses(): Long
    fun getFrontEvictions(): Long
    fun isFrontEvictionObservationSupported(): Boolean
    fun isBulkRemovalCountSupported(): Boolean
    fun getStatisticsScope(): String
    fun getSupportedOperations(): Array<String>
    fun isBackWriteCompletionIncluded(): Boolean
}
```

`class NearJCacheStatisticsMXBean ... : NearJCacheTierStatisticsMXBean` 선언으로 표준/custom getter와 `clear()`를 한 구현에 고정한다. `NearJCacheStatisticsMXBean()`은 active recorder를 생성하는 public no-arg constructor를 유지한다. private recorder constructor를 추가하고 모든 getter는 현재 generation reference 한 개에서 계산한다. 평균 시간은 `totalNanos.toDouble() / count / 1_000.0`을 마지막에 `Float`로 변환한다. legacy `add*`는 standalone active bean에서 같은 recorder를 갱신하고 JMX interface에는 포함하지 않는다. `EmptyNearJCacheStatisticsMXBean`도 같은 interface를 구현한다.

`supportedOperations` exact array는 `get`, `getAll`, `put`, `putAll`, `putIfAbsent`, `replace`, `remove`, `getAndPut`, `getAndReplace`, `getAndRemove` 순서다. `replace`/`remove`는 overload family token이며 `containsKey`, `clear`, `removeAll`, `loadAll`, `invoke`, `invokeAll`은 unsupported로 제외한다.

- [ ] **Step 5: recorder/MXBean test를 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheStatisticsRecorderTest' \
  --tests '*NearJCacheStatisticsMXBeanTest' --no-build-cache
```

Expected: reset 경계, clock count 0, average/percentage, capabilities, Empty bean test가 모두 통과한다.

- [ ] **Step 6: recorder commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheTierStatisticsMXBean.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorder.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBean.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/EmptyNearJCacheStatisticsMXBean.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsRecorderTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheStatisticsMXBeanTest.kt
git commit -m "feat: NearJCache 통계를 generation 단위로 격리한다" \
  -m "Constraint: disabled 경로는 clock, counter update, operation allocation을 만들지 않는다
Rejected: operation별 통계 token 객체 | hot path allocation을 피한다
Confidence: high
Scope-risk: moderate
Directive: operation은 시작 시 얻은 recording context를 완료까지 유지한다
Tested: NearJCacheStatisticsRecorderTest, NearJCacheStatisticsMXBeanTest
Not-tested: wrapper operation 연결은 후속 task에서 검증"
```

## Task 4: get/getAll logical·tier 통계 연결

**Complexity:** 높음

**Depends on:** Task 3

**Files:**

- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`

- [ ] **Step 1: front hit, back hit, full miss, mixed bulk, empty bulk RED test를 작성한다**

```kotlin
@Test
fun `front miss와 back hit는 logical hit 하나와 두 tier 결과를 기록한다`() {
    val fixture = statisticsEnabledFixture()
    fixture.back.put("key", "value")

    fixture.cache.get("key") shouldBeEqualTo "value"

    fixture.statistics.cacheHits shouldBeEqualTo 1L
    fixture.statistics.cacheMisses shouldBeEqualTo 0L
    fixture.statistics.frontHits shouldBeEqualTo 0L
    fixture.statistics.frontMisses shouldBeEqualTo 1L
    fixture.statistics.backHits shouldBeEqualTo 1L
    fixture.statistics.backMisses shouldBeEqualTo 0L
}
```

mixed `getAll(setOf("front", "back", "missing"))`은 logical hit 2/miss 1, front hit 1/miss 2, back hit 1/miss 1을 기대한다. fixed time source는 bulk elapsed를 한 번만 누적하고 empty set은 count/time 0을 기대한다. front populate `RuntimeException`을 숨기는 기존 get 계약에서도 caller-visible logical hit는 기록한다.

- [ ] **Step 2: RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheOperationStatisticsTest' --no-build-cache
```

Expected: recorder가 wrapper 연산과 연결되지 않아 count assertion이 실패한다.

- [ ] **Step 3: get과 getAll을 한 context로 계측한다**

```kotlin
override operator fun get(key: K): V? {
    val recording = statisticsRecorder.current()
    val startedAt = recording.startTimeNanos()
    val (frontValue, observedEpoch) = mutationGate.withLock {
        frontCache.get(key) to mutationEpoch.get()
    }
    if (frontValue != null) {
        recording.recordGet(startedAt, 1, 0, 1, 0, 0, 0)
        return frontValue
    }
    val backValue = backCache.get(key)
    if (backValue == null) {
        recording.recordGet(startedAt, 0, 1, 0, 1, 0, 1)
        return null
    }
    populateFrontWhenEpochMatches(key, backValue, observedEpoch)
    recording.recordGet(startedAt, 1, 0, 0, 1, 1, 0)
    return backValue
}
```

실제 구현은 현재 `get`의 cancellation/error/front-populate semantics를 그대로 유지한다. `getAll`은 `frontValues`, `missingKeys`, `backValues`에서 six count를 계산하고 정상 반환 직전에 `recordGet`을 한 번 호출한다. 예외 종료와 empty input에서는 기록하지 않는다.

- [ ] **Step 4: read 통계와 기존 read/epoch 계약을 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheOperationStatisticsTest' \
  --tests '*NearJCacheContractTest' --no-build-cache
```

Expected: 통계 assertion과 stale-populate/cancellation/error 기존 test가 모두 통과한다.

- [ ] **Step 5: read 통계 commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt
git commit -m "feat: NearJCache read 결과를 logical tier 통계로 연결한다" \
  -m "Constraint: getAll elapsed는 key별로 중복 배분하지 않는다
Confidence: high
Scope-risk: moderate
Directive: 예외 종료와 empty bulk는 count와 time을 기록하지 않는다
Tested: NearJCacheOperationStatisticsTest, NearJCacheContractTest
Not-tested: mutation 통계는 후속 task에서 검증"
```

## Task 5: mutation·compound·async caller-visible 통계 연결

**Complexity:** 높음

**Depends on:** Task 4

**Files:**

- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheCompoundOperationContractTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheWriteThroughFailureTest.kt`

- [ ] **Step 1: operation matrix의 mutation RED test를 모두 추가한다**

```kotlin
@Test
fun `getAndPut은 이전 값 hit와 put을 같은 elapsed로 각각 기록한다`() {
    val fixture = statisticsEnabledFixture(timeValues = longArrayOf(10L, 1_010L))
    fixture.back.put("key", "old")

    fixture.cache.getAndPut("key", "new") shouldBeEqualTo "old"

    fixture.statistics.cacheHits shouldBeEqualTo 1L
    fixture.statistics.cachePuts shouldBeEqualTo 1L
    fixture.statistics.averageGetTime shouldBeEqualTo 1.0F
    fixture.statistics.averagePutTime shouldBeEqualTo 1.0F
}
```

별도 test로 `put`, `putAll` 0/1/N, `putIfAbsent` success/failure, 두 `replace` success/failure, 두 `remove` success/failure, `getAndReplace`, `getAndRemove`, exceptional partial-unknown `putAll`, unsupported `removeAll` count, `NearJCache.clear()` 통계 유지, statistics `clear()` 데이터 유지, async caller-return count, exceptional `BackCacheWriteCompletion`의 동일 operation ID 분류를 검증한다. async test fixture는 수동 완료 `CompletableFuture`와 고정 barrier를 사용해 caller 정상 반환 뒤 exceptional completion, scheduling/반환 전 failure, retry 성공/고갈, listener close와 cache close 중 completion을 각각 결정적으로 만든다. 각 test는 listener가 받은 단일 `BackCacheWriteCompletion` snapshot의 `operationId`, `operation`, `completion`만 함께 읽고 `lastBackCacheWrite`의 분리 polling을 금지한다.

- [ ] **Step 2: RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheOperationStatisticsTest' \
  --tests '*NearJCacheCompoundOperationContractTest' \
  --tests '*NearJCacheWriteThroughFailureTest' --no-build-cache
```

Expected: mutation count/time이 0이어서 새 assertion이 실패한다.

- [ ] **Step 3: 성공 반환 직전 mutation count/time을 기록한다**

각 public operation 시작에서 `val recording = statisticsRecorder.current()`와 `val startedAt = recording.startTimeNanos()`를 한 번만 읽는다. 다음 exact 규칙을 구현한다.

```kotlin
// put/putAll 정상 반환
recording.recordPut(startedAt, count = 1L)
recording.recordPut(startedAt, count = map.size.toLong())

// conditional mutation은 true일 때만 기록
if (inserted) recording.recordPut(startedAt, 1L)
if (replaced) recording.recordPut(startedAt, 1L)
if (removed) recording.recordRemove(startedAt, 1L)

// compound는 previous value로 logical get과 mutation을 함께 기록
recording.recordGet(
    startedAt = startedAt,
    hits = if (oldValue == null) 0 else 1,
    misses = if (oldValue == null) 1 else 0,
    frontHits = 0,
    frontMisses = 1,
    backHits = if (oldValue == null) 0 else 1,
    backMisses = if (oldValue == null) 1 else 0,
)
```

`getAndPut`은 put을 항상, `getAndReplace`는 old value가 있을 때만 put, `getAndRemove`는 old value가 있을 때만 removal을 같은 `startedAt`으로 기록한다. `putAll(emptyMap())`, `removeAll`, `clear`, `containsKey`, 예외 종료는 통계 recorder를 갱신하지 않는다.

- [ ] **Step 4: async caller-visible 경계를 기존 observation API와 함께 검증한다**

async `put`이 caller에게 정상 반환하면 put count/time을 기록한다. 이후 listener가 받은 atomic completion이 exceptional completion이어도 count를 되돌리지 않는다. scheduling 자체가 caller 반환 전에 실패하면 count/time을 기록하지 않는다. retry는 caller-visible count를 한 번만 남기고 remote attempt 수와 분리한다. listener/cache close와 completion 경합은 callback 최대 1회, executor/future cleanup, 표준 count 불변을 검증한다. migration fixture는 application coordinator가 새 write를 차단하고 각 mutation 직후 보존한 completion inventory가 모두 terminal일 때만 handle/cache close를 허용하며, inventory가 없거나 하나라도 pending이면 online async migration을 거부한다.

- [ ] **Step 5: mutation/compound/failure test를 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheOperationStatisticsTest' \
  --tests '*NearJCacheCompoundOperationContractTest' \
  --tests '*NearJCacheWriteThroughFailureTest' --no-build-cache
```

Expected: operation matrix와 기존 atomic/retry/failure contract가 모두 통과한다.

- [ ] **Step 6: mutation 통계 commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheCompoundOperationContractTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheWriteThroughFailureTest.kt
git commit -m "feat: NearJCache mutation 성공 경계를 표준 통계로 기록한다" \
  -m "Constraint: 비동기 표준 수치는 caller-visible 성공이며 durable back completion이 아니다
Confidence: high
Scope-risk: moderate
Directive: removeAll과 provider eviction 수를 추정하지 않는다
Tested: operation statistics, compound contract, write-through failure tests
Not-tested: JMX 등록 lifecycle은 후속 task에서 검증"
```

## Task 6: explicit transactional JMX registrar

**Complexity:** 높음

**Depends on:** Task 5

**Files:**

- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt`
- Create: `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeansJavaConsumer.java`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`

- [ ] **Step 1: flag matrix, ObjectName, MBeanInfo, collision/rollback RED test를 작성한다**

```kotlin
@Test
fun `두 번째 이름 collision은 첫 owned MBean만 rollback한다`() {
    val server = MBeanServerFactory.newMBeanServer()
    val fixture = bothEnabledFixture()
    val statisticsName = nearJCacheObjectName("NearJCacheStatistics", "manager", "cache")
    val foreign = StandardMBean(EmptyNearJCacheStatisticsMXBean(), NearJCacheTierStatisticsMXBean::class.java, true)
    server.registerMBean(foreign, statisticsName)

    assertFailsWith<InstanceAlreadyExistsException> {
        fixture.cache.registerMBeans(server, "manager", "cache")
    }

    server.isRegistered(statisticsName).shouldBeTrue()
    server.isRegistered(nearJCacheObjectName("NearJCacheConfiguration", "manager", "cache")).shouldBeFalse()
}
```

management-only/statistics-only/both/neither, blank/257자/control-character/앞뒤-whitespace ID 거부, case-sensitive·Unicode normalization 없음, 허용 특수문자 `: , = * ? \\ \"`의 quote/unquote round-trip, `isPattern/isDomainPattern/isPropertyPattern=false`, one statistics ObjectName, standard/custom attribute와 `clear`만 노출, legacy `add*` 미노출, defensive Java unmodifiable set, `InstanceNotFoundException` 성공 처리, rollback failure suppressed/recovery handle, exception serialization도 독립 test로 추가한다. public KDoc은 ID와 exception/ObjectName metadata에 credential/token/PII를 넣지 않는 caller 계약을 고정한다.

Java compile fixture는 `NearJCacheMBeans.registerMBeans(cache, server, managerId, cacheId)`를 직접 호출한다. reflection/`javap -public`은 facade binary name과 static descriptor `(NearJCache, MBeanServer, String, String)NearJCacheMBeanRegistration`을 고정하고 default/synthetic convenience overload가 없음을 확인한다.

- [ ] **Step 2: RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheMBeanRegistrationTest' --no-build-cache
```

Expected: registrar public API가 없어 compile failure가 발생한다.

- [ ] **Step 3: public registration types와 ObjectName factory를 구현한다**

```kotlin
fun NearJCache<*, *>.registerMBeans(
    mBeanServer: MBeanServer,
    managerId: String,
    cacheId: String,
): NearJCacheMBeanRegistration

interface NearJCacheMBeanRegistration: AutoCloseable {
    val managerId: String
    val cacheId: String
    val state: NearJCacheMBeanRegistrationState
    val activeObjectNames: Set<ObjectName>
    val isClosed: Boolean
    override fun close()
}

enum class NearJCacheMBeanRegistrationState {
    REGISTERED,
    RECOVERY_REQUIRED,
    CLOSING,
    CLOSED,
}

class NearJCacheMBeanRegistrationException(
    @Transient val recoveryRegistration: NearJCacheMBeanRegistration?,
    remainingObjectNames: Set<ObjectName>,
    cause: Throwable,
): RuntimeException(cause) {
    val remainingObjectNames: Set<ObjectName> = Collections.unmodifiableSet(LinkedHashSet(remainingObjectNames))

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
```

file 첫 줄에 `@file:JvmName("NearJCacheMBeans")`를 두어 Java facade를 고정한다. public interface/class/extension/state/exception에는 한국어 KDoc을 작성하고 Kotlin/Java caller 예제, ID 노출과 ownership/precondition을 포함한다.

ID는 Unicode/case normalization 없이 원문 기준 1..256자, non-blank, 앞뒤 whitespace 없음, ISO control character 없음으로 검증하고 case-sensitive inventory key로 사용한다. ObjectName domain은 `io.bluetape4k.cache`이고 단일 factory가 모든 경로의 `manager`/`cache` value에 `ObjectName.quote`를 사용한다. platform MBeanServer나 random ObjectName ID를 선택하는 overload/default는 만들지 않는다.

- [ ] **Step 4: normal-return ObjectInstance만 소유하는 transactional 등록을 구현한다**

configuration/statistics bean은 registration마다 non-secret UUID token을 descriptor field에 넣는 internal `StandardMBean` wrapper로 등록한다. wrapper는 `super.getMBeanInfo()`의 MXBean descriptor를 보존하고 `ImmutableDescriptor.union`으로 ownership field 하나만 추가한 새 `MBeanInfo`를 반환한다. `registerMBean`이 반환한 `ObjectInstance.objectName`과 token pair만 insertion-ordered owned map에 추가한다. cleanup 직전 `MBeanInfo.descriptor` token이 일치할 때만 unregister한다. 이미 foreign bean으로 교체돼 token이 없거나 다르면 unregister하지 않고 이름을 `RECOVERY_REQUIRED`로 남긴다. token check와 unregister는 JMX API상 atomic CAS가 아니므로 public KDoc과 runbook은 handle lifetime 동안 exact ObjectName을 외부에서 교체하지 않는 exclusive namespace precondition을 명시한다. 두 번째 등록 실패 시 owned map만 역순 rollback하고 collision/정상 반환 전 실패 이름은 unregister하지 않는다. rollback failure는 primary exception의 suppressed에 추가하고 remaining owned names가 있으면 cache registry에 recovery handle을 먼저 넣은 뒤 `NearJCacheMBeanRegistrationException`을 던진다.

등록 뒤 original MBean을 외부에서 해제하고 같은 ObjectName에 foreign MBean을 등록한 test는 handle close가 foreign bean을 보존하고 `RECOVERY_REQUIRED`를 반환하는지 검증한다. token read와 unregister 사이의 adversarial replacement는 atomic하게 막을 수 없으므로 test 대상이 아니라 명시한 caller precondition 위반으로 분류한다.

- [ ] **Step 5: registrar test를 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheMBeanRegistrationTest' --no-build-cache
```

Expected: flag matrix, MBeanInfo, ownership, collision, rollback, serialization test가 모두 통과한다.

- [ ] **Step 6: registrar commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt \
  cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeansJavaConsumer.java
git commit -m "feat: NearJCache MXBean을 명시적으로 원자 등록한다" \
  -m "Constraint: caller-owned MBeanServer와 opaque non-secret ID만 사용한다
Rejected: 자동 등록 | embedded 환경의 전역 side effect를 피한다
Confidence: high
Scope-risk: broad
Directive: 정상 반환 ObjectInstance가 없는 이름은 절대 unregister하지 않는다
Tested: NearJCacheMBeanRegistrationTest
Not-tested: cache close와의 경합은 후속 task에서 검증"
```

## Task 7: registration과 NearJCache close lifecycle 단일화

**Complexity:** 높음

**Depends on:** Task 6

**Files:**

- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanLifecycleTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt`

- [ ] **Step 1: register/close 경합, retry, ownership RED test를 작성한다**

```kotlin
@Test
fun `unregister 실패 후 다음 cache close는 남은 이름만 재시도한다`() {
    val server = FailOnceUnregisterMBeanServer()
    val fixture = bothEnabledFixture()
    val registration = fixture.cache.registerMBeans(server, "manager", "cache")

    assertFailsWith<NearJCacheMBeanRegistrationException> { fixture.cache.close() }
    registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.RECOVERY_REQUIRED

    fixture.cache.close()

    registration.state shouldBeEqualTo NearJCacheMBeanRegistrationState.CLOSED
    fixture.front.isClosed.shouldBeTrue()
    fixture.back.isClosed.shouldBeFalse()
}
```

explicit handle/cache close 동시 호출, close가 먼저 `CLOSING`에 들어간 뒤 새 등록 거부, registration reservation 뒤 close 대기, 외부 server callback의 register/close 재진입 fail-fast, blocking server가 lifecycle lock을 점유하지 않음, 성공한 cleanup 재실행 금지, JMX/listener/front failure의 primary/suppressed exact 순서, back/provider/MBeanServer 미종료를 추가한다. foreign replacement token mismatch는 unregister 0회와 `RECOVERY_REQUIRED`를 검증한다. concurrency test는 고정 barrier/`CountDownLatch`, 5초 이하 `assertTimeoutPreemptively`, 전용 executor `finally shutdownNow`, resource별 exact call count를 사용하고 sleep/polling에 의존하지 않는다.

- [ ] **Step 2: RED를 확인한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheMBeanLifecycleTest' \
  --tests '*NearJCacheContractTest' --no-build-cache
```

Expected: close가 registration registry를 drain하지 않아 MBean 잔존 또는 state assertion이 실패한다.

- [ ] **Step 3: 기존 `compoundGate`를 two-phase lifecycle reservation에 재사용한다**

`NearJCache`에 pending registration reservation, registration handle registry, resource별 completion을 추가한다. `registerMBeans()`는 `compoundGate` 안에서 `OPEN` 확인과 pending reservation만 만들고 lock을 해제한다. caller-owned `MBeanServer` register/rollback 뒤 다시 gate 안에서 handle 또는 recovery handle을 publish하고 reservation을 완료한다. 먼저 reservation된 register는 close보다 먼저 선형화된다. `close()`는 gate 안에서 `CLOSING` 전이와 pending/handle snapshot만 수행한 뒤 lock 밖에서 pending completion을 기다리고 JMX → listener → front를 정리한다. external call/future wait 중에는 internal lock을 잡지 않는다.

```kotlin
internal fun registerMBeansAtomically(
    registration: () -> NearJCacheMBeanRegistration,
): NearJCacheMBeanRegistration {
    val reservation = compoundGate.withLock { reserveRegistrationWhileOpen() }
    val result = runCatching { runOutsideLifecycleLock(registration) }
    compoundGate.withLock { completeRegistration(reservation, result) }
    return result.getOrThrow()
}
```

lock 순서는 `compoundGate` reservation → release → handle state reservation → release → `MBeanServer` call이다. 두 internal lock을 함께 잡지 않고 handle callback은 handle lock을 해제한 뒤 gate를 획득한다. 같은 thread의 MBeanServer callback이 register/close로 재진입하면 thread-local operation guard가 `IllegalStateException`으로 fail-fast한다. arbitrary blocking server는 production timeout으로 취소하지 않지만 lifecycle gate를 막지 않는다.

handle close state table은 `REGISTERED -> CLOSING -> CLOSED|RECOVERY_REQUIRED`이고 concurrent caller는 진행 중 attempt의 같은 success/failure snapshot을 기다린다. `RECOVERY_REQUIRED` 재시도는 실패/token 불명확 이름만 다루며 `InstanceNotFoundException`은 성공으로 제거한다. cache close의 첫 실패는 primary, 이후 JMX handle 순서 → listener → front 순서로 suppressed에 추가한다. 성공 resource는 다음 attempt에서 호출하지 않고 모든 resource가 성공해야 `closeCompleted=true`다.

- [ ] **Step 4: lifecycle test와 기존 close test를 GREEN으로 만든다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheMBeanLifecycleTest' \
  --tests '*NearJCacheMBeanRegistrationTest' \
  --tests '*NearJCacheContractTest' --no-build-cache
```

Expected: 경합, reentrancy fail-fast, blocking-server lock release, retry/idempotency, concurrent close result 공유, primary/suppressed exact ordering, token ownership test가 bounded timeout 안에 모두 통과한다.

- [ ] **Step 5: lifecycle commit을 만든다**

```bash
git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistration.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanLifecycleTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt
git commit -m "feat: NearJCache JMX 정리를 cache lifecycle에 결합한다" \
  -m "Constraint: register와 close는 같은 reservation protocol에서 선형화하되 외부 호출은 lock 밖에서 수행한다
Confidence: high
Scope-risk: broad
Directive: 실패한 resource만 재시도하고 caller-owned back/provider/server는 닫지 않는다
Tested: MBean lifecycle, registration, NearJCache contract tests
Not-tested: 전체 module과 Kover는 통합 task에서 검증"
```

## Task 8: ABI, Kover, README/manual/capability 계약 동기화

**Complexity:** 중간

**Depends on:** Task 7

**Files:**

- Modify: `cache/cache-core/build.gradle.kts`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt`
- Modify: `cache/cache-core/README.md`
- Modify: `cache/cache-core/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md`
- Modify: `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md`
- Modify: `docs/cache/near-cache-capability-matrix.md`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheDocumentationTest.kt`
- Create: `docs/operations/issue-1351-nearcache-management.md`
- Create: `docs/operations/templates/issue-1351-nearcache-management.json`

- [ ] **Step 1: public/serialization ABI RED-or-regression test를 확장한다**

reflection으로 다음 descriptor를 exact하게 확인한다.

```kotlin
NearJCache::class.java.getConstructor(
    javax.cache.Cache::class.java,
    javax.cache.Cache::class.java,
    NearJCacheConfig::class.java,
)
NearJCacheManagementMXBean::class.java.getConstructor(NearJCache::class.java)
NearJCacheStatisticsMXBean::class.java.getConstructor()
NearJCacheStatisticsMXBean::class.java.getMethod("addRemovals", Long::class.javaPrimitiveType)
```

기존 5-argument Java consumer와 current 6-argument constructor를 각각 Java fixture/reflection으로 호출한다. `copy` parameter 순서/default mask, `component1`..`component6`, `ObjectStreamClass.lookup(...).serialVersionUID`, legacy/current `readObject` round-trip을 exact assertion으로 실행한다. `javap -public`은 `NearJCache`에 기존 public constructor 외 새 constructor가 없고 test factory만 synthetic인지 확인한다. 새 exception의 `serialVersionUID`, transient recovery handle, immutable remaining names도 확인한다.

- [ ] **Step 2: management package Kover exclusion을 제거한다**

`cache/cache-core/build.gradle.kts`의 다음 행과 바로 위의 임시 설명만 제거한다.

```kotlin
packages("io.bluetape4k.cache.nearcache.jcache.management")
```

testFixtures abstract class exclusion은 이 작업과 무관하므로 유지한다. threshold는 추가하지 않는다.

- [ ] **Step 3: README EN/KO와 manual EN/KO를 source-equivalent하게 갱신한다**

각 locale에 다음 순서의 예제를 넣는다.

1. `MutableConfiguration.setTypes`, `setStatisticsEnabled`, `setManagementEnabled`, `setStoreByValue(false)`.
2. caller-owned `MBeanServer`, stable non-secret `managerId/cacheId`, `registerMBeans`.
3. `JMX.newMXBeanProxy`로 configuration/statistics 조회.
4. `statistics.clear()`의 통계 초기화와 `nearCache.clear()`의 데이터 삭제를 나란히 표시.
5. `statisticsScope`, `supportedOperations`, unsupported removal/eviction capability 해석.
6. async count는 caller-visible 성공이며 remote failure는 같은 `operationId` completion으로 확인.
7. synchronous migration은 old handle close → old cache close 완료 → 새 instance/handle 생성, shutdown은 handle → cache 순서.
8. collision/recovery/stale owner runbook, exact ObjectName exclusive namespace precondition, descriptor token의 best-effort replacement detection, caller-owned back/provider/server 비소유권.
9. ID는 1..256자 non-control non-secret 값이며 ObjectName/exception에 노출될 수 있고 credential/token/PII를 넣지 않는 경계.

비동기 migration은 zero-loss global drain API가 없음을 명시한다. application은 새 write admission을 먼저 중지하고 각 mutation 직후 같은 coordinator에서 보존한 atomic `BackCacheWriteCompletion` inventory가 모두 terminal인지 확인한 뒤 old handle/cache를 닫아야 한다. 이 inventory를 보존하지 않은 async instance는 online migration을 지원하지 않으며 synchronous mode 또는 application restart/convergence 절차를 사용한다. diagnostic `operation` 문자열은 log/분류용이고 stable correlation key는 `operationId`이며, dashboard aggregation의 versioned dimension으로 사용하지 않는다.

README/manual 예제는 complete imports, Caffeine front/back/config 생성, flag matrix, Kotlin extension과 Java `NearJCacheMBeans` facade, MXBean proxy, recovery, close 순서를 포함한다. `NearJCacheDocumentationTest`가 같은 fixture를 compile/run하고 marker-delimited EN/KO snippet token parity를 검증한다. 양 locale의 stale `front-only`, `front cache only`, `로컬 캐시만` 문구는 `rg` negative check로 실패시키고 `NearJCache.clear()`=front/back data clear, `CacheStatisticsMXBean.clear()`=counter reset을 exact token으로 확인한다.

operations guide/template은 `DISABLED`, `NOT_REGISTERED`, `REGISTERED`, `RECOVERY_REQUIRED`, `CLOSING`, `CLOSED` classifier 입력(configured flags, handle state, active names)과 JMX query를 고정한다. `RECOVERY_REQUIRED`는 즉시 alert하고 `CLOSING`은 rollout 전에 정한 양수 `closing_alert_after_seconds`를 넘으면 alert한다. async remote failure query/threshold는 application별 값으로 rollout 전에 고정한다. template은 base/head/tree, artifact/config identity, canary target, query/window/threshold/result, rollback identity, MBean inventory/cleanup, owner/reviewer sign-off를 포함한다. JMX absence만으로 `DISABLED`를 판정하는 fixture/template은 validation에서 거부한다.

- [ ] **Step 4: capability matrix에 wrapper-v1 범위를 반영한다**

blocking `NearJCache` 행에 configuration snapshot, logical/tier 통계, explicit custom-domain JMX, `get/getAll/put/putAll/putIfAbsent/replace/remove/getAnd*` 지원, `loadAll/invoke/invokeAll/SuspendNearJCache` 미지원, eviction/removal capability false를 명시한다.

- [ ] **Step 5: ABI, 문서 locale parity, Kover를 검증한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests '*NearJCacheConfigCompatibilityTest' \
  --tests '*NearJCacheManagementMXBeanTest' \
  --tests '*NearJCacheStatisticsMXBeanTest' \
  --tests '*NearJCacheDocumentationTest' --no-build-cache
./gradlew :bluetape4k-cache-core:koverXmlReport --no-build-cache
python3 -m json.tool docs/operations/templates/issue-1351-nearcache-management.json >/dev/null
rg -n 'statisticsScope|supportedOperations|registerMBeans|statistics\.clear|nearCache\.clear' \
  cache/cache-core/README.md cache/cache-core/README.ko.md \
  docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/cache/near-cache-capability-matrix.md
if rg -n 'front-only|front cache only|로컬 캐시만' \
  cache/cache-core/README.md cache/cache-core/README.ko.md \
  docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/cache/near-cache-capability-matrix.md; then
  exit 1
fi
```

Expected: compatibility/documentation tests PASS, Kover XML 생성, 다섯 문서 모두 핵심 token을 포함하고 locale별 의미가 일치하며 stale front-only 표현이 0건이다. operations JSON은 parse되고 classifier/alert/identity 필수 field가 documentation test에서 검증된다.

- [ ] **Step 6: docs/coverage commit을 만든다**

```bash
git add cache/cache-core/build.gradle.kts \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheDocumentationTest.kt \
  cache/cache-core/README.md cache/cache-core/README.ko.md \
  docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/cache/near-cache-capability-matrix.md \
  docs/operations/issue-1351-nearcache-management.md \
  docs/operations/templates/issue-1351-nearcache-management.json
git commit -m "docs: NearJCache 운영 지표와 lifecycle 사용법을 공개한다" \
  -m "Constraint: README와 manual locale은 같은 API와 unsupported 범위를 설명한다
Confidence: high
Scope-risk: moderate
Directive: JMX-only dashboard는 DISABLED와 NOT_REGISTERED를 자체 구분할 수 없음을 유지한다
Tested: compatibility tests, koverXmlReport, locale token parity
Not-tested: final candidate benchmark는 후속 task에서 실행"
```

## Task 9: candidate benchmark와 전체 구현 검증

**Complexity:** 높음

**Depends on:** Task 8

**Files:**

- Create: `docs/benchmarks/raw/issue-1351/candidate/jmh.json`
- Create: `docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json`
- Create: `docs/benchmarks/raw/issue-1351/candidate/concurrency-active.json`
- Create: `docs/benchmarks/issue-1351-compare.jq`
- Modify: `docs/benchmarks/2026-08-16-issue-1351-nearcache-statistics.md`

- [ ] **Step 1: targeted test를 fresh full-module test로 확장한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:cleanTest \
  :bluetape4k-cache-core:test --no-build-cache
```

Expected: full `cache-core` test PASS. 실패 뒤 retry PASS가 나오면 원인을 조사하고 clean test를 다시 실행한다.

- [ ] **Step 2: compile/static/coverage proof를 실행한다**

Run:

```bash
./gradlew :bluetape4k-cache-core:compileKotlin \
  :bluetape4k-cache-core:compileTestKotlin \
  :bluetape4k-cache-core:detekt \
  :bluetape4k-cache-core:koverXmlReport \
  :bluetape4k-cache-lettuce:benchmarkBenchmarkCompile \
  --no-build-cache
git diff --check origin/fix/1348-lettuce-entryprocessor-atomicity...HEAD
```

Expected: 모든 task와 diff check PASS. benchmark source는 production module source에 들어가지 않는다.

- [ ] **Step 3: baseline과 같은 profile로 candidate를 기록한다**

Run:

```bash
./gradlew :bluetape4k-cache-lettuce:benchmarkBenchmarkJar --no-configuration-cache
java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
  '.*NearJCacheStatisticsBenchmark.*' \
  -p statisticsEnabled=false -t 1 -f 3 -wi 5 -i 10 -w 1s -r 1s -prof gc -rf json \
  -rff docs/benchmarks/raw/issue-1351/candidate/jmh.json
```

Expected: baseline과 같은 benchmark 5개, JDK, fork/warmup/measurement, unit을 가진 candidate JSON.

`frontHit`은 다음 loop로 baseline과 같은 thread profile을 실행한다.

```bash
for statistics in false true; do
  mkdir -p "cache/cache-lettuce/build/benchmarks/issue-1351/candidate-$statistics"
  for threads in 1 2 4 8 16; do
    java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
      '.*NearJCacheStatisticsBenchmark.frontHit.*' -p statisticsEnabled="$statistics" \
      -t "$threads" -f 3 -wi 5 -i 10 -w 1s -r 1s -prof gc -rf json \
      -rff "cache/cache-lettuce/build/benchmarks/issue-1351/candidate-$statistics/threads-$threads.json"
  done
done
jq -s 'add' cache/cache-lettuce/build/benchmarks/issue-1351/candidate-false/threads-*.json \
  > docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json
jq -s 'add' cache/cache-lettuce/build/benchmarks/issue-1351/candidate-true/threads-*.json \
  > docs/benchmarks/raw/issue-1351/candidate/concurrency-active.json
```

disabled concurrency는 baseline 대비 thread별 throughput/scoreError/rawData를 비교하고, active concurrency는 새 recorder의 contention 관찰값으로만 기록해 release hard gate로 사용하지 않는다.

- [ ] **Step 4: source/profile 동등성을 fail-closed 검증한다**

Run:

```bash
baseline_commit=$(jq -er '.harnessCommit | select(test("^[0-9a-f]{40}$"))' docs/benchmarks/raw/issue-1351/baseline/manifest.json)
git show "$baseline_commit":cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt \
  | shasum -a 256 | awk '{print $1}' > cache/cache-lettuce/build/benchmarks/issue-1351/baseline-source.sha256
shasum -a 256 cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt \
  | awk '{print $1}' > cache/cache-lettuce/build/benchmarks/issue-1351/candidate-source.sha256
diff -u cache/cache-lettuce/build/benchmarks/issue-1351/baseline-source.sha256 \
  cache/cache-lettuce/build/benchmarks/issue-1351/candidate-source.sha256
jq -S 'map({jmhVersion,benchmark,mode,threads,forks,jvm,jvmArgs,vmName,vmVersion,jdkVersion,warmupIterations,warmupTime,warmupBatchSize,measurementIterations,measurementTime,measurementBatchSize,params,scoreUnit:.primaryMetric.scoreUnit,allocationUnit:.secondaryMetrics["gc.alloc.rate.norm"].scoreUnit})' \
  docs/benchmarks/raw/issue-1351/baseline/jmh.json > cache/cache-lettuce/build/benchmarks/issue-1351/baseline-profile.json
jq -S 'map({jmhVersion,benchmark,mode,threads,forks,jvm,jvmArgs,vmName,vmVersion,jdkVersion,warmupIterations,warmupTime,warmupBatchSize,measurementIterations,measurementTime,measurementBatchSize,params,scoreUnit:.primaryMetric.scoreUnit,allocationUnit:.secondaryMetrics["gc.alloc.rate.norm"].scoreUnit})' \
  docs/benchmarks/raw/issue-1351/candidate/jmh.json > cache/cache-lettuce/build/benchmarks/issue-1351/candidate-profile.json
diff -u cache/cache-lettuce/build/benchmarks/issue-1351/baseline-profile.json \
  cache/cache-lettuce/build/benchmarks/issue-1351/candidate-profile.json
```

Expected: benchmark source SHA-256와 JMH version/JVM/args/method/mode/unit/params/fork/warmup/measurement profile이 모두 일치한다. jar SHA-256은 production code가 달라 서로 같을 수 없으므로 각각 문서에 기록한다. profiler 존재는 `gc.alloc.rate.norm` field/unit 비교로 검증한다.

- [ ] **Step 5: method별 median throughput과 allocation uncertainty를 계산한다**

`docs/benchmarks/issue-1351-compare.jq`를 다음 계약으로 작성한다.

```jq
def median:
  sort | length as $n |
  if $n == 0 then null
  elif $n % 2 == 1 then .[$n / 2 | floor]
  else (.[($n / 2) - 1] + .[$n / 2]) / 2
  end;
def max($a; $b): if $a > $b then $a else $b end;
def key: [.benchmark, (.threads | tostring), (.params | tojson)] | join("|");
def indexed: map({key: key, value: .}) | from_entries;

($b[0] | indexed) as $baseline |
($c[0] | indexed) as $candidate |
if (($baseline | keys | sort) != ($candidate | keys | sort)) then
  error("baseline/candidate benchmark cardinality or keys differ")
else [($baseline | keys[]) as $name |
  ($baseline[$name]) as $before |
  ($candidate[$name]) as $after |
  ($before.primaryMetric.rawData | flatten | median) as $beforeMedian |
  ($after.primaryMetric.rawData | flatten | median) as $afterMedian |
  ($before.secondaryMetrics["gc.alloc.rate.norm"].score) as $beforeAlloc |
  ($after.secondaryMetrics["gc.alloc.rate.norm"].score) as $afterAlloc |
  (($before.secondaryMetrics["gc.alloc.rate.norm"].scoreError // 0) +
   ($after.secondaryMetrics["gc.alloc.rate.norm"].scoreError // 0)) as $allocError |
  {
    benchmark: $name,
    beforeMedian: $beforeMedian,
    afterMedian: $afterMedian,
    beforeAlloc: $beforeAlloc,
    afterAlloc: $afterAlloc,
    allocationErrorBudget: max(0.001; $allocError),
    pass: (($afterMedian >= $beforeMedian * 0.95) and
           ($afterAlloc <= $beforeAlloc + max(0.001; $allocError)))
  }
] as $rows |
  if ($rows | all(.pass)) then $rows else error("performance regression threshold failed") end
end
```

Run:

```bash
jq -e -n --slurpfile b docs/benchmarks/raw/issue-1351/baseline/jmh.json \
  --slurpfile c docs/benchmarks/raw/issue-1351/candidate/jmh.json \
  -f docs/benchmarks/issue-1351-compare.jq
jq -e -n --slurpfile b docs/benchmarks/raw/issue-1351/baseline/concurrency.json \
  --slurpfile c docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json \
  -f docs/benchmarks/issue-1351-compare.jq
```

`issue-1351-compare.jq`는 method 이름으로 두 배열을 join하고 rawData median을 계산한다. 모든 method에서 candidate median throughput가 baseline의 95% 이상이어야 한다. allocation은 `candidateScore <= baselineScore + max(0.001, baselineScoreError + candidateScoreError)`를 만족해야 하며 score/error/rawData를 모두 출력한다. 이는 exact 0 단정이 아니라 profiler uncertainty 안에서 새 allocation이 관찰되지 않는다는 정책이다. 실패하면 Task 3–5 hot path로 돌아가며 candidate만 반복하지 않고 baseline/candidate pair 전체를 같은 환경에서 다시 기록한다. concurrency disabled profile도 thread별 metadata가 일치해야 하며 threshold는 단일-thread hard gate와 동일하게 적용한다.

- [ ] **Step 6: benchmark 문서에 candidate와 caveat를 추가하고 commit한다**

```bash
git add docs/benchmarks/raw/issue-1351/candidate/jmh.json \
  docs/benchmarks/raw/issue-1351/candidate/concurrency-disabled.json \
  docs/benchmarks/raw/issue-1351/candidate/concurrency-active.json \
  docs/benchmarks/issue-1351-compare.jq \
  docs/benchmarks/2026-08-16-issue-1351-nearcache-statistics.md
git commit -m "test: NearJCache disabled 통계 비용 상한을 검증한다" \
  -m "Constraint: allocation delta 0 B/op와 median throughput 회귀 5% 이하를 같은 환경에서 비교한다
Confidence: medium
Scope-risk: narrow
Directive: 이 로컬 snapshot을 production ranking이나 flaky unit gate로 사용하지 않는다
Tested: full cache-core test, detekt, Kover, benchmark compile/JMH candidate, diff check
Not-tested: provider별 Testcontainers는 generic cache-core 계약에 불필요"
```

## Task 10: Step 5/6/6-R, lesson, stacked PR delivery

**Complexity:** 높음

**Depends on:** Task 9

**Files:**

- Create: `docs/lessons/2026-08-16-issue-1351-nearcache-management.md`
- Create when durable review evidence is useful: `docs/review/2026-08-16-issue-1351-nearcache-management-review.md`

- [ ] **Step 1: exact spec/plan acceptance mapping을 verifier checklist로 확인한다**

각 spec §5–§14 항목을 production symbol, test method, docs section, benchmark evidence에 연결한다. 누락은 구현 Task 2–9로 되돌려 수정하고 해당 targeted/full proof를 다시 실행한다.

- [ ] **Step 2: Kotlin final checklist와 Type-A 6관점 pre-PR review를 수행한다**

performance, stability, security, Ops, developer/API, caller 6개 read-only lane과 main integration을 exact branch diff에 수행한다. P0/P1은 수정 후 affected lane과 검증을 재실행한다. P2/P3는 작은 in-scope 수정 또는 근거 있는 follow-up으로 disposition한다.

- [ ] **Step 3: lesson을 작성·검토·commit한다**

lesson에는 configuration source와 exactness, generation capture, collision ownership, retry 가능한 close, caller-visible async 통계, baseline-first benchmark에서 실제로 확인한 surprise/miss와 future guard를 기록한다. 추측이나 구현에서 발생하지 않은 교훈은 쓰지 않는다.

```bash
git add docs/lessons/2026-08-16-issue-1351-nearcache-management.md
# review artifact를 만들었다면 해당 exact path만 별도로 stage한다.
git add docs/review/2026-08-16-issue-1351-nearcache-management-review.md
git commit -m "docs: NearJCache 운영 계측의 lifecycle 교훈을 보존한다" \
  -m "Confidence: high
Scope-risk: narrow
Directive: 통계 의미, provider 완료, MBean 소유권을 하나의 상태로 합치지 않는다
Tested: exact spec-plan-diff mapping, six-perspective pre-PR review
Not-tested: hosted CI는 PR 생성 뒤 확인"
```

- [ ] **Step 4: final local proof와 exact head를 고정한다**

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test --no-build-cache
./gradlew :bluetape4k-cache-core:detekt \
  :bluetape4k-cache-core:koverXmlReport \
  :bluetape4k-cache-lettuce:benchmarkBenchmarkCompile --no-build-cache
git diff --check origin/fix/1348-lettuce-entryprocessor-atomicity...HEAD
git status --short --branch
git rev-parse HEAD
python3 -m json.tool docs/operations/templates/issue-1351-nearcache-management.json >/dev/null
```

Expected: clean worktree, P0=0/P1=0, 모든 local proof PASS, exact head 1개. review evidence는 base/head/tree, benchmark manifest/hash, collision/recovery/close/async test 결과와 operations schema validation을 연결한다. 이번 작업은 library PR이라 external canary/deployment/rollback 실행은 권한·범위 밖이며 template에 `not_applicable_reason=library_pr_only`로 명시하고 배포 증거를 꾸며내지 않는다. 실제 downstream rollout에서는 canary/query/window/threshold/result와 rollback 후 MBean inventory empty를 채우기 전 close하지 않는다.

- [ ] **Step 5: authorized stacked head를 push하고 PR을 생성·read-back한다**

모든 이전 gate와 plan approval이 PR 생성 권한을 충족하는지 먼저 재확인한다. 충족하면 `feat/1351-nearcache-management`를 push하고 base `fix/1348-lettuce-entryprocessor-atomicity`로 Korean PR을 생성한다. assignee `debop`, milestone `1.13.0`, issue의 `enhancement`, `test`, `tech-debt`, `cache` label을 맞추고 body 마지막 `##` heading을 `## DoD Status`로 둔다.

- [ ] **Step 6: exact PR head의 CI/review를 통과시키고 merge-ready에서 멈춘다**

required checks와 final aggregator, live reviews/threads, mergeability를 exact remote head에서 확인한다. green 뒤 리뷰를 다시 읽고, merge-ready DoD를 보고한 다음 fresh merge 승인을 기다린다. auto-merge는 사용하지 않는다.

## Spec-to-Task 추적성

| Spec 계약 | Plan task | 주 증거 |
|---|---|---|
| §5 configuration snapshot/type source/flags | Task 2 | `NearJCacheConfigurationSnapshotTest` |
| §6.1 logical/tier/capability | Task 3–5 | recorder + operation statistics tests |
| §6.2 operation matrix | Task 4–5 | read/mutation/compound/failure tests |
| §6.3 generation reset | Task 3 | concurrent reset/time tests |
| §6.4 disabled 비용 | Task 1, 3, 9 | fake clock + baseline/candidate JMH |
| §6.5 unsupported eviction | Task 3, 8 | capability getter + docs |
| §7.1 explicit API/security | Task 6, 8 | registration/security/docs tests |
| §7.2 flag matrix/live state | Task 6–8 | registration test + runbook |
| §7.3 collision/rollback/close | Task 6–7 | registration/lifecycle tests |
| §8 ABI | Task 2–3, 8 | reflection/Java consumer/serialization tests |
| §9 failure modes | Task 2–7 | targeted negative/lifecycle tests |
| §10 complete test strategy | Task 2–9 | targeted → full module → static/Kover/JMH |
| §11 docs/runbook | Task 8 | README/manual/capability parity |
| §12 Issue acceptance | Task 2–9 | acceptance rows 전체 |
| §13 DoD/lesson | Task 9–10 | final verifier/review/lesson evidence |
| §14 stacked train/rollout | Task 8, 10 | docs + exact base/head PR read-back |

## Step 3-R 계획 검토 결과

| 관점 | 최종 판정 | P0 | P1 | P2/P3 disposition |
|---|---:|---:|---:|---|
| performance | CLEAR | 0 | 0 | 없음 |
| stability | CLEAR | 0 | 0 | 없음 |
| security/privacy | WATCH | 0 | 0 | JMX에 token-check+unregister CAS가 없어 exclusive ObjectName namespace를 caller precondition으로 고정; descriptor token은 cleanup 전에 완료된 foreign replacement만 탐지 |
| operator/Ops | CLEAR | 0 | 0 | 없음 |
| developer/API | CLEAR | 0 | 0 | 없음 |
| caller/user | CLEAR | 0 | 0 | 없음 |

main integration은 v2.3 spec의 configuration fallback failure, generation/disabled 비용, Java facade ABI,
two-phase lifecycle, ownership 한계, async migration, operations evidence를 Task 1–10과 다시 대조했다.
P0/P1은 0이며 security WATCH는 library가 제거할 수 없는 JMX API의 atomicity 한계를 숨기지 않고
caller trust boundary와 negative test로 제한한 accepted P2다. 구현 단계에서 이 precondition을
수용할 수 없는 shared-server 환경이 발견되면 security lane을 P1/BLOCK으로 다시 연다.

## Plan DoD

- [x] 모든 production 변경은 먼저 실패하는 targeted test를 가진다.
- [x] disabled baseline은 계측 구현 전에 exact commit/JMH JSON으로 고정된다.
- [x] public/serialization ABI, lifecycle ownership, security/collision, concurrency/reset이 task와 command에 배정된다.
- [x] README EN/KO, manual EN/KO, capability matrix가 같은 지원 범위를 설명한다.
- [x] Testcontainers가 필요 없다는 범위와 benchmark의 비-container 경로가 명시된다.
- [x] six-perspective Step 3-R review가 P0=0/P1=0으로 수렴한다.
- [ ] implementation은 이 계획의 사용자 승인 뒤 시작한다.
