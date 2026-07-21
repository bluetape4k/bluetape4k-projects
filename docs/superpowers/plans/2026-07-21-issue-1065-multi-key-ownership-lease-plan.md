# Issue #1065 Multi-Key Ownership Lease Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 같은 Redis hash slot의 여러 key를 caller-supplied token과 TTL로 원자적으로 점유·검사·갱신·해제하는 stateless Lettuce primitive를 제공한다.

**Architecture:** standalone/cluster connection을 공통 Redis scripting interface로 축약하고, 입력 검증과 Lua vector decoder를 하나의 internal support에 둔다. sync/`CompletableFuture`/suspend facade는 같은 support를 사용하며 production resilience 정책은 외부 `SuspendDecorators`에 남긴다.

**Tech Stack:** Kotlin 2.3, Java 21, Lettuce 7.6, Redis Lua, Kotlin coroutines, JUnit 5, Kluent-style Bluetape assertions, Testcontainers Redis/Redis Cluster, Resilience4j test integration, Gradle Kotlin DSL.

---

## 1. 작업 경계와 파일 책임

| 파일 | 책임 |
|---|---|
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt` | 기존 binary signature를 유지하면서 standalone/cluster 공통 scripting-interface overload 제공 |
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseConfig.kt` | trusted instance policy인 `maxKeys`와 생성 시 재검증 |
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseResult.kt` | operation enum, stable exceptions, serializable sealed result/count API |
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseSupport.kt` | bounded snapshot, slot/TTL/token 검증, 네 Lua script, vector invariant/decoder |
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLease.kt` | standalone/cluster sync 및 `CompletableFuture` facade |
| `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendMultiKeyLease.kt` | standalone/cluster suspend facade와 cancellation propagation |
| `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt` | generalized runner의 sync/async/suspend 및 NOSCRIPT 회귀 |
| `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseContract.kt` | 모든 adapter가 공유하는 operation/result/validation/integrity scenario table |
| `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseDocumentationTest.kt` | README와 공유할 caller recovery/decorator example의 compile/execution proof |
| `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/*Test.kt` | validation/decoder, adapter parity, hostile Redis, cluster, cancellation, resilience, performance contract |
| `infra/lettuce/build.gradle.kts` | test-only resilience dependency와 opt-in performance test task |
| `infra/lettuce/README.md`, `infra/lettuce/README.ko.md` | source-equivalent public examples, recovery, telemetry, migration/runbook |
| `scripts/generate-infra-lettuce-diagram-01.mjs` | primitive-family diagram source 및 SVG/PNG 동시 생성 |
| `docs/images/readme-diagrams/infra-lettuce-diagram-01.{svg,png}` | generated visual assets |
| `docs/lessons/2026-07-21-multi-key-ownership-lease.md` | 구현/검증에서 얻은 durable lesson |

새 module, settings/BOM/catalog/Kover/Nightly 등록은 없다. production dependency도 추가하지 않는다.

### Conditional hazard decisions

- `multiKeyLeasePerformanceTest`는 production ranking용 benchmark harness가 아니라 실제
  Redis의 bounded regression characterization이다. 따라서 기존 `src/benchmark` JMH
  source set을 섞지 않고 test tag/custom `Test` task로 격리한다.
- 새 chart asset은 만들지 않으며 raw 결과는
  `infra/lettuce/build/reports/multi-key-lease-performance/results.json`에 기록한다.
- 새 module/artifact/Spring/Exposed/JDK preview/API migration이 아니므로 settings, BOM,
  catalog, Kover, Nightly, auto-configuration gate는 N/A다.
- 이 repository/module에 별도 CHANGELOG가 없고 public 변경 설명은 bilingual README,
  English KDoc, PR body, lesson이 담당한다.
- 실제 Redis/Testcontainers command는 모든 worktree가 공유하는
  `$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock`을
  `lockf -k -t 900`으로 획득한 뒤 `--no-parallel --max-workers=1`로 실행한다. lock timeout은
  임의 재시도하지 않고 blocker로 기록하며, 획득·해제/timeout 증거를 PR validation 표에 남긴다.

## 2. 위험 예측과 중단점

| 위험 | 조기 신호 | 완화/검증 | rollback 또는 rerun 지점 |
|---|---|---|---|
| Lua write 중 runtime error가 partial state를 남김 | Redis command failure 뒤 일부 key 존재 | 모든 분류/인자 검증을 첫 write 전에 완료하고 같은-token inspect/release hostile test 실행 | Task 4 script만 되돌리고 Task 3-4 재실행 |
| cluster API 타입 또는 routing 불일치 | cluster compile error, CROSSSLOT/MOVED | `RedisScripting*Commands` overload와 실제 `RedisClusterServer` fixture | Task 2 runner overload부터 rerun |
| result code/count drift | decoder invariant failure, adapter별 다른 result | operation별 table-driven decoder test를 먼저 고정 | Task 3 public model/Task 4 decoder를 함께 수정 |
| cancellation/flaky TTL test | 단독 PASS·suite FAIL, timeout | controllable pending `RedisFuture`, dispatch barrier, eventual PTTL polling | Task 6 cancellation fixture부터 rerun |
| retry가 validation/domain result까지 재시도 | attempt > 1, breaker failure 증가 | exact exception predicate, maxAttempts=2, negative metric assertions | Task 8 decorator config만 rollback |
| Redis Lua가 server queueing을 증가 | normalized p95 또는 probe p99 악화 | 1/8/32 × concurrency 1/16, 독립 PING connection | `maxKeys`/script 구조를 Task 4로 되돌려 재측정 |
| test dependency가 publication에 누출 | POM/runtime graph에 resilience4j 표시 | generated POM과 `runtimeClasspath` grep | Task 8 build file change rollback |
| migration dual writer | integrity exception, persistent same-token key | README cutover/rollback single-writer runbook | 새 writer 중지, drain/정리, 기존 writer 복귀 |

## 3. 구현 작업

### Task 1: RedisScriptRunner를 cluster-compatible scripting interface로 일반화

**Complexity:** M
**Dependency:** 없음
**Pattern skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/script/RedisScript.kt`
- Modify/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt`

- [ ] **Step 1: generalized sync/async/suspend overload가 없어서 compile fail하는 테스트를 추가한다.**

```kotlin
private lateinit var scripting: RedisScriptingCommands<String, String>
private lateinit var asyncScripting: RedisScriptingAsyncCommands<String, String>

@BeforeEach
fun setup() {
    syncCommands = connection.sync()
    asyncCommands = connection.async()
    scripting = syncCommands
    asyncScripting = asyncCommands
}

@Test
fun `general scripting interfaces support all runner styles`() = runTest {
    val key = randomName()
    RedisScriptRunner.run<String>(scripting, setAndReturnScript, ScriptOutputType.VALUE, arrayOf(key), "sync") shouldBeEqualTo "sync"
    RedisScriptRunner.runAsync<String>(asyncScripting, setAndReturnScript, ScriptOutputType.VALUE, arrayOf(key), "async").get() shouldBeEqualTo "async"
    RedisScriptRunner.runSuspending<String>(asyncScripting, setAndReturnScript, ScriptOutputType.VALUE, arrayOf(key), "suspend") shouldBeEqualTo "suspend"
}
```

- [ ] **Step 2: targeted test를 실행해 generalized overload compile failure를 확인한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.script.RedisScriptTest" --no-parallel --max-workers=1`
Expected: FAIL at `compileTestKotlin` because runner does not accept `RedisScriptingCommands`/`RedisScriptingAsyncCommands`.

- [ ] **Step 3: 기존 public overload는 유지하고 generalized overload와 private implementation을 추가한다.**

```kotlin
fun <T> run(
    commands: RedisScriptingCommands<String, String>,
    script: RedisScript,
    outputType: ScriptOutputType,
    keys: Array<String>,
    vararg args: String,
): T = runScripting(commands, script, outputType, keys, *args)

private fun <T> runScripting(
    commands: RedisScriptingCommands<String, String>,
    script: RedisScript,
    outputType: ScriptOutputType,
    keys: Array<String>,
    vararg args: String,
): T = try {
    commands.evalsha(script.sha1, outputType, keys, *args)
} catch (_: RedisNoScriptException) {
    commands.eval(script.source, outputType, keys, *args)
}

fun <T> run(
    commands: RedisCommands<String, String>,
    script: RedisScript,
    outputType: ScriptOutputType,
    keys: Array<String>,
    vararg args: String,
): T = runScripting(commands, script, outputType, keys, *args)
```

Async는 `RedisScriptingAsyncCommands`로 `evalsha(...).toCompletableFuture().exceptionallyCompose`를 수행하고, suspend는 같은 interface의 `RedisFuture.awaitSuspending()`를 사용한다. 기존 `RedisAsyncCommands` overload는 generalized private function에 위임한다.

- [ ] **Step 4: 기존 signature와 새 interface fixture를 함께 통과시킨다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.script.RedisScriptTest" --no-parallel --max-workers=1`
Expected: PASS; 기존 NOSCRIPT sync/async/suspend 테스트도 유지된다.

- [ ] **Step 5: Lore commit을 만든다.**

```text
Allow Redis scripts to run through cluster-compatible interfaces

Constraint: Existing RedisCommands signatures are public and must retain binary compatibility.
Confidence: high
Scope-risk: narrow
Tested: :bluetape4k-lettuce:test --tests RedisScriptTest
```

### Task 2: Public result/config/exception 계약을 TDD로 고정

**Complexity:** M
**Dependency:** Task 1
**Pattern skill:** `bluetape-kotlin-patterns`, `ecc-kotlin-testing`

**Files:**
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseConfig.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseResult.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseResultTest.kt`

- [ ] **Step 1: config validation, exhaustive result properties, serialization round-trip, exception redaction 테스트를 작성한다.**

```kotlin
@Test
fun `config rejects non-positive maxKeys`() {
    assertFailsWith<IllegalArgumentException> { LettuceMultiKeyLeaseConfig(0) }
}

@Test
fun `result counts are serializable pre-mutation observations`() {
    val counts = MultiKeyLeaseCounts(3, 1, 1, 1)
    val result: MultiKeyReleaseResult = MultiKeyReleaseResult.OwnershipMismatch(counts)
    val bytes = ByteArrayOutputStream().also { buffer ->
        ObjectOutputStream(buffer).use { it.writeObject(result) }
    }.toByteArray()
    val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
    restored shouldBeEqualTo result
}

@Test
fun `stable exceptions never expose keys or tokens`() {
    val secretKey = "ticket:{sale}:user:secret-user"
    val secretToken = "owner-secret"
    val failures = listOf(
        MultiKeyLeaseCrossSlotException(distinctSlotCount = 2),
        MultiKeyLeaseIntegrityException(
            operation = MultiKeyLeaseOperation.INSPECT,
            requestedKeyCount = 2,
            invalidLeaseKeyCount = 1,
        ),
    )
    failures.forEach { assertThrowableRedacted(it, secretKey, secretToken) }
    listOf(
        MultiKeyAcquireResult::class,
        MultiKeyInspectResult::class,
        MultiKeyRenewResult::class,
        MultiKeyReleaseResult::class,
    ).flatMap { it.sealedSubclasses }.flatMap { it.memberProperties }
        .none { it.name.contains("key", true) || it.name.contains("token", true) }.shouldBeTrue()
}
```

`assertThrowableRedacted`는 `message`, `toString()`, public property values와 전체 cause chain을
검사한다. 실제 Redis persistent same-token fixture에서 발생한
`MultiKeyLeaseIntegrityException`에도 같은 helper를 적용한다.
Task 2는 아직 구현되지 않은 Task 3 validator를 참조하지 않는다. validator가 실제
cross-slot exception을 발생시키는 경로는 Task 3의 pre-dispatch validation test가 담당한다.

- [ ] **Step 2: test compile failure를 확인한다.**

Run: `./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.MultiKeyLeaseResultTest"`
Expected: FAIL because lease public types do not exist.

- [ ] **Step 3: spec와 동일한 public declarations를 구현한다.**

```kotlin
data class LettuceMultiKeyLeaseConfig(val maxKeys: Int = 32): Serializable {
    init { require(maxKeys > 0) { "maxKeys must be positive." } }
    private companion object { private const val serialVersionUID: Long = 1L }
}

enum class MultiKeyLeaseOperation { ACQUIRE, INSPECT, RENEW, RELEASE }

data class MultiKeyLeaseCounts(
    val requestedKeys: Int,
    val ownedKeys: Int,
    val missingKeys: Int,
    val mismatchedKeys: Int,
): Serializable

sealed interface MultiKeyAcquireResult: Serializable {
    data object Acquired: MultiKeyAcquireResult
    data class AlreadyOwned(val minimumPttlMillis: Long): MultiKeyAcquireResult
    data class PartialOwnership(val counts: MultiKeyLeaseCounts): MultiKeyAcquireResult
    data class Conflicted(val counts: MultiKeyLeaseCounts): MultiKeyAcquireResult
}
```

같은 파일에 spec의 `MultiKeyInspectResult`, `MultiKeyRenewResult`, `MultiKeyReleaseResult`, `MultiKeyLeaseCrossSlotException`, `MultiKeyLeaseIntegrityException`을 정확히 선언한다. public class/data class에는 English KDoc과 serial UID를 둔다.
`serialVersionUID`는 public ABI field가 되지 않도록 config/count/result data class의 private
companion에 `private const val serialVersionUID = 1L`로 선언한다. 각 `data object`는 object body에
직접 `private const val serialVersionUID: Long = 1L`을 둔다. config, counts, 모든 concrete result
data class/data object를 table-driven `ObjectOutputStream` round-trip하고
`ObjectStreamClass.lookup(type.java).serialVersionUID == 1L`도 검증한다.

- [ ] **Step 4: public model test를 통과시킨다.**

Run: `./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.MultiKeyLeaseResultTest"`
Expected: PASS.

- [ ] **Step 5: Lore commit을 만든다.**

```text
Make multi-key lease outcomes exhaustive for callers

Constraint: Results must not expose keys or owner tokens.
Confidence: high
Scope-risk: narrow
Tested: :bluetape4k-lettuce:test --tests MultiKeyLeaseResultTest
```

### Task 3: 입력 검증과 vector decoder를 Redis 없이 고정

**Complexity:** L
**Dependency:** Task 2
**Pattern skill:** `bluetape-kotlin-patterns`, `ecc-kotlin-testing`

**Files:**
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseSupport.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseSupportTest.kt`

- [ ] **Step 1: bounded snapshot/slot/token/TTL validation RED tests를 작성한다.**

```kotlin
@Test
fun `validation preserves order and enforces one slot`() {
    val input = listOf("ticket:{sale}:ip:a", "ticket:{sale}:user:b")
    validateLeaseInput(input, "owner", Duration.ofMillis(10), LettuceMultiKeyLeaseConfig(), StringCodec.UTF8)
        .keys shouldBeEqualTo input
}

@Test
fun `validation rejects cross slot before dispatch`() {
    assertFailsWith<MultiKeyLeaseCrossSlotException> {
        validateLeaseInput(listOf("a:{one}", "b:{two}"), "owner", Duration.ofSeconds(1), LettuceMultiKeyLeaseConfig(), StringCodec.UTF8)
    }.distinctSlotCount shouldBeEqualTo 2
}

@Test
fun `bounded iterator rejects one element beyond max without overflow`() {
    val keys = object: AbstractCollection<String>() {
        override val size: Int = Int.MAX_VALUE
        override fun iterator(): Iterator<String> = listOf("a:{x}", "b:{x}", "c:{x}").iterator()
    }
    assertFailsWith<IllegalArgumentException> {
        validateLeaseInput(keys, "owner", null, LettuceMultiKeyLeaseConfig(2), StringCodec.UTF8)
    }
}
```

negative cases는 empty, blank, duplicate, blank token, zero/negative/sub-ms TTL, `Duration.toMillis()` overflow, `maxKeys=Int.MAX_VALUE`의 작은 collection을 포함한다.

- [ ] **Step 2: decoder status/count/TTL invariant RED table을 작성한다.**

```kotlin
@Test
fun `renew decoder gives mismatch precedence`() {
    decodeRenew(listOf(43L, 3L, 0L, 1L, 2L, 0L, -1L)) shouldBeEqualTo
        MultiKeyRenewResult.OwnershipMismatch(MultiKeyLeaseCounts(3, 0, 1, 2))
}

@Test
fun `decoder rejects inconsistent vectors`() {
    listOf(
        emptyList(),
        listOf(40L, 3L, 1L, 1L, 0L, 0L, -1L),
        listOf(999L, 1L, 1L, 0L, 0L, 0L, -1L),
    ).forEach { vector -> assertFailsWith<IllegalStateException> { decodeRenew(vector) } }
}
```

- [ ] **Step 3: targeted test의 expected compile failure를 확인한다.**

Run: `./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseSupportTest"`
Expected: FAIL because validation/decoder support does not exist.

- [ ] **Step 4: validation snapshot과 공통 vector invariant를 구현한다.**

```kotlin
internal fun snapshotKeys(keys: Collection<String>, maxKeys: Int): List<String> {
    val snapshot = ArrayList<String>(minOf(maxKeys, 32))
    val iterator = keys.iterator()
    require(iterator.hasNext()) { "keys must not be empty." }
    while (iterator.hasNext()) {
        require(snapshot.size < maxKeys) { "keys must contain at most $maxKeys entries." }
        snapshot += iterator.next().also { require(it.isNotBlank()) { "keys must not contain blank values." } }
    }
    require(snapshot.toSet().size == snapshot.size) { "keys must not contain duplicates." }
    return snapshot
}

internal fun requireSameSlot(keys: List<String>, codec: RedisCodec<String, String>) {
    val slots = keys.asSequence().map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet()
    if (slots.size != 1) throw MultiKeyLeaseCrossSlotException(slots.size)
}
```

decoder는 vector length 7, non-negative counts, `requested == owned + missing + mismatched`, `invalidTtl <= owned`, TTL-bearing status만 positive PTTL을 허용하는 순서로 검사한 뒤 operation별 sealed result를 반환한다.
UTF-8과 의도적으로 다른 key encoding을 쓰는 custom codec test는 raw encoded bytes 기준 expected
slot과 validator 결과가 일치하는지 검증해 client routing과 pre-dispatch validation drift를 막는다.

- [ ] **Step 5: support test를 통과시킨다.**

Run: `./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseSupportTest"`
Expected: PASS.

- [ ] **Step 6: Lore commit을 만든다.**

```text
Reject unsafe lease inputs before Redis dispatch

Constraint: Cross-slot and malformed vectors must fail without exposing identifiers.
Confidence: high
Scope-risk: narrow
Tested: :bluetape4k-lettuce:test --tests LettuceMultiKeyLeaseSupportTest
```

### Task 4: Lua state machine을 실제 Redis에서 TDD

**Complexity:** XL
**Dependency:** Task 3
**Pattern skill:** `bluetape-kotlin-patterns`, `ecc-kotlin-testing`

**Files:**
- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseSupport.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseScriptTest.kt`

- [ ] **Step 1: acquire/inspect RED matrix를 실제 Redis fixture에 작성한다.**

```kotlin
@Test
fun `acquire is all or nothing and replay does not extend ttl`() {
    executeAcquire(keys, token, 10_000) shouldBeEqualTo MultiKeyAcquireResult.Acquired
    val before = executeInspect(keys, token).shouldBeInstanceOf<MultiKeyInspectResult.Owned>().minimumPttlMillis
    executeAcquire(keys, token, 20_000).shouldBeInstanceOf<MultiKeyAcquireResult.AlreadyOwned>()
    val after = executeInspect(keys, token).shouldBeInstanceOf<MultiKeyInspectResult.Owned>().minimumPttlMillis
    after.shouldBeLessOrEqualTo(before)
    after.shouldBeGreaterOrEqualTo(before - 1_000)
}

@Test
fun `conflict never creates missing key`() {
    commands.psetex(keys.first(), 5_000, "other")
    executeAcquire(keys, token, 5_000).shouldBeInstanceOf<MultiKeyAcquireResult.Conflicted>()
    commands.exists(keys.last()) shouldBeEqualTo 0L
}
```

partial ownership, all missing lost, mixed conflict, persistent same-token integrity를 추가한다.

test-only Lua는 첫 key를 같은 token/TTL로 `SET`한 직후 `error('injected')`를 발생시킨다.
command failure 뒤 public `inspect`가 `PartialOwnership`을 반환하고 public `release`가 own key만
정리해 `PartialRelease`를 반환하며 최종 `EXISTS` 합계가 0인지 확인한다. exception/result/message/cause에
secret key/token이 없는지도 검증한다. 이 fixture는
Redis Lua runtime error가 rollback을 제공하지 않는다는 recovery 계약을 결정적으로 고정한다.

- [ ] **Step 2: renew/release RED matrix를 작성한다.**

```kotlin
@Test
fun `renew and release mutate only matching owner keys`() {
    commands.psetex(keys[0], 5_000, token)
    commands.psetex(keys[1], 5_000, "other")
    executeRenew(keys, token, 10_000).shouldBeInstanceOf<MultiKeyRenewResult.OwnershipMismatch>()
    commands.get(keys[1]) shouldBeEqualTo "other"
    executeRelease(keys, token).shouldBeInstanceOf<MultiKeyReleaseResult.OwnershipMismatch>()
    commands.get(keys[1]) shouldBeEqualTo "other"
}
```

full, partial missing, all missing, mismatch-with-zero-owned, external persistent key cleanup을 포함한다.

- [ ] **Step 3: script test가 missing script executor로 실패하는지 확인한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseScriptTest" --no-parallel --max-workers=1`
Expected: FAIL because operation scripts/executors are absent.

- [ ] **Step 4: acquire/inspect Lua를 구현한다.**

각 script는 모든 `GET`/`PTTL` 분류를 첫 write 전에 끝내고 다음 vector를 반환한다.

```lua
-- [status, requested, owned, missing, mismatched, invalidTtl, minimumPttl]
local owned, missing, mismatched, invalidTtl, minimumPttl = 0, 0, 0, 0, -1
for _, key in ipairs(KEYS) do
  local value = redis.call('GET', key)
  if not value then
    missing = missing + 1
  elseif value == ARGV[1] then
    owned = owned + 1
    local pttl = redis.call('PTTL', key)
    if pttl < 0 then invalidTtl = invalidTtl + 1
    elseif minimumPttl < 0 or pttl < minimumPttl then minimumPttl = pttl end
  else
    mismatched = mismatched + 1
  end
end
```

acquire는 integrity `90`, conflict `13`, replay `11`, partial `12`, all-missing write success `10`; inspect는 integrity `90`, conflict `23`, owned `20`, partial `22`, lost `21`을 반환한다. non-TTL result field는 `-1`이다.

- [ ] **Step 5: renew/release Lua를 구현한다.**

renew는 pre-classification 뒤 integrity면 no-op, 아니면 matching key만 `PEXPIRE`; status는 renewed `40`, partial `41`, lost `42`, mismatch `43`이다. release는 matching token만 `DEL`하고 persistent same-token도 정리하며 released `50`, partial `51`, lost `52`, mismatch `53`을 반환한다.

- [ ] **Step 6: script matrix를 통과시킨다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseScriptTest" --no-parallel --max-workers=1`
Expected: PASS; Redis key inspection에서 wrong-owner mutation과 conflict partial write가 없다.

- [ ] **Step 7: Lore commit을 만든다.**

```text
Keep multi-key lease transitions atomic within one Redis script

Constraint: Runtime failures remain ambiguous because Redis Lua does not roll back prior writes.
Confidence: high
Scope-risk: moderate
Tested: :bluetape4k-lettuce:test --tests LettuceMultiKeyLeaseScriptTest
```

### Task 5: Sync/CompletableFuture facade와 adapter parity

**Complexity:** L
**Dependency:** Task 4
**Pattern skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLease.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseContract.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseTest.kt`

- [ ] **Step 1: sync/async가 같은 scenario table을 반환하는 RED contract를 작성한다.**

```kotlin
private val adapters = listOf<LeaseAdapter>(syncAdapter(), asyncAdapter())

@TestFactory
fun `sync and async adapters share the lease contract`() = adapters.map { adapter ->
    dynamicTest(adapter.name) {
        adapter.acquire(keys, token, Duration.ofSeconds(5)) shouldBeEqualTo MultiKeyAcquireResult.Acquired
        adapter.inspect(keys, token).shouldBeInstanceOf<MultiKeyInspectResult.Owned>()
        adapter.renew(keys, token, Duration.ofSeconds(10)) shouldBeEqualTo MultiKeyRenewResult.Renewed
        adapter.release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released
    }
}
```

공통 scenario table은 다음을 개별 case로 고정한다.

- acquire: `Acquired`, TTL을 연장하지 않는 `AlreadyOwned`, `PartialOwnership`, `Conflicted`
- inspect: `Owned`, `Lost`, `PartialOwnership`, `Conflicted`
- renew: `Renewed`, `PartialLoss`, `Lost`, `OwnershipMismatch`
- release: `Released`, `PartialRelease`, `Lost`, `OwnershipMismatch`
- dispatch 전 거절: empty/blank/duplicate key, blank token, invalid TTL, invalid config
- integrity/recovery: acquire/inspect/renew의 persistent same-token exception과 release cleanup
- routing: same-slot success와 cross-slot pre-dispatch failure

Task 5에서는 sync/async adapter가 이 전체 table을 실행하고, Task 6에서는 suspend adapter를
동일 table에 추가한다. Task 7 실제 cluster는 backend capability subset 전체를 세 adapter로 실행한다.

- [ ] **Step 2: facade absence failure를 확인한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseTest" --no-parallel --max-workers=1`
Expected: FAIL because `LettuceMultiKeyLease` does not exist.

- [ ] **Step 3: standalone/cluster secondary constructors와 여덟 public methods를 구현한다.**

```kotlin
class LettuceMultiKeyLease private constructor(
    private val syncCommands: RedisScriptingCommands<String, String>,
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    private val codec: RedisCodec<String, String>,
    private val config: LettuceMultiKeyLeaseConfig,
) {
    constructor(connection: StatefulRedisConnection<String, String>, config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig()):
        this(connection.sync(), connection.async(), connection.codec, config)
    constructor(connection: StatefulRedisClusterConnection<String, String>, config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig()):
        this(connection.sync(), connection.async(), connection.codec, config)

    init { require(config.maxKeys > 0) { "maxKeys must be positive." } }

    fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration): MultiKeyAcquireResult =
        runAcquire(syncCommands, validateAcquire(keys, ownerToken, leaseTime, config, codec))

    fun acquireAsync(keys: Collection<String>, ownerToken: String, leaseTime: Duration): CompletableFuture<MultiKeyAcquireResult> =
        runAcquireAsync(asyncCommands, validateAcquire(keys, ownerToken, leaseTime, config, codec))
}
```

inspect/renew/release sync/async도 보유한 `codec`을 각 validation 함수에 전달하고 같은 snapshot과
decoder를 사용한다. English KDoc에 advisory boundary, token reuse, cancellation ambiguity,
shared hash tag 예제를 포함한다.
primary constructor는 승인된 API대로 `private`를 유지한다. test는 mocked
`StatefulRedisConnection<String, String>`/cluster connection의 `sync()`, `async()`, `codec`이
test commands/custom codec을 반환하도록 구성해 public constructor만 통과한다. 정상 config를
reflection test helper로 `maxKeys=0`으로 손상시켜 public constructor에 전달하고 생성 즉시
`IllegalArgumentException`과 Redis command interaction 0회를 검증한다.

- [ ] **Step 4: adapter contract와 validation-before-dispatch를 통과시킨다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseTest" --no-parallel --max-workers=1`
Expected: PASS.

- [ ] **Step 5: Lore commit을 만든다.**

```text
Expose stateless sync and future multi-key leases

Constraint: Every call carries the full key set, owner token, and applicable TTL.
Confidence: high
Scope-risk: moderate
Tested: :bluetape4k-lettuce:test --tests LettuceMultiKeyLeaseTest
```

### Task 6: Suspend facade와 cancellation contract

**Complexity:** L
**Dependency:** Task 5
**Pattern skill:** `kotlin-coroutines-skill`, `ecc-kotlin-testing`

**Files:**
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendMultiKeyLease.kt`
- Modify/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseContract.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendMultiKeyLeaseTest.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseCancellationTest.kt`

- [ ] **Step 1: suspend adapter를 기존 전체 scenario table에 추가하는 RED contract를 작성한다.**

```kotlin
@Test
fun `suspend adapter preserves the same result contract`() = runTest {
    val lease = LettuceSuspendMultiKeyLease(connection)
    lease.acquire(keys, token, Duration.ofSeconds(5)) shouldBeEqualTo MultiKeyAcquireResult.Acquired
    lease.inspect(keys, token).shouldBeInstanceOf<MultiKeyInspectResult.Owned>()
    lease.renew(keys, token, Duration.ofSeconds(10)) shouldBeEqualTo MultiKeyRenewResult.Renewed
    lease.release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released
}
```

- [ ] **Step 2: controllable pending RedisFuture cancellation RED test를 작성한다.**

```kotlin
@Test
fun `suspend cancellation cancels pending RedisFuture`() = runTest {
    val pending = TestRedisFuture<List<Any>>()
    every { asyncCommands.evalsha<List<Any>>(any(), any(), any(), *anyVararg()) } returns pending
    every { connection.async() } returns asyncCommands
    every { connection.codec } returns StringCodec.UTF8
    val lease = LettuceSuspendMultiKeyLease(connection, config)
    val job = launch { lease.acquire(keys, token, Duration.ofSeconds(5)) }
    runCurrent()
    job.cancelAndJoin()
    pending.isCancelled.shouldBeTrue()
}

private class TestRedisFuture<T>: CompletableFuture<T>(), RedisFuture<T> {
    override fun getError(): String? = if (isCompletedExceptionally) "completed exceptionally" else null
    override fun await(timeout: Long, unit: TimeUnit): Boolean =
        try { get(timeout, unit); true } catch (_: TimeoutException) { false }
}
```

정상 config를 reflection helper로 손상시킨 suspend facade도 constructor 호출 시
`IllegalArgumentException`으로 거부되고 Redis interaction이 0회인지 검증한다. Future facade cancellation은
별도 실제 Redis test에서 dispatch 관찰 후 반환 `CompletableFuture.cancel(true)`를 호출하고,
upstream/server가 settle할 때까지 bounded polling한 뒤 같은-token `inspect`로 상태를 확인한다.
caller wait 취소가 server non-execution을 보장하지 않으며 허용 상태는 full-owned/full-missing뿐이고
partial은 금지됨을 test 이름과 assertion으로 고정한다.

- [ ] **Step 3: missing suspend facade failure를 확인한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceSuspendMultiKeyLeaseTest" --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseCancellationTest" --no-parallel --max-workers=1`
Expected: FAIL because suspend facade/test seam is absent.

- [ ] **Step 4: `RedisScriptRunner.runSuspending` 기반 네 suspend method를 구현한다.**

```kotlin
class LettuceSuspendMultiKeyLease private constructor(
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    private val codec: RedisCodec<String, String>,
    private val config: LettuceMultiKeyLeaseConfig,
) {
    constructor(connection: StatefulRedisConnection<String, String>, config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig()): this(connection.async(), connection.codec, config)
    constructor(connection: StatefulRedisClusterConnection<String, String>, config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig()): this(connection.async(), connection.codec, config)

    init { require(config.maxKeys > 0) { "maxKeys must be positive." } }

    suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration): MultiKeyAcquireResult =
        runAcquireSuspending(asyncCommands, validateAcquire(keys, ownerToken, leaseTime, config, codec))
}
```

inspect/renew/release도 보유한 `codec`을 validation에 전달해 동일하게 구현하며
`CancellationException`을 잡거나 변환하지 않는다.
실제 Redis cancellation RED test는 전용 `ClientResources.eventBus()` subscription과 latch로
`EVALSHA` 또는 fallback `EVAL`의 `CommandStartedEvent` dispatch를 관찰한 뒤 job/future를 취소한다.
같은-token `inspect`는 full-owned/full-missing만 허용하고 `PartialOwnership`은 실패시킨다.
subscription, 전용 connection/client는 `try/finally` 또는 `use`로 항상 닫는다.

- [ ] **Step 5: cancellation과 suspend parity를 통과시킨다.**

Run: 위 targeted command
Expected: PASS; pending future는 cancelled, 실제 Redis fixture는 full-acquired 또는 full-missing만 허용하고 partial은 0건이다.

- [ ] **Step 6: Lore commit을 만든다.**

```text
Preserve cancellation across suspend lease operations

Constraint: Dispatch-time cancellation cannot prove that Redis skipped the script.
Confidence: high
Scope-risk: moderate
Tested: targeted suspend and cancellation tests
```

### Task 7: Redis Cluster·NOSCRIPT·동시성·TTL hostile fixtures

**Complexity:** XL
**Dependency:** Task 6
**Pattern skill:** `ecc-kotlin-testing`, `kotlin-coroutines-skill`

**Files:**
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseClusterTest.kt`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseConcurrencyTest.kt`
- Modify/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/script/RedisScriptTest.kt`

- [ ] **Step 1: actual cluster same-slot/cross-slot RED test를 작성한다.**

```kotlin
@Test
fun `cluster routes same-slot scripts and rejects cross-slot before Redis`() {
    RedisClusterServer().use { server ->
        server.start()
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val lease = LettuceMultiKeyLease(connection)
                lease.acquire(listOf("lease:{sale}:a", "lease:{sale}:b"), token, Duration.ofSeconds(5)) shouldBeEqualTo MultiKeyAcquireResult.Acquired
                assertFailsWith<MultiKeyLeaseCrossSlotException> {
                    lease.acquire(listOf("lease:{a}:x", "lease:{b}:y"), token, Duration.ofSeconds(5))
                }
            }
        }
    }
}
```

- [ ] **Step 2: contention/wrong-owner/expiry RED tests를 작성한다.**

두 caller가 overlapping key set을 동시에 acquire하면 winner 1명, loser-created key 0개임을 `MultithreadingTester`와 barrier로 검증한다. stale token renew/release, forced missing key, new owner overwrite를 포함한다. TTL expiry는 lease TTL 200ms로 만들고 `await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(20)).untilAsserted { inspect(...) shouldBeEqualTo MultiKeyInspectResult.Lost }`로 기다린 뒤 모든 key의 `EXISTS`가 0인지 확인한다. exact sleep은 금지한다. cluster fixture에서도 sync/async/suspend 세 adapter가 공통 contract의 same-slot acquire/inspect/renew/release, cross-slot validation, replay/TTL, integrity/recovery subset을 모두 통과해야 한다.

- [ ] **Step 3: isolated NOSCRIPT fixture를 작성한다.**

공유 fixture/`@ResourceLock`에 의존하지 않고 전용 `RedisServer` container와 전용 client/connection을
각 test가 `use`/`finally`로 소유한다. sync/async/suspend 각각 `SCRIPT FLUSH` 후 성공을 검증하고
역순으로 connection/client/container를 닫는다. 별도 MockK scripting-interface unit fixture는 첫
`evalsha`에 `RedisNoScriptException`을 발생시키고 fallback `eval`을 성공시켜 각 style별
`evalsha=1`, `eval=1`, 추가 dispatch=0을 `verify(exactly = 1)`/`confirmVerified`로 고정한다.
exact sleep 대신 Awaitility-style bounded polling helper를 사용한다.

- [ ] **Step 4: hostile suite를 실행하고 root cause가 없는 retry를 금지한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseClusterTest" --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseConcurrencyTest" --tests "io.bluetape4k.redis.lettuce.script.RedisScriptTest" --no-parallel --max-workers=1`
Expected: PASS once sequentially; sync/async/suspend NOSCRIPT는 각각 `evalsha=1`, `eval=1`; timing failure는 원인을 수정한 뒤 전체 command를 처음부터 재실행한다.

- [ ] **Step 5: Lore commit을 만든다.**

```text
Prove multi-key leases against cluster and hostile ownership changes

Constraint: Testcontainers-backed Redis suites run sequentially.
Confidence: high
Scope-risk: moderate
Tested: cluster, concurrency, TTL, NOSCRIPT targeted suites
```

### Task 8: Retry + CircuitBreaker + Bulkhead test-only integration

**Complexity:** L
**Dependency:** Task 7
**Pattern skill:** `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`

**Files:**
- Modify: `infra/lettuce/build.gradle.kts`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeaseResilience4jTest.kt`

- [ ] **Step 1: test-only project dependency를 추가한다.**

```kotlin
testImplementation(project(":bluetape4k-resilience4j"))
```

- [ ] **Step 2: ambiguous response RED integration test를 작성한다.**

```kotlin
val retry = Retry.of(
    "multi-key-lease",
    RetryConfig.custom<MultiKeyAcquireResult>()
        .maxAttempts(2)
        .waitDuration(Duration.ZERO)
        .retryOnException { it is IOException || it is RedisConnectionException || it is RedisCommandTimeoutException }
        .build(),
)
val circuitBreaker = CircuitBreaker.of(
    "multi-key-lease",
    CircuitBreakerConfig.custom()
        .slidingWindowSize(2)
        .minimumNumberOfCalls(2)
        .failureRateThreshold(50.0F)
        .build(),
)
val bulkhead = Bulkhead.of(
    "multi-key-lease",
    BulkheadConfig.custom()
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ZERO)
        .build(),
)
var attempts = 0
val decorated = SuspendDecorators.ofSupplier {
    attempts++
    val result = lease.acquire(keys, token, Duration.ofSeconds(10))
    if (attempts == 1) throw IOException("response lost after Redis success")
    result
}.withRetry(retry).withCircuitBreaker(circuitBreaker).withBulkhead(bulkhead).decorate()

decorated().shouldBeInstanceOf<MultiKeyAcquireResult.AlreadyOwned>()
attempts shouldBeEqualTo 2
```

`Duration.ZERO` retry/bulkhead wait는 deterministic test 전용이다. production README example은
bounded non-zero retry backoff와 application capacity에 맞춘 bulkhead wait/concurrency를 사용한다.

Retry success-with-retry 1, CircuitBreaker logical success 1, Bulkhead available permits 복구를 event/metric assertion으로 확인한다.

- [ ] **Step 3: domain result와 non-retry exception RED cases를 추가한다.**

`Conflicted`, `PartialOwnership`, validation, integrity, cancellation은 attempt 1이며 breaker failure/permit leak이 없어야 한다.

- [ ] **Step 4: targeted test를 통과시킨다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.LettuceMultiKeyLeaseResilience4jTest" --no-parallel --max-workers=1`
Expected: PASS.

- [ ] **Step 5: production dependency 비노출을 증명한다.**

Run: `./gradlew :bluetape4k-lettuce:dependencies --configuration runtimeClasspath`
Expected: output에 `bluetape4k-resilience4j` 없음.

Run: `./gradlew :bluetape4k-lettuce:generatePomFileForBluetape4kPublication`
Expected: `infra/lettuce/build/publications/Bluetape4k/pom-default.xml`에 resilience4j project dependency 없음.

- [ ] **Step 6: Lore commit을 만든다.**

```text
Demonstrate bounded resilience around ambiguous lease responses

Constraint: Resilience4j remains test-only and caller-configured.
Confidence: high
Scope-risk: narrow
Tested: resilience integration, runtimeClasspath, generated publication POM
```

### Task 9: 성능 characterization task와 evidence

**Complexity:** L
**Dependency:** Task 8
**Pattern skill:** `bluetape-kotlin-patterns`, `ecc-kotlin-testing`

**Files:**
- Modify: `infra/lettuce/build.gradle.kts`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceMultiKeyLeasePerformanceTest.kt`

- [ ] **Step 1: 기본 test와 분리된 tagged task를 추가한다.**

```kotlin
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("performance") }
}

tasks.register<Test>("multiKeyLeasePerformanceTest") {
    description = "Runs multi-key lease Redis characterization tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("performance") }
    shouldRunAfter(tasks.test)
}
```

- [ ] **Step 2: `@Tag("performance")` fixture를 작성한다.**

key count `1/8/32`, concurrency `1/16`에서 acquire/release p50/p95, throughput,
timeout/error count를 수집한다. 각 조합은 20 warm-up round와 100 measured round를
사용한다. concurrency 1은 한 caller의 기준 cycle, concurrency 16은 16 caller가 같은
hash-tag와 동일 key 집합을 barrier로 동시에 경쟁하는 high-contention cycle이다. 각 round는
`Acquired` 한 건과 나머지 `Conflicted`를 result type별/overall로 집계하고 winner만 release한
뒤 다음 round로 넘어간다. workload와 별도 `StatefulRedisConnection`은 10ms 고정 주기로
PING p95/p99를 측정한다.

```kotlin
normalizedP95(keys = 32, concurrency).shouldBeLessOrEqualTo(normalizedP95(keys = 8, concurrency) * 4.0)
errors.get() shouldBeEqualTo 0
probeP99.shouldBeLessThan(connectionTimeout.toMillis())
```

fixture는 Redis image/version, Java/Kotlin/Lettuce version, CPU count, sample/warm-up 수,
metric direction(`lower latency is better`, `higher throughput is better`), 여섯 조합의 raw
수치를 `infra/lettuce/build/reports/multi-key-lease-performance/results.json`에 기록한다.
normalized p95 기준은 concurrency 1과 16 각각 독립적으로 적용한다.

fixture 하나가 container, client, workload/probe connections, executor를 명시적으로 소유하고
`try/finally`/`use`에서 역순으로 닫는다. 각 parameter 조합 뒤 모든 worker future 종료를 기다리고,
test key를 삭제해 `EXISTS == 0`과 executor active task 0을 확인한 다음 다음 조합으로 이동한다.
assertion 또는 worker가 실패해도 teardown guard가 동일 cleanup을 수행한다.

- [ ] **Step 3: characterization을 단독 실행한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:multiKeyLeasePerformanceTest --no-parallel --max-workers=1`
Expected: PASS, report에 6개 조합의 p50/p95/throughput 및 probe p95/p99가 출력된다.

- [ ] **Step 4: maxKeys/script 변경 시 재실행 지침을 test KDoc과 README 작업 목록에 연결한다.**

- [ ] **Step 5: Lore commit을 만든다.**

```text
Bound the Redis cost of multi-key lease scripts

Constraint: Absolute latency is environment-dependent, so regression uses normalized in-run ratios.
Confidence: medium
Scope-risk: narrow
Tested: :bluetape4k-lettuce:multiKeyLeasePerformanceTest
```

### Task 10: English KDoc, bilingual README, diagram을 source-equivalent하게 갱신

**Complexity:** L
**Dependency:** Task 9
**Pattern skill:** `bluetape-writer`, `bluetape-diagram`

**Files:**
- Modify: all new public files under `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/`
- Modify: `infra/lettuce/README.md`
- Modify: `infra/lettuce/README.ko.md`
- Create/Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/lease/MultiKeyLeaseDocumentationTest.kt`
- Modify: `scripts/generate-infra-lettuce-diagram-01.mjs`
- Regenerate: `docs/images/readme-diagrams/infra-lettuce-diagram-01.svg`
- Regenerate: `docs/images/readme-diagrams/infra-lettuce-diagram-01.png`

- [ ] **Step 1: public English KDoc을 API와 테스트 결과에 맞춘다.**

KDoc에는 shared hash tag, external high-entropy token, acquire-only deterministic replay, partial/mismatch handling, same-token persistent integrity exception, operation별 ambiguous completion, single-writer/advisory boundary를 포함한다. token은 인증 credential이 아니며 JWT/session token/사용자 식별자/PII를 재사용하지 않고, Redis에 평문 저장되므로 ACL/TLS가 실제 보안 경계임을 명시한다.
same-slot validation이 connection `RedisCodec.encodeKey`의 실제 wire bytes를 사용하므로 custom
String key codec도 client routing과 동일한 slot을 계산한다는 계약을 KDoc에 포함한다.

- [ ] **Step 2: English/Korean README에 동일 구조의 executable example을 추가한다.**

```kotlin
val keys = listOf("ticket:{sale-42}:inflight:ip:$ipDigest", "ticket:{sale-42}:inflight:user:$userDigest")
val ownerToken = UUID.randomUUID().toString()
when (val result = lease.acquire(keys, ownerToken, Duration.ofSeconds(10))) {
    MultiKeyAcquireResult.Acquired -> startWorkflow()
    is MultiKeyAcquireResult.AlreadyOwned -> recoverExistingAttempt(result.minimumPttlMillis)
    is MultiKeyAcquireResult.PartialOwnership -> reconcile(result.counts)
    is MultiKeyAcquireResult.Conflicted -> reject(result.counts)
}
```

Retry→CircuitBreaker→Bulkhead 예제, bounded telemetry dimensions, token redaction, ACL/TLS 경계, JWT/session token/PII 재사용 금지, cutover/rollback, token-loss persistent-key cleanup runbook을 두 locale에 같은 순서로 쓴다.
완전한 caller 예제는 `Retry`, `CircuitBreaker`, `Bulkhead` config를 모두 생성하고
`.withRetry().withCircuitBreaker().withBulkhead()` 순서를 유지한다. owner token은 decorator 바깥에서
한 번 생성해 모든 attempt가 재사용하며, bounded non-zero production backoff를 사용하고
`Duration.ZERO`는 Task 8 deterministic test 전용이라고 설명한다. 이 exact policy와 recovery helper를
`MultiKeyLeaseDocumentationTest`가 compile하고 실제 Redis에서 smoke 실행해 README와 integration
fixture의 drift를 review 가능하게 만든다.

acquire 외에도 inspect/renew/release의 exhaustive `when` 또는 같은 정보를 담은 compact recovery
table을 두 locale에 둔다. `PartialLoss`, `PartialRelease`, `OwnershipMismatch`는 durable authority
reconciliation으로 연결하고 counts가 mutation 전 관찰값임을 명시한다. renew/release의 ambiguous
completion은 새 token이 아니라 same-token `inspect`로 시작하며, `Lost`만으로 prior release와 expiry를
구분할 수 없다는 한계를 예제/표에 포함한다.

migration checklist는 양쪽 README에서 다음 순서를 그대로 유지한다.

1. production key가 shared slot인지 확인하고 durable database guard를 유지한다.
2. 기존 writer를 중지한다.
3. 기존 최대 TTL만큼 drain하거나 기존 token으로 정리한다.
4. 같은 namespace/hash-tag/token 계약으로 새 writer를 활성화한다.
5. dual-write를 금지한다.
6. rollback은 새 writer 중지, drain/정리, durable authority 확인, 기존 writer 재활성화의 역순으로 수행한다.

token 유실 persistent key는 운영 승인으로 exact namespace/key 집합을 확인한 뒤 수동 삭제하거나
namespace를 교체하고 재검증한다. operation/result/exception만 metric dimension으로 허용하고
key/token은 log와 label에서 금지한다.

- [ ] **Step 3: diagram generator에 `Multi-Key Lease` family를 추가하고 SVG/PNG를 함께 생성하도록 한다.**

```javascript
import { execFileSync } from "node:child_process";
const svgPath = "docs/images/readme-diagrams/infra-lettuce-diagram-01.svg";
const pngPath = "docs/images/readme-diagrams/infra-lettuce-diagram-01.png";
writeFileSync(svgPath, svg.replace(/[ \t]+$/gm, ""));
execFileSync(process.env.CAIROSVG ?? "cairosvg", [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
```

- [ ] **Step 4: docs/visual validation을 실행한다.**

Run: `node scripts/generate-infra-lettuce-diagram-01.mjs`
Expected: SVG와 PNG가 모두 재생성되고 `Multi-Key Lease` label이 존재한다.

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.MultiKeyLeaseDocumentationTest" --no-parallel --max-workers=1`
Expected: README와 공유한 recovery/decorator policy가 compile되고 실제 Redis smoke가 PASS한다.

Run: `git diff --check && rg -n "Multi-Key Lease|LettuceMultiKeyLease|ACL|TLS|JWT|PII|operation|result|exception|key/token|single.writer|cutover|rollback|dual.write|durable|manual|namespace" infra/lettuce/README.md infra/lettuce/README.ko.md docs/images/readme-diagrams/infra-lettuce-diagram-01.svg`
Expected: 양 locale에 bounded telemetry, redaction, same-token recovery, decorator policy, ambiguous completion, cutover/rollback, dual-write 금지, token-loss cleanup heading/marker가 같은 순서로 존재하고 SVG에 새 primitive가 포함된다. test helper는 두 README의 normalized required-heading 목록을 읽어 순서와 source-equivalence도 assertion한다.

- [ ] **Step 5: Lore commit을 만든다.**

```text
Teach callers to recover multi-key lease outcomes safely

Constraint: Public KDoc is English while README locales remain source-equivalent.
Confidence: high
Scope-risk: narrow
Tested: diagram regeneration, locale marker scan, git diff --check
```

### Task 11: 통합 검증, review, lesson, exact-head PR

**Complexity:** XL
**Dependency:** Tasks 1-10
**Pattern skill:** `verification-before-completion`, `requesting-code-review`, `bluetape-full-feature`

**Files:**
- Create: `docs/lessons/2026-07-21-multi-key-ownership-lease.md`
- Create when useful: `docs/review/2026-07-21-issue-1065-multi-key-lease-review.md`

- [ ] **Step 1: targeted tests를 순차 실행한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test --tests "io.bluetape4k.redis.lettuce.lease.*" --tests "io.bluetape4k.redis.lettuce.script.RedisScriptTest" --no-parallel --max-workers=1`
Expected: PASS.

- [ ] **Step 2: full module와 static checks를 실행한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:test detekt detektTest --no-parallel --max-workers=1`
Expected: PASS; Testcontainers-backed tests는 다른 module/worktree와 병렬 실행하지 않는다.

- [ ] **Step 3: performance와 publication boundary를 다시 검증한다.**

Run: `lockf -k -t 900 "$(git rev-parse --git-common-dir)/bluetape-testcontainers.lock" ./gradlew :bluetape4k-lettuce:multiKeyLeasePerformanceTest :bluetape4k-lettuce:generatePomFileForBluetape4kPublication --no-parallel --max-workers=1`
Expected: performance criteria PASS; POM에 resilience4j dependency 없음.

- [ ] **Step 4: repository hygiene를 확인한다.**

Run: `git diff --check && git status --short && git diff --stat`
Expected: intended lease/test/docs/diagram/lesson files만 존재하고 placeholder·generated drift가 없다.

- [ ] **Step 5: spec/plan traceability와 여섯 관점 code review를 실행한다.**

Step 5 verifier checklist와 Step 6-R review를 적용해 performance, stability, security, ops, API, caller P0/P1을 0으로 만든다. 수정이 생기면 영향받은 targeted/full/static/performance validation을 처음부터 재실행한다.

- [ ] **Step 6: durable lesson을 Korean으로 작성하고 Lore commit한다.**

lesson은 context, decision, unexpected failure/review miss, outcome, exact verification, future guard를 포함한다.

```text
Record the multi-key lease delivery boundary

Confidence: high
Scope-risk: narrow
Tested: full verification and six-lens review evidence
```

- [ ] **Step 7: exact head를 push하고 English PR을 생성한다.**

Base: `develop`
Head: `feat/issue-1065-multi-key-lease`
Repository: `bluetape4k/bluetape4k-projects`

PR body는 `Closes #1065`, public API/result contract, Redis Cluster boundary, resilience test-only proof, exact validation commands, migration/rollback을 포함한다. performance evidence 표에는 여섯 조합의 p50/p95/throughput, result별 count, error count, PING p95/p99, warm-up/sample/probe 설정, Redis image, JVM/CPU 환경을 기록한다.
Testcontainers validation 표에는 shared `lockf` path, acquire/release 시각, 대기 여부와 timeout이
없었음을 함께 기록한다.

- [ ] **Step 8: live CI/review를 확인하고 merge-ready에서 멈춘다.**

exact local SHA = remote head = PR head를 확인하고 CI, current reviews, unresolved threads가 모두 통과한 뒤 사용자에게 별도 fresh merge approval을 요청한다. auto-merge와 merge는 수행하지 않는다.

## 4. Spec-to-task traceability

| Spec requirement | Plan task |
|---|---|
| Stateless per-call API, config maxKeys | 2, 3, 5, 6 |
| Same-slot standalone/cluster | 1, 3, 5, 6, 7 |
| Acquire/inspect/renew/release results | 2, 3, 4 |
| Lua atomic competition boundary/runtime ambiguity | 4, 7 |
| Sync/async/suspend parity and cancellation | 5, 6, 7 |
| NOSCRIPT fallback | 1, 7 |
| Retry/CircuitBreaker/Bulkhead external composition | 8 |
| O(n), maxKeys=32 characterization | 9 |
| English KDoc/bilingual README/diagram | 10 |
| POM/runtime dependency exclusion | 8, 11 |
| Migration, rollback, telemetry, integrity runbook | 10 |
| Full tests/static/review/lesson/PR | 11 |

## 5. Execution stop conditions

- 각 TDD step에서 expected RED를 관찰하지 못하면 구현 전 테스트가 이미 충족되는 이유를 조사한다.
- Testcontainers timing failure는 단순 재시도로 덮지 않고 root cause를 수정한 뒤 해당 validation lane 전체를 재실행한다.
- public API/result/migration 의미가 spec과 달라져야 하면 구현을 멈추고 spec 수정·사용자 승인·Step 2-R부터 반복한다.
- PR merge-ready 보고 후 fresh merge approval 전에는 merge하지 않는다.
