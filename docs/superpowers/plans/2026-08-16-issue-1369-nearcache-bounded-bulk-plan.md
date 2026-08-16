# Issue #1369 NearJCache bounded bulk front population 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `NearJCache.getAll()`의 반환 정확성과 기존 front hit 경로를 유지하면서, back hit를 front에 저장하는 기본 동작을 `BypassFront`로 바꾸고 명시한 entry-count 상한 안에서만 batch 전체를 populate한다.

**Architecture:** 공개 `Serializable` sealed policy를 `NearJCacheConfig`의 일곱 번째 property로 추가하되 prior no-arg/5/6-인자 constructor·copy descriptor와 legacy stream을 보존한다. `getAll()`은 back 결과를 모두 반환하고, mutation epoch가 같으며 정책이 허용할 때만 기존 `mutationGate` 안에서 한 번의 `frontCache.putAll`을 실행한다. 생성 시점의 immutable configuration snapshot과 MXBean에는 subtype 이름과 분리된 stable token과 상한만 노출한다.

**Tech Stack:** Kotlin 2.4, Java 25, JCache (`javax.cache`), JMX (`javax.management`), Caffeine JCache, JUnit 5, MockK, bluetape4k assertions, Java serialization/reflection compatibility tests, Gradle 9.7, Detekt, kotlinx-benchmark/JMH

**Spec:** `docs/superpowers/specs/2026-08-16-epic-1408-nearjcache-safety-tail-design.md`

**Stack:** `develop` (`05e3174ac11fc488a8c1ebc6027df3759271aa55`) → `fix/1369-nearcache-bounded-bulk` → 후속 `fix/1368-nearcache-clear-authority`

---

## 실행 경계

- 이 계획은 Epic #1408의 PR 1인 #1369만 구현한다. `NearJCacheClearAuthority`, `clear()`, `clearAllCache()`, 무인자 `removeAll()`의 권한 변경은 PR 2로 남긴다.
- `SuspendNearJCache`, provider factory signature, dependency, module registration, tenant protocol, value byte-size 측정은 변경하지 않는다.
- public API는 `BulkFrontPopulationPolicy`와 `NearJCacheConfig.bulkFrontPopulationPolicy`만 추가한다. 정책의 기본값은 `BypassFront`다.
- 상한은 `keys.size`나 `missingKeys.size`가 아니라 `backValues.size`에 적용한다. 상한을 넘으면 first-N을 선택하지 않고 batch 전체의 front populate를 우회한다.
- 이 정책은 front residency의 entry 수만 제한한다. 요청 `Set`, provider의 back 응답, 최종 반환 `MutableMap`, value byte 크기의 allocation은 제한하지 않으므로 완전한 OOM 방어로 표현하지 않는다. 이 범위를 넓히려면 serializer-independent byte budget과 request/response budget을 별도 이슈로 설계해야 한다.
- JCache provider가 `getAll()`에서 반환한 `Map`은 해당 호출이 끝날 때까지 동기적으로 안정적이라는 기존 신뢰 경계를 유지한다. 방어적 전체 복사는 공격 입력의 heap amplification을 한 번 더 만들기 때문에 이 PR에서는 거부한다. 이 계약을 지키지 않는 custom provider의 동시 map mutation은 비범위로 명시한다.
- 기존 front hit, back result, logical/tier hit·miss 통계, epoch fencing, exception·cancellation 계약은 유지한다.
- production/KDoc와 내부 계획·lesson은 한국어로 작성한다. `README.md`는 영어, `README.ko.md`는 한국어로 동등하게 갱신한다.
- 구현은 `$test-driven-development`, `$bluetape-kotlin-patterns`, testing reference를 다시 읽은 뒤 시작한다. RED를 확인하지 않은 production 변경은 허용하지 않는다.
- PR 생성 대상은 `bluetape4k/bluetape4k-projects`, base는 `develop`, head는 `fix/1369-nearcache-bounded-bulk`다. push와 PR 생성은 구현·검증·review·lesson이 끝난 뒤 수행하며 merge는 exact head merge-ready 보고 뒤 별도 fresh 승인을 받는다.

## 파일 구조와 책임

| 파일 | 변경 책임 |
| --- | --- |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicy.kt` | safe default와 bounded opt-in을 나타내는 공개 serializable policy |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt` | 일곱 번째 policy property, 5/6-인자 ABI, legacy stream 기본값 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt` | DSL 기본값과 명시 정책 전달 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt` | `getAll()` 반환과 front residency 결정을 분리 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt` | stable policy token과 maximum entry count snapshot |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt` | 두 configuration attribute 공개 계약 |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt` | snapshot-backed getter 구현 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicyTest.kt` | policy validation·serialization 계약 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt` | no-arg/5/6/7-인자 ABI, copy/component, legacy/current serialization |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBinaryCompatibilityTest.kt` | precompiled 1.12.1/pre-#1369 consumer linkage와 fixture manifest |
| `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/LegacyNearJCacheConfigConsumer.java` | source-level 1.12.1 5-인자 Java descriptor 회귀 |
| `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/CurrentNearJCacheConfigConsumer.java` | current 6/7-인자 Java constructor/copy/getter 회귀 |
| `cache/cache-core/src/test/resources/compat/issue-1369/**` | 1.12.1 및 pre-#1369 Kotlin/Java precompiled consumer jar·source·manifest |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt` | DSL safe default와 bounded 전달 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt` | default bypass, threshold, oversized all-or-nothing, edge/failure/lifecycle |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt` | mixed front/back/miss logical·tier 통계 불변 |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt` | policy-to-stable-metadata mapping과 immutable snapshot |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt` | MXBean token/limit getter |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt` | 실제 MBeanServer 등록 후 stable attribute read-back |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheDocumentationTest.kt` | README 양언어와 capability matrix 계약 토큰 |
| `cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheBulkPopulationBenchmark.kt` | 변경 대상 back-path와 contention의 동일 harness baseline/candidate |
| `cache/cache-core/README.md`, `cache/cache-core/README.ko.md` | safe default, bounded opt-in, 결과와 residency 분리 설명 |
| `cache/cache-lettuce/README.md`, `cache/cache-lettuce/README.ko.md` | 동기 factory caller migration과 bounded opt-in |
| `cache/cache-hazelcast/README.md`, `cache/cache-hazelcast/README.ko.md` | listener-free 동기 factory에도 적용되는 caller migration |
| `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md`, `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md` | public factory/config의 upgrade와 운영 절차 |
| `docs/cache/near-cache-capability-matrix.md` | blocking `NearJCache` bulk policy와 stable metadata 경계 |
| `docs/operations/issue-1351-nearcache-management.md`, `docs/operations/templates/issue-1351-nearcache-management.json` | #1369 JMX canary evidence와 safe rollback 실행 계약 |
| `docs/benchmarks/raw/issue-1369/**` | raw JMH, 비교 결과, commit/tree/JAR/source/environment manifest |
| `docs/lessons/2026-08-16-issue-1369-nearcache-bounded-bulk.md` | 구현 결과, 검증, 실패·놀람, 후속 guard |

provider 모듈 production/test 파일, `CHANGELOG.md`, `AGENTS.md`, Gradle 설정은 PR 1에서 수정하지 않는다. `NearJCacheConfig`를 그대로 전달하는 Lettuce/Hazelcast 동기 factory에는 새 safe default가 자동 적용되므로 해당 README 쌍과 core manual 쌍은 갱신하되 provider Testcontainers는 필수 proof로 삼지 않는다. Redisson README에는 동기 `NearJCache` factory/example이 없음을 inventory로 재확인한다. symbol search에서 별도 정책 변환이나 추가 public factory가 발견되면 문서 scope를 다시 검토한다.

## dependency order와 commit 경계

```text
Task 1 baseline
  -> Task 2 policy/config compatibility
  -> Task 2A changed-path benchmark harness + baseline
  -> Task 3 getAll residency behavior/statistics
  -> Task 4 management metadata
  -> Task 5 docs/parity
  -> Task 6 candidate performance + full verification/review/lesson/PR
```

각 task의 production write scope는 앞 task의 green commit 위에서만 연다. 병렬 구현은 사용하지 않는다. 독립 read-only review만 병렬화하며 Testcontainers 명령은 실행하지 않는다.

## Task 1: front-hit performance 기준선 고정

**Complexity:** 낮음

**Depends on:** 없음

**Files:**

- Read: `cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt`
- Generate ignored evidence: `cache/cache-lettuce/build/benchmarks/issue-1369/baseline.json`
- Generate ignored evidence: `cache/cache-lettuce/build/benchmarks/issue-1369/baseline-environment.txt`

- [ ] **Step 1: benchmark가 front-only `bulkHit`을 측정하는지 확인한다**

Run:

```bash
rg -n 'fun bulkHit|nearCache.getAll\(keys\)|nearCache.putAll\(entries\)' \
  cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheStatisticsBenchmark.kt
```

Expected: setup이 entry를 미리 저장하고 `bulkHit()`이 `nearCache.getAll(keys)`를 호출한다. 이 경로는 `missingKeys.isEmpty()`에서 종료하므로 새 policy 분기 이전의 front-hit 비용을 측정한다.

- [ ] **Step 2: 현재 base-equivalent source로 benchmark jar를 만들고 기준선을 기록한다**

Run:

```bash
./gradlew :bluetape4k-cache-lettuce:benchmarkBenchmarkJar --no-configuration-cache
mkdir -p cache/cache-lettuce/build/benchmarks/issue-1369
java -version > cache/cache-lettuce/build/benchmarks/issue-1369/baseline-environment.txt 2>&1
java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
  '.*NearJCacheStatisticsBenchmark.bulkHit.*' \
  -p statisticsEnabled=false -t 1 -f 3 -wi 5 -i 10 -w 1s -r 1s -prof gc -rf json \
  -rff cache/cache-lettuce/build/benchmarks/issue-1369/baseline.json
```

Expected: benchmark 1개가 성공하고 JSON에 throughput과 `gc.alloc.rate.norm`이 기록된다. 이 task는 tracked file을 만들지 않으므로 commit하지 않는다.

## Task 2: public policy와 config 호환성

**Complexity:** 높음

**Depends on:** Task 1

**Pattern skills:** `$test-driven-development`, `$bluetape-kotlin-patterns`; tests trigger `references/testing.md`

**Files:**

- Create: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicy.kt`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicyTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt`
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBinaryCompatibilityTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt`
- Modify: `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/LegacyNearJCacheConfigConsumer.java`
- Create: `cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/CurrentNearJCacheConfigConsumer.java`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/nearjcache-config-1.12.1-consumers.jar`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/nearjcache-config-pre-1369-consumers.jar`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/src/NearJCacheConfigConsumer1121.kt`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/src/NearJCacheConfigConsumer1121.java`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/src/NearJCacheConfigConsumerPre1369.kt`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/src/NearJCacheConfigConsumerPre1369.java`
- Create: `cache/cache-core/src/test/resources/compat/issue-1369/manifest.json`

- [ ] **Step 1: policy validation과 round-trip RED test를 작성한다**

```kotlin
class BulkFrontPopulationPolicyTest {
    @Test
    fun `PopulateIfAtMost는 양수 상한만 허용한다`() {
        listOf(0, -1).forEach { maximumEntryCount ->
            assertFailsWith<IllegalArgumentException> {
                BulkFrontPopulationPolicy.PopulateIfAtMost(maximumEntryCount)
            }
        }
    }

    @Test
    fun `각 policy는 Java serialization round trip을 유지한다`() {
        listOf(
            BulkFrontPopulationPolicy.BypassFront,
            BulkFrontPopulationPolicy.PopulateIfAtMost(2),
        ).forEach { policy ->
            ObjectStreamClass.lookup(policy.javaClass).serialVersionUID shouldBeEqualTo 1L
            deserialize<BulkFrontPopulationPolicy>(serialize(policy)) shouldBeEqualTo policy
        }
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { output -> output.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { input -> input.readObject() as T }
}
```

직렬화 helper는 test 파일 내부에서 `ObjectOutputStream`과 `ObjectInputStream`을 `use`로 닫는다.

- [ ] **Step 2: 1.12.1/pre-#1369 precompiled consumer와 no-arg/5/6/7-인자 ABI RED를 추가한다**

Task 2 production edit 전에 두 consumer jar를 만든다. published 기준 좌표는 `io.github.bluetape4k:bluetape4k-cache-core:1.12.1`, URL은 Maven Central의 `https://repo1.maven.org/maven2/io/github/bluetape4k/bluetape4k-cache-core/1.12.1/bluetape4k-cache-core-1.12.1.jar`, pinned SHA-256은 `0943cea523c581b82ecdbef700a9e8629e669ceca60cfd1d6ba02986d39d59e6`다. compile dependency `javax.cache:cache-api:1.1.1`의 pinned SHA-256은 `9f34e007edfa82a7b2a2e1b969477dcf5099ce7f4f926fb54ce7e27c4a0cd54b`다. 다운로드와 build staging은 repository rule에 따라 `.codex/lib-sources/issue-1369/`만 사용한다. pre-#1369 jar는 base commit `05e3174ac11fc488a8c1ebc6027df3759271aa55`과 cache-core source/build diff가 없음을 먼저 확인한 현재 worktree에서 만든다.

```bash
set -euo pipefail
STAGING=.codex/lib-sources/issue-1369
mkdir -p "$STAGING/downloads" "$STAGING/classes-1121" "$STAGING/classes-pre1369"
curl --fail --location --proto '=https' \
  https://repo1.maven.org/maven2/io/github/bluetape4k/bluetape4k-cache-core/1.12.1/bluetape4k-cache-core-1.12.1.jar \
  --output "$STAGING/downloads/bluetape4k-cache-core-1.12.1.jar"
curl --fail --location --proto '=https' \
  https://repo1.maven.org/maven2/javax/cache/cache-api/1.1.1/cache-api-1.1.1.jar \
  --output "$STAGING/downloads/cache-api-1.1.1.jar"
printf '%s  %s\n' \
  0943cea523c581b82ecdbef700a9e8629e669ceca60cfd1d6ba02986d39d59e6 \
  "$STAGING/downloads/bluetape4k-cache-core-1.12.1.jar" |
  shasum -a 256 -c -
printf '%s  %s\n' \
  9f34e007edfa82a7b2a2e1b969477dcf5099ce7f4f926fb54ce7e27c4a0cd54b \
  "$STAGING/downloads/cache-api-1.1.1.jar" |
  shasum -a 256 -c -

BASE_SHA=05e3174ac11fc488a8c1ebc6027df3759271aa55
git diff --quiet "$BASE_SHA" -- \
  cache/cache-core/src/main cache/cache-core/build.gradle.kts \
  build.gradle.kts gradle/libs.versions.toml
./gradlew :bluetape4k-cache-core:jar --no-build-cache --no-configuration-cache
BASE_JARS=(cache/cache-core/build/libs/bluetape4k-cache-core-*.jar(N))
BASE_JARS=(${BASE_JARS:#*-sources.jar})
BASE_JARS=(${BASE_JARS:#*-javadoc.jar})
(( ${#BASE_JARS[@]} == 1 ))
BASE_JAR="$BASE_JARS[1]"

MAVEN_JAR="$STAGING/downloads/bluetape4k-cache-core-1.12.1.jar"
CACHE_API="$STAGING/downloads/cache-api-1.1.1.jar"
FIXTURE_SRC=cache/cache-core/src/test/resources/compat/issue-1369/src
kotlinc "$FIXTURE_SRC/NearJCacheConfigConsumer1121.kt" \
  -classpath "$MAVEN_JAR:$CACHE_API" -jvm-target 21 -d "$STAGING/classes-1121"
javac --release 21 -classpath "$MAVEN_JAR:$CACHE_API:$STAGING/classes-1121" \
  -d "$STAGING/classes-1121" "$FIXTURE_SRC/NearJCacheConfigConsumer1121.java"
jar --create \
  --file cache/cache-core/src/test/resources/compat/issue-1369/nearjcache-config-1.12.1-consumers.jar \
  -C "$STAGING/classes-1121" .

kotlinc "$FIXTURE_SRC/NearJCacheConfigConsumerPre1369.kt" \
  -classpath "$BASE_JAR:$CACHE_API" -jvm-target 25 -d "$STAGING/classes-pre1369"
javac --release 25 -classpath "$BASE_JAR:$CACHE_API:$STAGING/classes-pre1369" \
  -d "$STAGING/classes-pre1369" "$FIXTURE_SRC/NearJCacheConfigConsumerPre1369.java"
jar --create \
  --file cache/cache-core/src/test/resources/compat/issue-1369/nearjcache-config-pre-1369-consumers.jar \
  -C "$STAGING/classes-pre1369" .
```

fixture source는 함께 commit하며 manifest는 Maven coordinate/URL/pinned artifact SHA, base commit, cache API SHA, source/artifact/consumer jar SHA-256, `java -version`/`javac -version`/`kotlinc -version`/`jar --version`, 위 exact compile argv, JVM target과 expected descriptors를 기록한다. `shasum -a 256`으로 두 consumer jar와 모든 source를 manifest에 다시 대조한다.

1.12.1 Kotlin fixture는 5-field default constructor dispatch와 `copy$default`를 강제로 호출한다.

```kotlin
object NearJCacheConfigConsumer1121 {
    @JvmStatic
    fun constructWithDefaults(): NearJCacheConfig<String, String> =
        NearJCacheConfig(cacheName = "legacy-kotlin")

    @JvmStatic
    fun copyWithDefaults(source: NearJCacheConfig<String, String>): NearJCacheConfig<String, String> =
        source.copy(cacheName = "legacy-kotlin-copy")
}
```

pre-#1369 fixture는 여섯 번째 property를 사용해 6-field synthetic dispatch를 고정한다.

```kotlin
object NearJCacheConfigConsumerPre1369 {
    @JvmStatic
    fun constructWithDefaults(): NearJCacheConfig<String, String> =
        NearJCacheConfig(cacheName = "pre-1369", syncRemoteRetryCount = 2)

    @JvmStatic
    fun copyWithDefaults(source: NearJCacheConfig<String, String>): NearJCacheConfig<String, String> =
        source.copy(syncRemoteRetryCount = 2)
}
```

각 Java fixture는 해당 기준 jar의 public no-arg constructor, direct 5/6-인자 constructor와 `copy`를 호출한다. `NearJCacheConfigBinaryCompatibilityTest`는 `URLClassLoader`의 parent를 current test classloader로 두고 두 consumer jar만 child URL로 로드한다. reflection invocation 결과에서 no-arg/direct constructor, copy/getter/component 값을 read-back한다. linkage error는 unwrap해 `NoSuchMethodError`를 테스트 failure로 노출한다. 테스트는 fixture manifest hash와 `.github/pull_request_template.md`의 cache ABI/serialization stable-publication gate도 함께 읽는다.

기존 source-level `LegacyNearJCacheConfigConsumer.java`는 5-인자 descriptor를 계속 호출하고, 새 `CurrentNearJCacheConfigConsumer.java`는 6/7-인자 direct constructor/copy와 policy getter를 호출한다.

production edit 전에 다음을 실행한다.

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBinaryCompatibilityTest' \
  --no-build-cache --no-configuration-cache
```

Expected RED: pre-#1369 6-field fixture는 통과하지만 1.12.1 Kotlin default-dispatch fixture가 old synthetic constructor 또는 `copy$default`의 `NoSuchMethodError`로 실패한다. 이는 #1412 source-recompiled fixture가 놓친 실제 prior-release linkage gap이며, #1369의 API compatibility acceptance 안에서 hidden bridge로 함께 고정한다.

`NearJCacheConfigCompatibilityTest`에서 다음을 고정한다.

```kotlin
configClass.getConstructor(factoryType, String::class.java, configurationType,
    Boolean::class.javaPrimitiveType, Long::class.javaPrimitiveType).shouldNotBeNull()
configClass.getConstructor(factoryType, String::class.java, configurationType,
    Boolean::class.javaPrimitiveType, Long::class.javaPrimitiveType,
    Int::class.javaPrimitiveType).shouldNotBeNull()
configClass.getConstructor(factoryType, String::class.java, configurationType,
    Boolean::class.javaPrimitiveType, Long::class.javaPrimitiveType,
    Int::class.javaPrimitiveType, BulkFrontPopulationPolicy::class.java).shouldNotBeNull()
configClass.getConstructor().shouldNotBeNull()

(1..7).forEach { component -> configClass.getMethod("component$component").shouldNotBeNull() }
configClass.getMethod("component7").returnType shouldBeEqualTo BulkFrontPopulationPolicy::class.java
```

기존 5-인자 consumer와 새 6-인자 reflection constructor/copy가 모두 `BypassFront`를 복원하고, 7-인자 primary `copy`와 `copy$default`가 명시한 `PopulateIfAtMost(2)`를 유지하게 한다. 기존 `LegacyNearJCacheConfig` stream test에는 다음 assertion을 추가한다.

Kotlin source caller도 `bounded.copy(syncRemoteRetryCount = 2)`가 bounded policy를 보존하는지 compile/runtime로 확인한다. Java precompiled/source fixture의 `new NearJCacheConfig<>()`와 reflection `getConstructor()`가 public `()V` descriptor를 고정한다. 5/6-인자 compatibility `copy`를 reflection으로 호출하면 의도한 `BypassFront`를 얻어야 하며, named/default argument 호출에 overload ambiguity가 없어야 한다. 이 compatibility overload가 bounded policy를 유지하지 않고 safe `BypassFront`로 재설정하는 이유를 overload KDoc과 migration 문서에 명시하고, bounded source에서 reflection으로 old copy를 호출하는 regression test로 그 의도를 고정한다.

```kotlin
restored.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
```

현재 stream test는 다음처럼 명시 정책을 확인한다.

```kotlin
val original = NearJCacheConfig<String, String>(
    syncRemoteRetryCount = 0,
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
)
val restored = deserializeAsCurrent(serialize(original))
restored.bulkFrontPopulationPolicy shouldBeEqualTo original.bulkFrontPopulationPolicy
```

Java serialization이 `init`을 우회하는 경계를 고정하기 위해 test에서 reflection으로 `PopulateIfAtMost.maximumEntryCount`를 `0`과 `-1`로 변조한 뒤 config stream을 만든다. 두 stream은 `InvalidObjectException`을 발생시켜야 한다. policy field를 명시적 `null`로 만든 stream은 legacy 누락과 같은 safe fallback인 `BypassFront`로 복원한다. 예외 메시지에는 config 전체, cache name, key/value가 들어가지 않아야 한다.

이 파일에서 수정하는 assertion block의 `org.junit.jupiter.api.Assertions` 호출은
`bluetape4k-assertions`의 `shouldBeEqualTo`, `shouldNotBeNull`로 함께 옮긴다.

- [ ] **Step 3: builder safe default와 explicit policy RED test를 작성한다**

```kotlin
config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront

val bounded = nearJCacheConfig<String, String> {
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(3)
}
bounded.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.PopulateIfAtMost(3)
```

- [ ] **Step 4: targeted test가 기대한 이유로 실패하는지 확인한다**

Run:

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.BulkFrontPopulationPolicyTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBinaryCompatibilityTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigCompatibilityTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBuilderTest' \
  --no-build-cache --no-configuration-cache
```

Expected: bridge-only RED에서 관찰한 old linkage gap과 별도로 `BulkFrontPopulationPolicy`와 `bulkFrontPopulationPolicy`가 없어서 compile/test가 실패한다. unrelated failure면 구현하지 않고 먼저 원인을 분리한다.

- [ ] **Step 5: 최소 policy와 config 호환 구현을 작성한다**

`BulkFrontPopulationPolicy.kt`:

```kotlin
package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.support.requirePositiveNumber
import java.io.Serializable

/** `getAll`의 back hit를 front에 저장할지 결정하는 정책입니다. */
sealed interface BulkFrontPopulationPolicy: Serializable {
    /** back hit를 반환하지만 front에는 저장하지 않습니다. */
    data object BypassFront: BulkFrontPopulationPolicy {
        private const val serialVersionUID: Long = 1L
    }

    /** back hit 수가 [maximumEntryCount] 이하일 때만 batch 전체를 front에 저장합니다. */
    data class PopulateIfAtMost(
        val maximumEntryCount: Int,
    ): BulkFrontPopulationPolicy {
        init {
            maximumEntryCount.requirePositiveNumber("maximumEntryCount")
        }

        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
```

`NearJCacheConfig` primary constructor 마지막에 다음 property를 추가한다.

```kotlin
val bulkFrontPopulationPolicy: BulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
```

기존 5-인자 overload는 retry와 policy 기본값을 모두 전달한다. 현재 6-인자 descriptor를 보존하는 overload와 copy를 추가한다.

```kotlin
constructor(
    cacheManagerFactory: Factory<CacheManager>,
    cacheName: String,
    frontCacheConfiguration: MutableConfiguration<K, V>,
    isSynchronous: Boolean,
    syncRemoteTimeout: Long,
    syncRemoteRetryCount: Int,
) : this(
    cacheManagerFactory,
    cacheName,
    frontCacheConfiguration,
    isSynchronous,
    syncRemoteTimeout,
    syncRemoteRetryCount,
    BulkFrontPopulationPolicy.BypassFront,
)

fun copy(
    cacheManagerFactory: Factory<CacheManager>,
    cacheName: String,
    frontCacheConfiguration: MutableConfiguration<K, V>,
    isSynchronous: Boolean,
    syncRemoteTimeout: Long,
    syncRemoteRetryCount: Int,
): NearJCacheConfig<K, V> = NearJCacheConfig(
    cacheManagerFactory,
    cacheName,
    frontCacheConfiguration,
    isSynchronous,
    syncRemoteTimeout,
    syncRemoteRetryCount,
    BulkFrontPopulationPolicy.BypassFront,
)
```

새 7-field primary가 Kotlin compiler의 default-dispatch descriptor를 바꾸므로 direct 5/6 overload만으로는 충분하지 않다. 1.12.1 5-field와 pre-#1369 6-field synthetic constructor를 `DeprecationLevel.HIDDEN` secondary constructor로 보존한다. 각 bridge는 기존 mask bit로 old field의 source/default를 복원하고 새 field는 safe default로 고정한다.

```kotlin
@Deprecated("Binary compatibility bridge", level = DeprecationLevel.HIDDEN)
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNUSED_PARAMETER")
constructor(
    cacheManagerFactory: Factory<CacheManager>?,
    cacheName: String?,
    frontCacheConfiguration: MutableConfiguration<K, V>?,
    isSynchronous: Boolean,
    syncRemoteTimeout: Long,
    mask: Int,
    marker: kotlin.jvm.internal.DefaultConstructorMarker?,
) : this(
    cacheManagerFactory = if (mask and 0x01 != 0) CaffeineCacheManagerFactory else requireNotNull(cacheManagerFactory),
    cacheName = if (mask and 0x02 != 0) "near-jcache-" + Base58.randomString(8) else requireNotNull(cacheName),
    frontCacheConfiguration = if (mask and 0x04 != 0) getDefaultFrontCacheConfiguration() else requireNotNull(frontCacheConfiguration),
    isSynchronous = if (mask and 0x08 != 0) false else isSynchronous,
    syncRemoteTimeout = if (mask and 0x10 != 0) DEFAULT_SYNC_REMOTE_TIMEOUT else syncRemoteTimeout,
    syncRemoteRetryCount = DEFAULT_SYNC_REMOTE_RETRY_COUNT,
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
)
```

6-field bridge는 위 signature의 timeout 다음에 `syncRemoteRetryCount: Int`를 받고 mask `0x20`에서 retry 기본값을 복원한다. 두 hidden constructor의 마지막 JVM descriptor는 각각 `(I, DefaultConstructorMarker)`이며, 새 7-field compiler-generated descriptor와 공존해야 한다.

같은 방식으로 old static `copy$default` 두 descriptor를 companion의 `@JvmStatic` bridge로 보존한다.

```kotlin
@JvmStatic
@JvmName("copy\$default")
@Deprecated("Binary compatibility bridge", level = DeprecationLevel.HIDDEN)
fun <K: Any, V: Any> copyDefault5(
    source: NearJCacheConfig<K, V>,
    cacheManagerFactory: Factory<CacheManager>?,
    cacheName: String?,
    frontCacheConfiguration: MutableConfiguration<K, V>?,
    isSynchronous: Boolean,
    syncRemoteTimeout: Long,
    mask: Int,
    marker: Any?,
): NearJCacheConfig<K, V> {
    if (marker != null) throw UnsupportedOperationException(
        "Super calls with default arguments are not supported",
    )
    return source.copy(
        if (mask and 0x01 != 0) source.cacheManagerFactory else requireNotNull(cacheManagerFactory),
        if (mask and 0x02 != 0) source.cacheName else requireNotNull(cacheName),
        if (mask and 0x04 != 0) source.frontCacheConfiguration else requireNotNull(frontCacheConfiguration),
        if (mask and 0x08 != 0) source.isSynchronous else isSynchronous,
        if (mask and 0x10 != 0) source.syncRemoteTimeout else syncRemoteTimeout,
    )
}
```

`copyDefault6`는 retry parameter와 `0x20` source selection을 추가해 explicit 6-arg `copy`를 호출한다. `javap -p -s`와 precompiled fixtures가 public no-arg, 5/6 old, 7 new constructor 및 `copy$default` descriptor를 모두 확인한다. Kotlin이 exact hidden bridge bytecode를 만들지 못하거나 platform declaration clash가 나면 호환성을 주장하지 않고 구현을 중단해 spec/major-version 결정을 다시 연다.

`readObject`는 누락/null 필드를 안전한 기본값으로 복원하고, constructor validation을 우회한 policy를 거부한다.

```kotlin
val restoredPolicy = if (fields.defaulted("bulkFrontPopulationPolicy")) {
    BulkFrontPopulationPolicy.BypassFront
} else {
    fields.get("bulkFrontPopulationPolicy", null) as? BulkFrontPopulationPolicy
        ?: BulkFrontPopulationPolicy.BypassFront
}
if (
    restoredPolicy is BulkFrontPopulationPolicy.PopulateIfAtMost &&
    restoredPolicy.maximumEntryCount <= 0
) {
    throw InvalidObjectException("Invalid bulk front population policy")
}
setSerializedField("bulkFrontPopulationPolicy", restoredPolicy)
```

builder에는 같은 기본값 property를 추가하고 `build()`의 named argument로 전달한다.
`NearJCacheConfig` class KDoc에는 `@property bulkFrontPopulationPolicy`를 추가해 safe default와 `backValues.size` 기준을 설명하고, `NearJCacheConfigBuilder.bulkFrontPopulationPolicy`에도 같은 한국어 KDoc을 둔다. `rg -n '@property bulkFrontPopulationPolicy|bulkFrontPopulationPolicy'`로 두 public surface와 builder `build()` 전달을 read-back한다.

- [ ] **Step 6: targeted test를 GREEN으로 확인한다**

Task 2 Step 4의 같은 Gradle 명령을 실행한다.

그 뒤 exact bytecode descriptor와 fixture manifest를 read-back한다.

```bash
javap -classpath cache/cache-core/build/classes/kotlin/main -p -s \
  io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
jq -e '.baselines["1.12.1"].artifactSha256 and
       .baselines["pre-1369"].commit == "05e3174ac11fc488a8c1ebc6027df3759271aa55" and
       .consumers[].sha256 and .compilers.java and .compilers.kotlin and
       .expectedDescriptors.constructors and .expectedDescriptors.copyDefault' \
  cache/cache-core/src/test/resources/compat/issue-1369/manifest.json
```

Expected: 네 test class가 모두 PASS하고 precompiled 1.12.1/pre-#1369 consumers, public no-arg와 5/6/7 direct·synthetic descriptor, `component1..7`, `copy`, `copy$default`, config/policy `serialVersionUID=1L`, legacy/current/null/malformed stream을 확인한다. `javap`에는 `()V`, old 5/6과 new 7 `copy$default` 및 synthetic constructor descriptor가 함께 있어야 한다.

- [ ] **Step 7: policy/config commit을 만든다**

```bash
git add \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicy.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilder.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicyTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBinaryCompatibilityTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigBuilderTest.kt \
  cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/LegacyNearJCacheConfigConsumer.java \
  cache/cache-core/src/test/java/io/bluetape4k/cache/nearcache/jcache/CurrentNearJCacheConfigConsumer.java \
  cache/cache-core/src/test/resources/compat/issue-1369
git commit -m 'NearJCache bulk residency를 안전한 기본 정책으로 제한한다' \
  -m 'Constraint: prior 5/6-인자 JVM ABI와 serialVersionUID=1L을 유지한다
Rejected: value byte-size 측정 | generic serializer와 allocation 신뢰 경계를 추가한다
Confidence: high
Scope-risk: moderate
Directive: 새 config 필드는 legacy stream에서 BypassFront로 복원한다
Tested: BulkFrontPopulationPolicyTest, NearJCacheConfigBinaryCompatibilityTest, NearJCacheConfigCompatibilityTest, NearJCacheConfigBuilderTest
Not-tested: getAll runtime 적용은 다음 commit에서 검증'
```

## Task 2A: 변경 대상 back-path benchmark harness와 기준선 고정

**Complexity:** 높음

**Depends on:** Task 2

**Files:**

- Create: `cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheBulkPopulationBenchmark.kt`
- Create: `docs/benchmarks/raw/issue-1369/baseline/path-t1.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/contention-t1.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/contention-t2.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/contention-t4.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/contention-t8.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/contention-t16.json`
- Create: `docs/benchmarks/raw/issue-1369/baseline/manifest.json`

- [ ] **Step 1: 변경 대상 path와 오염 없는 상태 재설정을 구현한다**

한 파일에 다음 두 public benchmark class와 두 enum을 모두 정의한다. 생략한 production helper를 가정하지 않고 Caffeine JCache 생성, scenario fixture, invocation reset, teardown까지 이 파일이 소유한다.

```kotlin
enum class BulkScenario {
    FRONT_HIT_BYPASS,
    FRONT_HIT_BOUNDED,
    BACK_MISS_BYPASS,
    BACK_MISS_BOUNDED,
    BACK_HIT_BYPASS,
    BACK_HIT_BOUNDED,
    BACK_HIT_OVERSIZED,
}

enum class BulkContentionPolicy { BYPASS_FRONT, POPULATE_IF_AT_MOST }

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class NearJCacheBulkPathBenchmark {
    lateinit var frontCache: JCache<String, String>
    lateinit var backCache: JCache<String, String>
    lateinit var nearCache: NearJCache<String, String>
    lateinit var keys: Set<String>
    lateinit var entries: Map<String, String>

    @Param("FRONT_HIT_BYPASS", "FRONT_HIT_BOUNDED", "BACK_MISS_BYPASS", "BACK_MISS_BOUNDED",
        "BACK_HIT_BYPASS", "BACK_HIT_BOUNDED", "BACK_HIT_OVERSIZED")
    lateinit var scenario: BulkScenario

    @Param("1", "4", "128")
    var batchSize: Int = 1

    @Setup(Level.Trial)
    fun setup() {
        val returnedEntryCount = if (scenario == BulkScenario.BACK_HIT_OVERSIZED) batchSize + 1 else batchSize
        keys = (0 until returnedEntryCount).mapTo(linkedSetOf()) { "key-$it" }
        entries = keys.associateWith { "value-$it" }
        val configuration = benchmarkConfiguration()
        frontCache = JCaching.Caffeine.getOrCreate("issue-1369-front-${UUID.randomUUID()}", configuration)
        backCache = JCaching.Caffeine.getOrCreate("issue-1369-back-${UUID.randomUUID()}", configuration)
        if (scenario != BulkScenario.BACK_MISS_BYPASS && scenario != BulkScenario.BACK_MISS_BOUNDED) {
            backCache.putAll(entries)
        }
        nearCache = NearJCache(frontCache, backCache, NearJCacheConfig(
            frontCacheConfiguration = configuration,
            isSynchronous = true,
            bulkFrontPopulationPolicy = when (scenario) {
                BulkScenario.FRONT_HIT_BOUNDED,
                BulkScenario.BACK_MISS_BOUNDED,
                BulkScenario.BACK_HIT_BOUNDED,
                BulkScenario.BACK_HIT_OVERSIZED -> BulkFrontPopulationPolicy.PopulateIfAtMost(batchSize)
                else -> BulkFrontPopulationPolicy.BypassFront
            },
        ))
        resetFront()
        val expectedSize = when (scenario) {
            BulkScenario.BACK_MISS_BYPASS, BulkScenario.BACK_MISS_BOUNDED -> 0
            else -> entries.size
        }
        check(nearCache.getAll(keys).size == expectedSize)
        resetFront()
    }

    @Setup(Level.Invocation)
    fun resetFront() {
        frontCache.removeAll(keys)
        if (scenario.name.startsWith("FRONT_HIT")) frontCache.putAll(entries)
    }

    @Benchmark
    fun getAll(): Map<String, String> = nearCache.getAll(keys)

    @TearDown(Level.Trial)
    fun tearDown() {
        frontCache.clear()
        backCache.clear()
        nearCache.close()
        backCache.close()
    }
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class NearJCacheBulkContentionBenchmark {
    lateinit var frontCache: JCache<String, String>
    lateinit var backCache: JCache<String, String>
    lateinit var nearCache: NearJCache<String, String>

    @Param("BYPASS_FRONT", "POPULATE_IF_AT_MOST")
    lateinit var policy: BulkContentionPolicy

    @Setup(Level.Trial)
    fun setup() {
        val configuration = benchmarkConfiguration()
        frontCache = JCaching.Caffeine.getOrCreate("issue-1369-contention-front-${UUID.randomUUID()}", configuration)
        backCache = JCaching.Caffeine.getOrCreate("issue-1369-contention-back-${UUID.randomUUID()}", configuration)
        nearCache = NearJCache(frontCache, backCache, NearJCacheConfig(
            frontCacheConfiguration = configuration,
            isSynchronous = true,
            bulkFrontPopulationPolicy = when (policy) {
                BulkContentionPolicy.BYPASS_FRONT -> BulkFrontPopulationPolicy.BypassFront
                BulkContentionPolicy.POPULATE_IF_AT_MOST ->
                    BulkFrontPopulationPolicy.PopulateIfAtMost(128)
            },
        ))
    }

    @Benchmark
    fun getAll(threadState: NearJCacheBulkThreadState): Map<String, String> =
        nearCache.getAll(threadState.keys)

    @TearDown(Level.Trial)
    fun tearDown() {
        frontCache.clear()
        backCache.clear()
        nearCache.close()
        backCache.close()
    }
}

@State(Scope.Thread)
open class NearJCacheBulkThreadState {
    lateinit var keys: Set<String>

    @Setup(Level.Trial)
    fun setup(benchmark: NearJCacheBulkContentionBenchmark, params: ThreadParams) {
        keys = (0 until 128).mapTo(linkedSetOf()) { "thread-${params.threadIndex}-key-$it" }
        benchmark.backCache.putAll(keys.associateWith { "value-$it" })
    }

    @Setup(Level.Invocation)
    fun resetFront(benchmark: NearJCacheBulkContentionBenchmark) {
        benchmark.frontCache.removeAll(keys)
    }
}

private fun benchmarkConfiguration() = jcacheConfiguration<String, String> {
    setTypes(String::class.java, String::class.java)
    setStoreByValue(false)
    setStatisticsEnabled(false)
    setManagementEnabled(false)
}
```

`BACK_HIT_BOUNDED`는 `maximumEntryCount=batchSize`와 back hit `batchSize`를 사용한다. `BACK_HIT_OVERSIZED`는 같은 상한과 `batchSize + 1` back hit를 사용한다. `BACK_MISS_BYPASS`와 `BACK_MISS_BOUNDED`는 back을 비워 정책 분기 비용이 miss 경로를 바꾸지 않는지도 직접 비교한다. `Level.Invocation` setup은 score 밖에서 해당 key partition만 front에서 제거해 bounded populate가 다음 invocation을 front hit로 바꾸지 못하게 한다.

contention benchmark는 `Scope.Benchmark`의 `NearJCache` 하나를 공유하고, `Scope.Thread` auxiliary state가 thread index별 겹치지 않는 128-key partition을 가진다. 각 invocation setup이 자기 partition만 front에서 제거한다. 이 구조는 setup 비용을 측정하지 않으면서 실제 shared `mutationGate` 경쟁을 포함한다. 필요한 import는 기존 benchmark와 동일한 JMH/JCaching imports에 `ThreadParams`, `UUID`, 새 policy만 추가한다.

- [ ] **Step 2: harness compile과 fixture sanity를 확인한다**

```bash
./gradlew :bluetape4k-cache-lettuce:benchmarkBenchmarkJar --no-configuration-cache
```

Expected: benchmark jar가 생성되고 `java -jar ... -l '.*NearJCacheBulk.*'` listing에 `NearJCacheBulkPathBenchmark.getAll`, `NearJCacheBulkContentionBenchmark.getAll`이 각각 한 번 나타난다. trial preflight와 fixture size assertion이 잘못된 조합을 측정 전에 실패시킨다.

- [ ] **Step 2A: baseline 전에 harness-only commit을 만든다**

```bash
git add cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheBulkPopulationBenchmark.kt
git commit -m 'NearJCache bulk 변경 경로의 benchmark harness를 고정한다' \
  -m 'Constraint: baseline과 candidate는 같은 tracked benchmark source를 사용한다
Rejected: benchmark와 baseline 동시 commit | 측정 시점의 clean source commit을 증명하지 못한다
Confidence: high
Scope-risk: narrow
Directive: invocation reset과 thread partition fixture를 benchmark evidence와 함께 변경하지 않는다
Tested: benchmark jar compile, JMH listing, scenario preflight
Not-tested: baseline/candidate 수치는 후속 단계에서 수집'
```

Expected: harness-only commit 뒤 worktree가 clean하고 이 commit SHA와 source SHA-256이 baseline manifest의 source identity가 된다.

- [ ] **Step 3: runtime 변경 전 동일 harness 기준선을 기록한다**

```bash
set -euo pipefail
test -z "$(git status --porcelain)"
BASELINE_MEASUREMENT_COMMIT="$(git rev-parse HEAD)"
BASELINE_MEASUREMENT_TREE="$(git rev-parse 'HEAD^{tree}')"
mkdir -p docs/benchmarks/raw/issue-1369/baseline
java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
  '.*NearJCacheBulkPathBenchmark.getAll.*' \
  -t 1 -f 3 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -rf json \
  -rff docs/benchmarks/raw/issue-1369/baseline/path-t1.json
for threads in 1 2 4 8 16; do
  java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
    '.*NearJCacheBulkContentionBenchmark.getAll.*' \
    -t "$threads" -f 3 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -rf json \
    -rff "docs/benchmarks/raw/issue-1369/baseline/contention-t${threads}.json"
done
```

Expected: path matrix는 7 scenarios × 3 batch sizes, 즉 정확히 21개 unique row이고, contention은 두 정책을 `1/2/4/8/16` threads에서 각각 정확히 2개 unique row로 측정한다. 모든 row에 throughput, `scoreError`, non-empty numeric `rawData`, `gc.alloc.rate.norm`, allocation `scoreError`가 존재한다.

- [ ] **Step 4: 재현 manifest를 작성하고 read-back한다**

측정 직전 clean harness commit을 `measurementCommit`, 그 tree를 `measurementTree`, 상태를 `cleanAtMeasurement: true`로 기록한다. 측정이 끝나면 raw JSON만 먼저 commit하고 그 SHA를 `rawEvidenceCommit`으로 고정한다. 그 다음 `baseline/manifest.json`을 작성해 이 세 provenance 값과 benchmark source SHA-256, JMH jar SHA-256을 기록한다. 공통 schema는 `provenance`, `artifacts`, `invariants` 세 object다. `invariants`는 `environment`(OS/architecture/CPU, Java vendor/full version), `toolchain`(Gradle/JMH version과 공개 가능한 JVM args), `profile`(fork/warmup/measurement, thread profiles), `fixture`(scenario/policy/batch)를 포함한다. `toolchain.jvmArgs`는 raw `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, Gradle daemon argv를 복사하지 않고 benchmark process에 명시된 memory/GC allowlist만 기록한다. `-D` property, file/path option, control character, `password|secret|token|credential|apiKey|auth`가 들어간 값은 기록 전에 거부한다. benchmark source SHA는 두 run에서 같아야 하지만 schema상 `artifacts.benchmarkSourceSha256`에만 둔다. 이렇게 하면 측정 시점의 clean source commit, 생성된 raw evidence commit, manifest를 담는 후속 commit의 의미가 섞이지 않는다.

```bash
BASELINE_MEASUREMENT_COMMIT="$(git rev-parse HEAD)"
BASELINE_MEASUREMENT_TREE="$(git rev-parse 'HEAD^{tree}')"
git add docs/benchmarks/raw/issue-1369/baseline/*.json
git commit -m 'NearJCache bulk 변경 전 raw 성능 증거를 고정한다' \
  -m 'Constraint: 생성된 raw artifact는 측정 시점의 clean source commit과 분리해 추적한다
Confidence: high
Scope-risk: narrow
Tested: JMH path와 contention profile 완주
Not-tested: manifest invariant와 candidate 비교는 후속 단계에서 검증'
BASELINE_RAW_EVIDENCE_COMMIT="$(git rev-parse HEAD)"
test "$BASELINE_RAW_EVIDENCE_COMMIT" != "$BASELINE_MEASUREMENT_COMMIT"
```

`apply_patch`로 manifest를 작성한 뒤 다음으로 read-back한다.

```bash
BASELINE_MEASUREMENT_COMMIT="$(jq -r '.provenance.measurementCommit' docs/benchmarks/raw/issue-1369/baseline/manifest.json)"
BASELINE_MEASUREMENT_TREE="$(jq -r '.provenance.measurementTree' docs/benchmarks/raw/issue-1369/baseline/manifest.json)"
BASELINE_RAW_EVIDENCE_COMMIT="$(jq -r '.provenance.rawEvidenceCommit' docs/benchmarks/raw/issue-1369/baseline/manifest.json)"
jq -e '.provenance.measurementCommit and
   .provenance.measurementTree and
   .provenance.cleanAtMeasurement == true and
   .provenance.rawEvidenceCommit and
   .provenance.rawEvidenceCommit != .provenance.measurementCommit and
   (.artifacts.benchmarkSourceSha256 | type == "string" and length == 64) and
   (.artifacts.jmhJarSha256 | type == "string" and length == 64) and
   .invariants.environment.os and .invariants.environment.cpu and
   .invariants.environment.java and .invariants.toolchain.gradle and
   .invariants.toolchain.jmh and
   (.invariants.toolchain.jvmArgs | type == "array" and
     all(.[];
       type == "string" and
       (test("[[:cntrl:]]") | not) and
       (test("password|secret|token|credential|apiKey|auth"; "i") | not) and
       (test("^-X(ms|mx|ss)[0-9]+[kKmMgG]?$|^-XX:[+-][A-Za-z0-9_.]+$|^-XX:[A-Za-z0-9_.]+=[A-Za-z0-9_.:+-]+$") )) and
   .invariants.profile.forks == 3 and
   .invariants.profile.threads == [1,2,4,8,16] and
   .invariants.fixture.scenarioCount == 7 and
   .invariants.fixture.batchSizes == [1,4,128]' \
  docs/benchmarks/raw/issue-1369/baseline/manifest.json
test "$(git rev-parse "${BASELINE_MEASUREMENT_COMMIT}^{tree}")" = "$BASELINE_MEASUREMENT_TREE"
test "$(git rev-parse HEAD)" = "$BASELINE_RAW_EVIDENCE_COMMIT"
BENCHMARK_SOURCE=cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheBulkPopulationBenchmark.kt
JMH_JAR=cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar
test "$(shasum -a 256 "$BENCHMARK_SOURCE" | awk '{print $1}')" = \
  "$(jq -r '.artifacts.benchmarkSourceSha256' docs/benchmarks/raw/issue-1369/baseline/manifest.json)"
test "$(shasum -a 256 "$JMH_JAR" | awk '{print $1}')" = \
  "$(jq -r '.artifacts.jmhJarSha256' docs/benchmarks/raw/issue-1369/baseline/manifest.json)"
jq -e -n '
  def safe_jvm_arg:
    type == "string" and
    (test("[[:cntrl:]]") | not) and
    (test("password|secret|token|credential|apiKey|auth"; "i") | not) and
    test("^-X(ms|mx|ss)[0-9]+[kKmMgG]?$|^-XX:[+-][A-Za-z0-9_.]+$|^-XX:[A-Za-z0-9_.]+=[A-Za-z0-9_.:+-]+$");
  (["-Xms512m", "-Xmx2g", "-XX:+UseZGC"] | all(.[]; safe_jvm_arg)) and
  (["-Dpassword=secret", "-Xlog:file=/tmp/gc.log", "-Xms512m\nforged"] |
    all(.[]; safe_jvm_arg | not))'
git diff --check
```

- [ ] **Step 5: baseline evidence commit을 만든다**

```bash
git add docs/benchmarks/raw/issue-1369/baseline
git commit -m 'NearJCache bulk 변경 경로의 성능 기준선을 고정한다' \
  -m 'Constraint: runtime 변경 전후에 같은 committed harness와 fixture를 사용한다
Rejected: front-hit 단일 측정 | 실제 back-path와 mutationGate 경쟁을 검증하지 못한다
Confidence: high
Scope-risk: narrow
Directive: invocation별 front reset과 thread별 key partition을 제거하지 않는다
Tested: benchmark jar compile, path matrix, 1/2/4/8/16-thread contention, manifest read-back
Not-tested: bounded runtime candidate는 다음 commit 이후 측정'
```

## Task 3: `getAll` 반환 정확성과 bounded residency 분리

**Complexity:** 높음

**Depends on:** Task 2A

Task 2A의 baseline raw data·manifest가 검증된 commit 위에서만 runtime test와 production write scope를 연다.

**Pattern skills:** `$test-driven-development`, `$bluetape-kotlin-patterns`; tests trigger `references/testing.md`

**Files:**

- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt`

- [ ] **Step 1: default bypass와 front-hit 보존 RED test를 작성한다**

```kotlin
@Test
fun `getAll 기본 정책은 back hit를 반환하고 front populate를 우회한다`() {
    every { frontCache.getAll(setOf("front", "remote")) } returns mutableMapOf("front" to "front-value")
    every { backCache.getAll(setOf("remote")) } returns mutableMapOf("remote" to "back-value")
    val cache = newNearCache(frontCache, backCache)

    cache.getAll(setOf("front", "remote")) shouldBeEqualTo
        mapOf("front" to "front-value", "remote" to "back-value")

    verify(exactly = 1) { backCache.getAll(setOf("remote")) }
    verify(exactly = 0) { frontCache.putAll(any()) }
}
```

기존 `front hit만 있으면 back을 조회하지 않는다` test는 그대로 유지해 policy 판정과 back 호출이 front-only path에 들어오지 않음을 증명한다.
기존 `표준 Cache getAll은 front와 back 결과를 한 번씩 병합하고 populate한다`는 기본 config를 쓰므로 그대로 두지 않는다. 이름을 `표준 Cache getAll은 bounded opt-in에서 front와 back 결과를 병합하고 populate한다`로 바꾸고 `PopulateIfAtMost(1)`을 명시한다. 새 default-bypass test와 이 opt-in test를 별도 계약으로 유지한다.
test helper는 config를 선택적으로 받도록 바꾼다.

```kotlin
private fun newNearCache(
    frontCache: JCache<String, String>,
    backCache: JCache<String, String>,
    config: NearJCacheConfig<String, String> = NearJCacheConfig(isSynchronous = true),
): NearJCache<String, String> = NearJCache(frontCache, backCache, config)
```

- [ ] **Step 2: bounded threshold와 oversized all-or-nothing RED test를 작성한다**

```kotlin
@Test
fun `bounded policy는 back hit 수가 상한 이하이면 batch 전체를 populate한다`() {
    val backValues = mutableMapOf("a" to "1", "b" to "2")
    every { frontCache.getAll(backValues.keys) } returns mutableMapOf()
    every { backCache.getAll(backValues.keys) } returns backValues
    val cache = newNearCache(
        frontCache,
        backCache,
        NearJCacheConfig(bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2)),
    )

    cache.getAll(backValues.keys) shouldBeEqualTo backValues
    verify(exactly = 1) { frontCache.putAll(backValues) }
}

@Test
fun `bounded policy는 back hit 수가 상한을 넘으면 batch 전체 populate를 우회한다`() {
    val backValues = mutableMapOf("a" to "1", "b" to "2", "c" to "3")
    every { frontCache.getAll(backValues.keys) } returns mutableMapOf()
    every { backCache.getAll(backValues.keys) } returns backValues
    val cache = newNearCache(
        frontCache,
        backCache,
        NearJCacheConfig(bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2)),
    )

    cache.getAll(backValues.keys) shouldBeEqualTo backValues
    verify(exactly = 0) { frontCache.putAll(any()) }
    verify(exactly = 0) { frontCache.put(any(), any()) }
}
```

- [ ] **Step 3: 요청 크기가 아닌 실제 back hit 수와 큰 value 경계를 RED test로 고정한다**

요청은 100개지만 back hit가 2개인 fixture에서 `PopulateIfAtMost(2)`가 두 값을 한 batch로 저장하도록 한다. 반대로 한 개의 큰 `String` value는 byte size를 측정하지 않고 entry-count 1로 취급한다.

```kotlin
val keys = (1..100).map { "key-$it" }.toSet()
val backValues = mutableMapOf("key-1" to "one", "key-100" to "hundred")
// result == backValues, frontCache.putAll(backValues) exactly once

val largeValue = "x".repeat(1_000_000)
// PopulateIfAtMost(1): result contains largeValue and putAll is called once
```

테스트는 serialization이나 메모리 크기 추정을 호출하지 않는 대신 exact interaction을 검증한다.

stateful Caffeine JCache black-box fixture도 추가한다. `BypassFront`에서는 같은 key 집합을 두 번 읽어 같은 결과를 얻고 back `getAll`이 두 번 실행된다. bounded in-range에서는 첫 호출이 front를 채우고 두 번째 호출은 back을 다시 조회하지 않는다. 빈 key 집합과 전체 back miss는 빈 결과, front no-op, 기존 통계 계약을 유지한다.

- [ ] **Step 4: partial hit/miss 통계와 failure/lifecycle 회귀를 조정한다**

`NearJCacheOperationStatisticsTest`에 front hit 1, back hit 1, back miss 1인 `getAll` case를 두고 logical hits 2/misses 1, front hits 1/misses 2, back hits 1/misses 1을 확인한다. `NearJCacheContractTest`의 기존 epoch race, back exception, front `RuntimeException`, `CancellationException` test는 bounded policy를 명시해 populate branch를 계속 실행하도록 바꾼다.

```kotlin
val boundedConfig = NearJCacheConfig<String, String>(
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(10),
)
```

다음 concrete fixture를 추가한다.

- bounded `getAll`의 `frontCache.putAll()`이 `IllegalStateException("secret-key secret-value")`을 한 번 던지면 전체 back 결과를 반환하고 `putAll`은 정확히 한 번만 호출되며 retry가 없다. Logback `ListAppender`의 `formattedMessage`에는 `cacheName`, key/value/payload가 없고 `throwableProxy`는 `null`이다. control character와 tenant namespace를 포함한 악성 `cacheName` fixture도 log message에 나타나지 않는다.
- 같은 front failure fixture가 다음 호출에서는 성공하게 해 `withLock`이 gate를 해제하고 후속 cache operation이 끝나는지 확인한다.
- front `putAll()`의 `CancellationException`은 `assertSame(failure, thrown)`으로 identity를 확인하고, latch/gate cleanup 뒤 후속 호출이 끝나야 한다.
- `backCache.getAll()`이 runtime provider exception 또는 `CancellationException`을 던지는 두 test는 각각 동일 throwable instance를 재전파하고 `frontCache.putAll()`을 호출하지 않으며 logical/tier statistics가 증가하지 않음을 확인한다.
- 기존 epoch race는 latch release를 `finally`에 두고 reader 종료를 명시적으로 assert한다. timeout/interruption은 test failure로 기록하고 blocked virtual thread를 남기지 않는다.

`getAll()`에는 retry executor, future, 추가 background task를 도입하지 않는다. front failure를 삼키는 계약은 RuntimeException에만 적용하고, cancellation identity와 `withLock` cleanup은 회귀 계약으로 고정한다.

- [ ] **Step 5: runtime RED를 확인한다**

Run:

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheContractTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheOperationStatisticsTest' \
  --no-build-cache --no-configuration-cache
```

Expected: default bypass·oversized cases가 기존 무제한 `putAll` 때문에 실패한다. 기존 contract failure가 섞이면 해당 failure를 먼저 분리한다.

- [ ] **Step 6: policy predicate와 최소 `getAll` 변경을 구현한다**

`BulkFrontPopulationPolicy.kt`에 internal predicate를 둔다.

```kotlin
internal fun BulkFrontPopulationPolicy.shouldPopulate(backValueCount: Int): Boolean = when (this) {
    BulkFrontPopulationPolicy.BypassFront -> false
    is BulkFrontPopulationPolicy.PopulateIfAtMost -> backValueCount <= maximumEntryCount
}
```

`NearJCache.getAll()`의 front population guard만 바꾼다.

```kotlin
val backValues = backCache.getAll(missingKeys)
if (backValues.isNotEmpty()) {
    mutationGate.withLock {
        if (
            mutationEpoch.get() == observedEpoch &&
            config.bulkFrontPopulationPolicy.shouldPopulate(backValues.size)
        ) {
            try {
                frontCache.putAll(backValues)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                log.warn {
                    "NearJCache front populate failed. operation=getAll, " +
                        "provider=${frontCache.javaClass.name}, " +
                        "failureType=${e.javaClass.name}"
                }
            }
        }
    }
}
```

반환 map과 `recordGet` 계산은 수정하지 않는다. batch 일부를 `put`하는 loop, `take(n)`, serializer, 새 metric label은 추가하지 않는다.

- [ ] **Step 7: runtime GREEN과 flake-free repeat를 확인한다**

Task 3 Step 5 명령을 한 번 실행한 뒤 epoch race를 포함한 `NearJCacheContractTest`를 `--rerun-tasks`로 다시 실행한다.

Expected: 두 실행이 모두 PASS하고 default bypass, migrated bounded opt-in standard test, repeated-read black-box, empty/all-miss, `n`, `n+1`, 100-key/2-hit, large value, mixed statistics, epoch cleanup, front RuntimeException/no-retry/sanitized-log/gate reuse, front cancellation identity/gate reuse, back runtime/cancellation identity와 no-statistics-mutation case가 통과한다.

- [ ] **Step 8: runtime contract commit을 만든다**

```bash
git add \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/BulkFrontPopulationPolicy.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheOperationStatisticsTest.kt
git commit -m 'NearJCache getAll 결과와 front residency 결정을 분리한다' \
  -m 'Constraint: 반환 map과 logical/tier 통계는 정책과 무관하게 유지한다
Rejected: first-N populate | provider iteration order에 따라 residency가 달라진다
Confidence: high
Scope-risk: moderate
Directive: 상한은 backValues.size에 all-or-nothing으로 적용한다
Tested: NearJCacheContractTest, NearJCacheOperationStatisticsTest, epoch race repeat
Not-tested: MXBean metadata와 문서 계약은 후속 commit에서 검증'
```

## Task 4: immutable management metadata

**Complexity:** 중간

**Depends on:** Task 3

**Pattern skills:** `$test-driven-development`, `$bluetape-kotlin-patterns`; tests trigger `references/testing.md`

**Files:**

- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt`

- [ ] **Step 1: snapshot과 MXBean stable metadata RED test를 작성한다**

```kotlin
@Test
fun `bulk front policy를 stable token과 상한으로 snapshot한다`() {
    val bypass = snapshotOf(BulkFrontPopulationPolicy.BypassFront)
    bypass.bulkFrontPopulationPolicy shouldBeEqualTo "BYPASS_FRONT"
    bypass.bulkFrontPopulationMaximumEntryCount shouldBeEqualTo 0

    val bounded = snapshotOf(BulkFrontPopulationPolicy.PopulateIfAtMost(25))
    bounded.bulkFrontPopulationPolicy shouldBeEqualTo "POPULATE_IF_AT_MOST"
    bounded.bulkFrontPopulationMaximumEntryCount shouldBeEqualTo 25
}

private fun snapshotOf(policy: BulkFrontPopulationPolicy): NearJCacheConfigurationSnapshot =
    nearJCacheConfigurationSnapshot(
        actualFront = cacheOf(configuration<String, Long>()),
        suppliedFront = configuration<String, Long>(),
        actualBack = cacheOf(configuration<String, Long>()),
        bulkFrontPopulationPolicy = policy,
    )
```

`NearJCacheManagementMXBeanTest`는 다음 getter를 확인한다.

```kotlin
bean.getBulkFrontPopulationPolicy() shouldBeEqualTo "POPULATE_IF_AT_MOST"
bean.getBulkFrontPopulationMaximumEntryCount() shouldBeEqualTo 25
```

snapshot 생성 후 source config 참조를 바꿀 수 없도록 policy가 immutable value임도 test 설명에 명시한다.

`NearJCacheConfigurationSnapshot`과 `nearJCacheConfigurationSnapshot(...)`은 internal이므로 새 metadata에 기본값을 숨기지 않는다. `rg -n 'nearJCacheConfigurationSnapshot\(|NearJCacheConfigurationSnapshot\(' cache/cache-core/src`로 찾은 production 1곳, snapshot test 7곳, management bean test 1곳을 모두 명시 인자로 갱신한다. compile failure가 누락 호출부를 차단해야 한다.

`NearJCacheMBeanRegistrationTest`의 fixture가 policy를 받게 하고 실제 `MBeanServer`에 configuration MBean을 등록한다. `MBeanInfo.attributes`에서 exact `BulkFrontPopulationPolicy`, `BulkFrontPopulationMaximumEntryCount` 이름과 readable/non-writable descriptor를 확인한 뒤 `server.getAttribute` 또는 `JMX.newMXBeanProxy`로 `BYPASS_FRONT/0`, `POPULATE_IF_AT_MOST/25`를 각각 read-back한다. test `finally`에서 registration을 닫아 platform-global MBean을 남기지 않는다.

- [ ] **Step 2: management RED를 확인한다**

Run:

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationSnapshotTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheManagementMXBeanTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheMBeanRegistrationTest' \
  --no-build-cache --no-configuration-cache
```

Expected: snapshot fields와 MXBean methods가 없어서 compile/test가 실패한다.

- [ ] **Step 3: snapshot mapping과 MXBean getter를 구현한다**

snapshot 함수에 policy 값을 명시적으로 전달한다.

```kotlin
internal fun nearJCacheConfigurationSnapshot(
    actualFront: Cache<*, *>,
    suppliedFront: Configuration<*, *>,
    actualBack: Cache<*, *>,
    bulkFrontPopulationPolicy: BulkFrontPopulationPolicy,
): NearJCacheConfigurationSnapshot
```

stable mapping은 subtype `simpleName`을 사용하지 않는다.

```kotlin
val (policyToken, maximumEntryCount) = when (bulkFrontPopulationPolicy) {
    BulkFrontPopulationPolicy.BypassFront -> "BYPASS_FRONT" to 0
    is BulkFrontPopulationPolicy.PopulateIfAtMost ->
        "POPULATE_IF_AT_MOST" to bulkFrontPopulationPolicy.maximumEntryCount
}
```

`NearJCache` init은 `config.bulkFrontPopulationPolicy`를 snapshot 함수에 넘긴다. MXBean interface와 bean에는 다음 exact method를 추가한다.

```kotlin
fun getBulkFrontPopulationPolicy(): String
fun getBulkFrontPopulationMaximumEntryCount(): Int
```

두 MXBean getter에는 stable token과 `0`/positive limit 의미를 설명하는 한국어 KDoc을 interface와 구현에 추가한다. Task 4 GREEN 뒤 `rg -n 'getBulkFrontPopulationPolicy|getBulkFrontPopulationMaximumEntryCount'`로 interface/implementation KDoc과 method가 함께 존재하는지 read-back한다.

- [ ] **Step 4: management GREEN과 public descriptor를 확인한다**

Task 4 Step 2 명령에 `NearJCacheConfigCompatibilityTest`를 추가해 실행한다.

Expected: snapshot/direct-bean test와 실제 MBeanServer registration test가 PASS하고 reflection으로 두 public method return type이 각각 `String`, primitive `int`임을 확인한다.

- [ ] **Step 5: management metadata commit을 만든다**

```bash
git add \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshot.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationMXBean.kt \
  cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBean.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfigCompatibilityTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheConfigurationSnapshotTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheManagementMXBeanTest.kt \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/management/NearJCacheMBeanRegistrationTest.kt
git commit -m 'NearJCache bulk 정책을 stable management metadata로 노출한다' \
  -m 'Constraint: metadata는 key, value, identity, provider payload를 포함하지 않는다
Rejected: Kotlin subtype simpleName | rename과 난독화에 stable token이 흔들린다
Confidence: high
Scope-risk: narrow
Directive: BYPASS_FRONT의 maximum entry count는 적용 불가를 뜻하는 0이다
Tested: NearJCacheConfigurationSnapshotTest, NearJCacheManagementMXBeanTest, NearJCacheMBeanRegistrationTest, NearJCacheConfigCompatibilityTest
Not-tested: README와 capability matrix parity는 다음 commit에서 검증'
```

## Task 5: public documentation과 executable parity

**Complexity:** 중간

**Depends on:** Task 4

**Pattern skills:** `$bluetape-writer`, Korean naturalness checklist, `$bluetape-kotlin-patterns`

**Files:**

- Modify: `cache/cache-core/README.md`
- Modify: `cache/cache-core/README.ko.md`
- Modify: `cache/cache-lettuce/README.md`
- Modify: `cache/cache-lettuce/README.ko.md`
- Modify: `cache/cache-hazelcast/README.md`
- Modify: `cache/cache-hazelcast/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md`
- Modify: `docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md`
- Modify: `docs/cache/near-cache-capability-matrix.md`
- Modify: `docs/operations/issue-1351-nearcache-management.md`
- Modify: `docs/operations/templates/issue-1351-nearcache-management.json`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheDocumentationTest.kt`

- [ ] **Step 1: 문서 계약 RED test를 추가한다**

core/Lettuce/Hazelcast README 양언어, core manual 양언어의 전용 `issue-1369-bulk-policy` marker section과 capability matrix에서 다음 exact token을 확인한다.

```kotlin
val requiredTokens = listOf(
    "BulkFrontPopulationPolicy.BypassFront",
    "BulkFrontPopulationPolicy.PopulateIfAtMost",
    "BYPASS_FRONT",
    "POPULATE_IF_AT_MOST",
    "bulkFrontPopulationMaximumEntryCount",
    "backValues.size",
    "legacy",
)
```

`NearJCacheDocumentationTest`에서 수정하는 assertion block도
`shouldBeTrue`, `shouldBeFalse`, `shouldBeEqualTo`를 사용해 touched-test 규칙을 맞춘다.

검증은 marker 존재만 보지 않는다. 각 marker block이 default bypass, bounded all-or-nothing, single-key `get()` 비변경, 반복 back read 가능성, legacy stream safe default를 모두 포함하는지 확인한다. Lettuce/Hazelcast EN/KO provider marker에는 각각 `LettuceCaches.nearJCache`/`HazelcastCaches.nearJCache` DSL에서 `bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(128)`를 설정하는 실제 복사 가능한 예제를 두고 marker-scoped token assertion으로 고정한다. core README의 기존 무조건적 `getAll ... populate` 문장과 capability matrix의 unconditional populate 설명은 삭제 또는 조건부 설명으로 교체됐는지 negative assertion으로 검사한다. 영문/한글 block의 코드·token·숫자·section 구조를 비교한다.

- [ ] **Step 2: documentation RED를 확인한다**

Run:

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheDocumentationTest' \
  --no-build-cache --no-configuration-cache
```

Expected: 새 policy token이 문서에 없어 test가 실패한다.

- [ ] **Step 3: README 양언어와 capability matrix를 동등하게 갱신한다**

영문 예제:

```kotlin
val safeDefault = NearJCacheConfig<String, User>()
val bounded = NearJCacheConfig<String, User>(
    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(128),
)
```

문서에는 다음 계약을 명시한다.

- 기본 `BypassFront`에서도 `getAll`은 front hit와 모든 back hit를 반환한다.
- `PopulateIfAtMost(n)`은 `backValues.size <= n`일 때만 batch 전체를 front에 저장한다.
- 초과 batch는 일부만 저장하지 않는다.
- entry count는 resident byte size나 back query size 제한이 아니다.
- single-key `get()`의 read-through populate는 바뀌지 않는다.
- MXBean token과 `0`의 의미를 설명한다.
- 새 config와 legacy stream 복원은 모두 `BypassFront`가 기본이며, 반환 결과는 같아도 반복 `getAll`의 back 조회 횟수·local hit ratio·back 부하가 달라질 수 있음을 migration section에 둔다.
- 이전 무제한 batch populate mode는 복원하지 않는다. front capacity와 local heap budget을 검토한 호출자만 `PopulateIfAtMost(n)`을 명시한다.
- canary는 configuration MXBean의 policy token/limit, statistics MXBean의 `FrontHits`, `FrontMisses`, `BackHits`, `BackMisses`, 평균 get time과 외부 back read load를 배포 전후 같은 traffic window에서 비교한다. bypass 원인 전용 counter는 없다는 관측 한계도 적는다.
- 기존 `issue-1351-nearcache-management` runbook/template을 #1369 rollout evidence로 확장한다. `managementEnabled`/`statisticsEnabled`, MBean registration과 ObjectName, target/query/window, 사전 threshold, observed result, rollbackIdentity를 모두 기록하고 누락 시 canary 진행을 중단한다.
- budget 초과 시 같은 config 객체를 hot-reload한다고 쓰지 않는다. 정상 rollback은 더 작은 `PopulateIfAtMost(n)` 또는 `BypassFront`로 만든 새 wrapper의 handover다. #1369 이전 artifact는 무제한 batch populate를 복원하므로 일반 rollback에서 제외한다. break-glass로만 시간 제한, front heap cap과 traffic 제한, 책임자/종료시각, 즉시 forward-fix를 기록한 경우에 한해 사용한다.

영문과 한국어는 section 구조, 코드, 숫자, token을 source-equivalent하게 유지한다.

- [ ] **Step 4: documentation GREEN과 link/marker parity를 확인한다**

Task 5 Step 2 명령 뒤 다음을 실행한다.

```bash
rg -n 'BulkFrontPopulationPolicy|BYPASS_FRONT|POPULATE_IF_AT_MOST|backValues.size' \
  cache/cache-core/README.md cache/cache-core/README.ko.md \
  cache/cache-lettuce/README.md cache/cache-lettuce/README.ko.md \
  cache/cache-hazelcast/README.md cache/cache-hazelcast/README.ko.md \
  docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/cache/near-cache-capability-matrix.md \
  docs/operations/issue-1351-nearcache-management.md \
  docs/operations/templates/issue-1351-nearcache-management.json
git diff --check
```

Expected: test PASS, 모든 marker/token과 migration/canary 계약이 열거한 EN/KO 문서와 ops runbook/template에 있고 실제 Lettuce/Hazelcast factory opt-in 예제, JMX activation/ObjectName/evidence fields, safe rollback 경계가 marker-scoped assertion을 통과한다. 기존 무조건적 populate 설명, broken relative link, whitespace error가 없다.

- [ ] **Step 5: documentation commit을 만든다**

```bash
git add \
  cache/cache-core/README.md \
  cache/cache-core/README.ko.md \
  cache/cache-lettuce/README.md \
  cache/cache-lettuce/README.ko.md \
  cache/cache-hazelcast/README.md \
  cache/cache-hazelcast/README.ko.md \
  docs/manual/en/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/manual/ko/modules/bluetape4k-cache-core/near-cache-semantics.md \
  docs/cache/near-cache-capability-matrix.md \
  docs/operations/issue-1351-nearcache-management.md \
  docs/operations/templates/issue-1351-nearcache-management.json \
  cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheDocumentationTest.kt
git commit -m 'NearJCache bulk residency 정책을 호출자와 운영자 계약으로 고정한다' \
  -m 'Constraint: README 영어와 한국어는 code, token, 숫자를 동등하게 유지한다
Rejected: 요청 크기 제한으로 설명 | 실제 경계는 backValues.size다
Confidence: high
Scope-risk: narrow
Directive: 결과 정확성과 front residency를 같은 개념으로 설명하지 않는다
Tested: NearJCacheDocumentationTest, marker/token parity, git diff --check
Not-tested: 전체 cache-core test와 Detekt는 최종 검증에서 수행'
```

## Task 6: performance, full verification, review, lesson, PR

**Complexity:** 높음

**Depends on:** Task 5

**Pattern skills:** `$verification-before-completion`, `$requesting-code-review`, `$bluetape-writer`, `$bluetape-kotlin-patterns`

**Files:**

- Create: `docs/benchmarks/raw/issue-1369/candidate/path-t1.json`
- Create: `docs/benchmarks/raw/issue-1369/candidate/contention-t{1,2,4,8,16}.json`
- Create: `docs/benchmarks/raw/issue-1369/candidate/path-t1-comparison.json`
- Create: `docs/benchmarks/raw/issue-1369/candidate/contention-t{1,2,4,8,16}-comparison.json`
- Create: `docs/benchmarks/raw/issue-1369/candidate/manifest.json`
- Create: `docs/lessons/2026-08-16-issue-1369-nearcache-bounded-bulk.md`

- [ ] **Step 1: 같은 committed harness로 candidate와 machine-readable comparison을 실행한다**

```bash
set -euo pipefail
test -z "$(git status --porcelain)"
CANDIDATE_MEASUREMENT_COMMIT="$(git rev-parse HEAD)"
CANDIDATE_MEASUREMENT_TREE="$(git rev-parse 'HEAD^{tree}')"
./gradlew :bluetape4k-cache-lettuce:benchmarkBenchmarkJar --no-configuration-cache
java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
  '.*NearJCacheBulkPathBenchmark.getAll.*' \
  -t 1 -f 3 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -rf json \
  -rff docs/benchmarks/raw/issue-1369/candidate/path-t1.json
for threads in 1 2 4 8 16; do
  java -jar cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar \
    '.*NearJCacheBulkContentionBenchmark.getAll.*' \
    -t "$threads" -f 3 -wi 3 -i 5 -w 500ms -r 500ms -prof gc -rf json \
    -rff "docs/benchmarks/raw/issue-1369/candidate/contention-t${threads}.json"
done
```

candidate도 측정 직전 clean runtime commit/tree를 `measurementCommit`/`measurementTree`로 기록하고 `cleanAtMeasurement: true`를 증명한다. raw JSON만 먼저 commit한 SHA를 `rawEvidenceCommit`으로 기록한 뒤 candidate manifest와 comparison artifact를 후속 commit에 담는다. baseline과 candidate는 같은 `provenance`/`artifacts`/`invariants` schema와 동일한 sanitized JVM-arg allowlist를 사용한다. `invariants`에는 environment/toolchain/profile/fixture만 넣고, benchmark source SHA는 양쪽 `artifacts.benchmarkSourceSha256`에서 별도로 같음을 확인한다. 의도적으로 달라지는 commit/tree/JAR SHA는 `provenance`와 `artifacts.jmhJarSha256`에 분리한다.

```bash
CANDIDATE_MEASUREMENT_COMMIT="$(git rev-parse HEAD)"
CANDIDATE_MEASUREMENT_TREE="$(git rev-parse 'HEAD^{tree}')"
git add docs/benchmarks/raw/issue-1369/candidate/*.json
git commit -m 'NearJCache bounded bulk raw 성능 증거를 고정한다' \
  -m 'Constraint: candidate raw artifact와 측정 직전 clean runtime commit을 구분한다
Confidence: high
Scope-risk: narrow
Tested: JMH path와 contention profile 완주
Not-tested: manifest invariant와 regression threshold는 후속 단계에서 검증'
CANDIDATE_RAW_EVIDENCE_COMMIT="$(git rev-parse HEAD)"
test "$CANDIDATE_RAW_EVIDENCE_COMMIT" != "$CANDIDATE_MEASUREMENT_COMMIT"
```

manifest 작성 후 invariant field가 완전히 같은지 다음 명령으로 확인한다.

```bash
CANDIDATE_MEASUREMENT_COMMIT="$(jq -r '.provenance.measurementCommit' docs/benchmarks/raw/issue-1369/candidate/manifest.json)"
CANDIDATE_MEASUREMENT_TREE="$(jq -r '.provenance.measurementTree' docs/benchmarks/raw/issue-1369/candidate/manifest.json)"
CANDIDATE_RAW_EVIDENCE_COMMIT="$(jq -r '.provenance.rawEvidenceCommit' docs/benchmarks/raw/issue-1369/candidate/manifest.json)"
jq -e '.provenance.measurementCommit and
       .provenance.measurementTree and
       .provenance.cleanAtMeasurement == true and
       .provenance.rawEvidenceCommit and
       .provenance.rawEvidenceCommit != .provenance.measurementCommit and
       (.artifacts.benchmarkSourceSha256 | type == "string" and length == 64) and
       (.artifacts.jmhJarSha256 | type == "string" and length == 64) and
       .invariants.environment.os and .invariants.environment.cpu and
       .invariants.environment.java and .invariants.toolchain.gradle and
       .invariants.toolchain.jmh and
       (.invariants.toolchain.jvmArgs | type == "array" and
         all(.[];
           type == "string" and
           (test("[[:cntrl:]]") | not) and
           (test("password|secret|token|credential|apiKey|auth"; "i") | not) and
           (test("^-X(ms|mx|ss)[0-9]+[kKmMgG]?$|^-XX:[+-][A-Za-z0-9_.]+$|^-XX:[A-Za-z0-9_.]+=[A-Za-z0-9_.:+-]+$") )) and
       .invariants.profile.forks == 3 and
       .invariants.profile.threads == [1,2,4,8,16] and
       .invariants.fixture.scenarioCount == 7 and
       .invariants.fixture.batchSizes == [1,4,128]' \
  docs/benchmarks/raw/issue-1369/candidate/manifest.json
test "$(git rev-parse "${CANDIDATE_MEASUREMENT_COMMIT}^{tree}")" = "$CANDIDATE_MEASUREMENT_TREE"
test "$(git rev-parse HEAD)" = "$CANDIDATE_RAW_EVIDENCE_COMMIT"
BENCHMARK_SOURCE=cache/cache-lettuce/src/benchmark/kotlin/io/bluetape4k/cache/nearcache/benchmark/NearJCacheBulkPopulationBenchmark.kt
JMH_JAR=cache/cache-lettuce/build/benchmarks/benchmark/jars/bluetape4k-cache-lettuce-benchmark-jmh-1.13.0-JMH.jar
test "$(shasum -a 256 "$BENCHMARK_SOURCE" | awk '{print $1}')" = \
  "$(jq -r '.artifacts.benchmarkSourceSha256' docs/benchmarks/raw/issue-1369/candidate/manifest.json)"
test "$(shasum -a 256 "$JMH_JAR" | awk '{print $1}')" = \
  "$(jq -r '.artifacts.jmhJarSha256' docs/benchmarks/raw/issue-1369/candidate/manifest.json)"
jq -e -n \
  --slurpfile baseline docs/benchmarks/raw/issue-1369/baseline/manifest.json \
  --slurpfile candidate docs/benchmarks/raw/issue-1369/candidate/manifest.json \
  '($baseline | length == 1) and ($candidate | length == 1) and
   ($baseline[0].invariants == $candidate[0].invariants) and
   ($baseline[0].artifacts.benchmarkSourceSha256 ==
      $candidate[0].artifacts.benchmarkSourceSha256) and
   ($baseline[0].provenance.cleanAtMeasurement == true) and
   ($candidate[0].provenance.cleanAtMeasurement == true) and
   ($baseline[0].provenance.measurementCommit !=
      $candidate[0].provenance.measurementCommit) and
   ($baseline[0].provenance.rawEvidenceCommit !=
      $candidate[0].provenance.rawEvidenceCommit)'
```

각 baseline/candidate pair는 먼저 non-empty exact row count, unique benchmark/thread/params key, 필수 numeric metric을 검사한 뒤 기존 `docs/benchmarks/issue-1351-compare.jq`로 검증한다. path file은 정확히 21 rows, 각 contention file은 정확히 2 rows여야 한다. 기존 validator는 두 입력의 key set이 같을 때 throughput median이 baseline의 95% 이상이며 candidate allocation이 `baseline allocation + max(0.001, baseline scoreError + candidate scoreError)` 이하인지 계산한다. 선행 preflight가 빈 배열과 duplicate-key 축약을 차단하므로 어느 단계든 실패하면 non-zero로 종료한다.

```bash
set -euo pipefail

validate_jmh_rows() {
  local file="$1"
  local expected="$2"
  jq -e --argjson expected "$expected" '
    def row_key: [.benchmark, (.threads | tostring), (.params | tojson)] | join("|");
    type == "array" and length == $expected and length > 0 and
    ([.[] | row_key] | length == (unique | length)) and
    all(.[];
      (.benchmark | type == "string" and length > 0) and
      (.threads | type == "number") and
      (.params | type == "object") and
      (.primaryMetric.score | type == "number") and
      (.primaryMetric.scoreError | type == "number") and
      (.primaryMetric.rawData |
        type == "array" and (flatten | length > 0) and
        all(flatten[]; type == "number")) and
      (.secondaryMetrics["gc.alloc.rate.norm"].score | type == "number") and
      (.secondaryMetrics["gc.alloc.rate.norm"].scoreError | type == "number"))
  ' "$file" >/dev/null
}

run_compare() {
  local baseline="$1"
  local candidate="$2"
  local expected="$3"
  local output="$4"
  local temporary="${output}.tmp"

  validate_jmh_rows "$baseline" "$expected"
  validate_jmh_rows "$candidate" "$expected"
  rm -f -- "$output" "$temporary"
  if ! jq -n --slurpfile b "$baseline" --slurpfile c "$candidate" \
      -f docs/benchmarks/issue-1351-compare.jq >"$temporary"; then
    rm -f -- "$temporary"
    return 1
  fi
  mv -- "$temporary" "$output"
}

run_compare \
  docs/benchmarks/raw/issue-1369/baseline/path-t1.json \
  docs/benchmarks/raw/issue-1369/candidate/path-t1.json \
  21 \
  docs/benchmarks/raw/issue-1369/candidate/path-t1-comparison.json
for threads in 1 2 4 8 16; do
  run_compare \
    "docs/benchmarks/raw/issue-1369/baseline/contention-t${threads}.json" \
    "docs/benchmarks/raw/issue-1369/candidate/contention-t${threads}.json" \
    2 \
    "docs/benchmarks/raw/issue-1369/candidate/contention-t${threads}-comparison.json"
done
```

`set -euo pipefail`과 `run_compare`의 explicit failure return이 첫 실패에서 shell을 종료한다. comparison은 temporary file에만 쓰고 validator가 성공한 뒤 `mv`하므로 partial/failed output은 승격되지 않는다. 비교 실패는 재실행으로 덮지 않는 deterministic gate다. 환경 drift 또는 benchmark flake가 의심되면 이 계획을 수정해 baseline과 candidate 전체를 같은 새 profile로 다시 수집한 뒤 재승인받는다. candidate manifest의 `comparison`에는 profile별 comparison artifact path, pass/fail, throughput 95%와 allocation error-budget formula를 기록한다.

- [ ] **Step 2: targeted test와 module validation을 순서대로 실행한다**

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.nearcache.jcache.BulkFrontPopulationPolicyTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBinaryCompatibilityTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigCompatibilityTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheConfigBuilderTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheContractTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheDocumentationTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheOperationStatisticsTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationSnapshotTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheManagementMXBeanTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.management.NearJCacheMBeanRegistrationTest' \
  --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-cache-core:detekt --no-configuration-cache
git diff --check develop...HEAD
```

Expected: targeted tests, full `cache-core` tests, module Detekt, diff check가 모두 PASS한다. Docker/Testcontainers는 시작하지 않는다.

- [ ] **Step 3: compatibility와 scope를 read-back한다**

```bash
git diff --name-only develop...HEAD
git log --oneline --decorate develop..HEAD
git status --short
```

Expected: 계획에 열거한 PR 1 파일과 spec/plan/lesson만 변경되고 worktree가 clean하다. `clear`, `clearAllCache`, 무인자 `removeAll`, provider factory, `SuspendNearJCache`, dependency/Gradle file에는 diff가 없다.

- [ ] **Step 4: six-perspective code review와 main integration을 수행한다**

performance, stability, security, operator/Ops, developer/API, user/caller 관점이 exact `develop...HEAD` diff를 독립적으로 검토한다. P0/P1은 수정하고 affected test와 해당 관점을 다시 실행한다. P2/P3은 이 PR에서 수정하거나 이유와 follow-up issue를 기록한다. 종료 조건은 latest P0=0/P1=0이다.

- [ ] **Step 5: lesson을 작성하고 writer gate를 통과한다**

`docs/lessons/2026-08-16-issue-1369-nearcache-bounded-bulk.md`에 다음 section을 작성한다.

```markdown
# Issue #1369 NearJCache bounded bulk front population 교훈

## 맥락
## 결정
## 결과
## 검증 증거
## 실패 또는 예상과 달랐던 점
## 다음 변경을 위한 guard
```

실제 RED/GREEN, benchmark 수치·환경, ABI descriptor, review finding, CI 결과만 기록한다. 구현 전 가정을 결과처럼 쓰지 않는다. `$bluetape-writer`의 SPW-01~SPW-05와 Korean naturalness checklist를 통과한다.

- [ ] **Step 6: lesson과 최종 수렴 commit을 만든다**

```bash
git add \
  docs/benchmarks/raw/issue-1369/candidate \
  docs/lessons/2026-08-16-issue-1369-nearcache-bounded-bulk.md
git commit -m 'NearJCache bounded bulk의 호환성과 성능 증거를 보존한다' \
  -m 'Constraint: 구현 결과와 fresh 검증만 durable lesson에 기록한다
Confidence: high
Scope-risk: narrow
Directive: bulk policy 변경은 result correctness와 residency를 별도로 검증한다
Tested: cache-core targeted/full test, detekt, JMH baseline/candidate, six-perspective review, git diff --check
Not-tested: provider Testcontainers는 factory signature와 provider behavior가 변경되지 않아 제외'
```

- [ ] **Step 7: exact head를 push하고 stacked PR 1을 생성한다**

CG-11~CG-13과 PR template을 새로 읽는다. push보다 먼저 `.omx/issue-1369-pr-body.md`를 `apply_patch`로 만들고 다음 순서의 한국어 section을 채운다: 요약, stacked train, 변경, 호출자 마이그레이션, 카나리/롤백, 검증, 위험/비범위, `## DoD Status`. #1369를 연결하고 #1408/1.13.0 최종 release checklist가 migration note/CHANGELOG 반영 여부를 소유한다는 handoff를 넣는다. benchmark 수치와 제한, provider Testcontainers N/A 근거, exact head를 포함한다.

```bash
test -s .omx/issue-1369-pr-body.md
rg -n '#1369|#1408|fix/1369-nearcache-bounded-bulk|fix/1368-nearcache-clear-authority|호출자 마이그레이션|카나리|롤백|benchmark|## DoD Status' \
  .omx/issue-1369-pr-body.md
rg '^## ' .omx/issue-1369-pr-body.md | tail -1
```

Expected: 필수 token이 모두 있고 마지막 출력이 `## DoD Status`다. 그 뒤에만 다음을 실행한다.

```bash
git push -u origin fix/1369-nearcache-bounded-bulk
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head fix/1369-nearcache-bounded-bulk \
  --assignee debop \
  --milestone 1.13.0 \
  --label documentation \
  --label test \
  --label security \
  --label tech-debt \
  --label cache \
  --title '[cache][security] NearJCache getAll front residency를 bounded policy로 제한한다' \
  --body-file .omx/issue-1369-pr-body.md
```

live read-back으로 base/head SHA, assignee, milestone, labels, body 마지막 H2와 #1408 release handoff를 확인한다.

- [ ] **Step 8: exact head CI와 review가 green일 때 merge-ready를 보고한다**

```bash
issue_1369_pr=$(gh pr view fix/1369-nearcache-bounded-bulk \
  --repo bluetape4k/bluetape4k-projects --json number --jq .number)
gh pr checks "$issue_1369_pr" --repo bluetape4k/bluetape4k-projects
gh pr view "$issue_1369_pr" --repo bluetape4k/bluetape4k-projects \
  --json headRefOid,baseRefName,headRefName,mergeStateStatus,reviews,statusCheckRollup
```

latest review thread와 exact head check를 다시 읽고 P0/P1=0이면 merge-ready DoD를 보고한다. 이 시점에는 merge하지 않고 fresh 승인을 기다린다. #1368 branch는 #1369 PR의 CI와 review blocker가 모두 해소된 뒤 이 exact head 위에서 만든다.

## spec-to-task traceability

| Spec/Issue 요구 | 구현 task | 증거 |
| --- | --- | --- |
| safe default가 무제한 residency를 허용하지 않음 | Task 2, 3 | `BypassFront` config/legacy default, default no-`putAll` test |
| bounded opt-in과 all-or-nothing | Task 2, 3 | `PopulateIfAtMost(n)` validation, `n`/`n+1` interaction test |
| 실제 back hit 수 기준 | Task 3 | 100-key/2-hit test, `backValues.size` predicate |
| 큰 value와 byte-size 비목표 | Task 3 | 1 MB value entry-count test, serializer interaction 없음 |
| 부분 hit/miss와 결과 정확성 | Task 3 | merged map과 logical/tier statistics test |
| exception·cancellation·epoch | Task 3 | back/front failure, cancellation, stale populate race repeat |
| 기존 API/serialization 호환성 | Task 2, 4 | no-arg/5/6/7 constructor/copy/component/reflection, precompiled fixture, legacy/current/null/malformed stream |
| builder safe default | Task 2 | default/explicit builder test |
| stable management metadata | Task 4 | snapshot/direct bean과 실제 MBeanServer token/limit read-back |
| front/back path와 contention 성능 | Task 1, 2A, 3, 6 | front control, 7-path/3-batch matrix, 1/2/4/8/16-thread same-harness comparison |
| caller migration과 반복 호출 | Task 2, 3, 5 | source-level copy, stateful repeated read, legacy/new default와 bounded opt-in 문서 |
| 운영자·호출자 문서 | Task 5 | actual provider factory 예제, core/provider README와 manual EN/KO marker/parity, capability matrix와 ops runbook/template assertion |
| entry-count 위협 경계 | Task 3, 5, 6 | large-value test, request/response/byte budget 비범위, PR body caveat |
| stacked PR boundary | Task 6 | live base=`develop`, head=`fix/1369-nearcache-bounded-bulk`; #1368 생성 hold |

## risk prediction과 rollback/rerun

| 위험 | signal | mitigation | rollback/rerun |
| --- | --- | --- | --- |
| data-class constructor/copy ABI drift | reflection 또는 Java consumer test 실패 | explicit 5/6 overload와 7-field primary shape 고정 | Task 2 commit 전으로 되돌리고 descriptor test부터 재실행 |
| legacy stream의 null/누락/invalid policy | deserialize failure, null access, validation 우회 | 누락/null은 `BypassFront`, non-positive는 `InvalidObjectException` | Task 2 serialization test와 full module test 재실행 |
| oversized batch 일부 populate | `put`/`putAll` interaction 발생 | one predicate, all-or-nothing 한 번의 `putAll` | Task 3 runtime commit revert 후 `n+1` RED부터 재개 |
| 통계가 residency policy에 종속 | logical/tier count 불일치 | record 계산과 반환 map은 기존 위치 유지 | operation statistics targeted test 재실행 |
| epoch race에서 stale populate | race test의 `putAll` 관찰 | epoch와 policy를 같은 `mutationGate`에서 검사 | race test `--rerun-tasks`, persistent failure면 PR 중단 |
| stable token drift | MXBean test에 subtype name 노출 | explicit string mapping | Task 4 mapping/test 재실행 |
| front/back-path regression | same-profile jq validator non-zero | policy branch를 back miss 이후에 두고 committed harness 고정 | PR 중단; baseline/candidate 전체 재수집은 plan 수정·재승인 후 수행 |
| default bypass의 back load 증가 | canary의 back hit/read load 또는 get latency가 service budget 초과 | JMX enable/register evidence와 token/limit/tier 통계를 같은 traffic window에서 비교 | 정상 경로는 bounded/Bypass wrapper handover; 이전 artifact는 time-bound break-glass와 heap/traffic 보상 통제 후 즉시 forward-fix |
| 문서가 요청 크기 제한으로 오해됨 | `keys.size` 표현 또는 locale mismatch | `backValues.size`, 반환/residency 분리, executable token test | Task 5 writer/parity gate 재실행 |
| #1368 authority가 PR 1에 섞임 | clear/provider diff 발견 | scope read-back으로 차단 | 해당 diff 제거 후 Task 2~6 affected proof 재실행 |

## 문서·release·repository hazard 판정

| Surface | 판정 | 근거 |
| --- | --- | --- |
| KDoc | Required | 새 public policy와 config/MXBean property를 한국어로 문서화 |
| README EN/KO | Required | default `getAll` residency behavior가 바뀜 |
| capability matrix | Required | blocking `NearJCache` policy와 metadata를 운영자가 확인해야 함 |
| diagram | N/A | 기존 2-tier topology와 call sequence는 바뀌지 않음 |
| manual EN/KO | Required | public config/factory의 기본 residency가 바뀌므로 core manual 쌍에 migration과 canary를 반영 |
| provider README EN/KO | Required | Lettuce/Hazelcast 동기 factory가 config safe default를 그대로 사용함 |
| CHANGELOG/release note | N/A before release, handoff required | PR body/lesson에 migration을 기록하고 #1408/1.13.0 final train DoD가 CHANGELOG/release note 반영을 확인 |
| AGENTS/workflow | N/A | contributor/runtime guidance 변경 없음 |
| module/BOM/catalog/CI/Nightly/Kover registration | N/A | 새 module/dependency/build task/coverage target 없음 |
| Testcontainers | N/A | `cache-core` behavior와 pass-through config만 변경하고 provider integration/signature는 변경하지 않음 |

## 구현 승인 및 중단 조건

- 이 plan이 사용자 승인을 받기 전에는 Task 1 benchmark와 Kotlin RED test를 시작하지 않는다.
- P0/P1 plan review finding은 plan을 수정하고 affected 관점을 다시 검토한 뒤 해소한다.
- 구현 중 public API shape, serialization 복원, all-or-nothing 정책을 지킬 수 없거나 provider factory 변경이 필요해지면 plan/spec 승인 단계로 돌아간다.
- Task 6의 검증·review·lesson이 수렴하기 전에는 push/PR을 만들지 않는다.
- merge-ready 보고 전에는 #1368 branch를 만들지 않고, #1369 merge는 fresh exact-head 승인 전에는 실행하지 않는다.

## Plan DoD

- [x] #1369만의 file/write scope와 #1368 exclusion을 고정했다.
- [x] spec의 모든 #1369 acceptance criterion을 ordered task와 command에 매핑했다.
- [x] policy/config, runtime, metadata, docs를 독립 commit 경계로 나눴다.
- [x] prior no-arg/5/6-인자 ABI와 legacy/current serialization proof를 포함했다.
- [x] success, edge, failure, cancellation, concurrency/epoch, statistics, performance case를 포함했다.
- [x] README 양언어, KDoc, capability matrix와 N/A surface 근거를 기록했다.
- [x] rollback/rerun과 stacked PR base/head/hold를 기록했다.
- [x] six-perspective plan review latest P0=0/P1=0
- [ ] 사용자 implementation plan 승인
