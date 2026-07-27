# Issue #1080 Lettuce Synchronizers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` or `executing-plans` to implement this plan task-by-task. Follow `bluetape-workflow`, `bluetape-full-feature`, `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`, `bluetape-writer`, and `bluetape-diagram`. Redis/Testcontainers and diagram rendering commands are serialized.

**Goal:** Deliver issue #1080 Delivery 2 as additive Lettuce implementations of a generation-safe distributed semaphore, a fixed-lease expirable permit semaphore, and a monotonic-generation count-down latch with blocking, async, and suspend semantic parity.

**Architecture:** Public identities, handles, configs, outcomes, and six caller-facing objects live under `io.bluetape4k.redis.lettuce.synchronizer`. Internal key layouts, Lua scripts, reply decoding, bounded polling, cancellation, and standalone/Cluster command adapters reuse `RedisScriptRunner` and the merged Lock-family conventions. Redis is the authority for capacity, permit ownership, expiry, latch count, and non-reused generation; legacy `LettuceSemaphore` remains unchanged.

**Tech Stack:** Kotlin 2.3, Java 21, Lettuce, Redis Lua, Kotlin Coroutines, `CompletableFuture`, JUnit 5, Testcontainers Redis, Gradle Kotlin DSL, SVG, CairoSVG.

**Approved design:** `docs/superpowers/specs/2026-07-25-issue-1080-lettuce-locks-synchronizers-design.md`

---

## 1. Delivery boundary

| Item | Fixed value |
|---|---|
| Repository | `bluetape4k/bluetape4k-projects` |
| Issue | `#1080` |
| Base | `develop` |
| Head | `codex/issue-1080-lettuce-synchronizers` |
| Included | distributed semaphore, expirable permit semaphore, count-down latch, blocking/async/suspend, tests, KDoc, English/Korean docs, localized diagram |
| Preserved | Lock delivery, `LettuceSemaphore`, `LettuceSuspendSemaphore`, root-checkout user drafts |
| Deferred | cross-family public API convergence |
| Forbidden | dependency additions, Lock redesign, legacy removal, merge, auto-merge |
| Stop | exact-head PR is CI/review/thread clean and merge-ready; `CG-16` remains pending |

The copied `SynchronizerTypes.kt` and `SynchronizerScripts.kt` are prior user work. Their useful identity redaction, serialization validation, result vocabulary, and Lua starting points are retained, but they are not accepted without tests. In particular:

- latch delete must never delete its generation key;
- initialization must not reuse generations after delete;
- non-expirable semaphore state must not be reclaimed by an expiry cleanup path;
- expirable permits require one unique permit ID and one fixed per-permit deadline;
- release consumes the complete acquired handle exactly once;
- every reply must be bounded and decoded into the same outcome across modes;
- sync validation must happen before Redis dispatch;
- async and suspend cancellation must cancel only the local wait/command and must not mutate ownership or latch state.

## 2. Fixed public contract

### 2.1 Identity, handles, configuration, and results

**Files:**

- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerTypes.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerObservation.kt`
- Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerTypesTest.kt`

The public contract uses opaque, serializable identities with redacted `toString()`:

```kotlin
class SemaphoreOwnerId private constructor(internal val value: String): Serializable {
    companion object {
        @JvmStatic fun random(): SemaphoreOwnerId
        @JvmStatic fun from(value: String): SemaphoreOwnerId
    }
}

class SemaphoreRequestId private constructor(internal val value: String): Serializable {
    companion object {
        @JvmStatic fun random(): SemaphoreRequestId
        @JvmStatic fun from(value: String): SemaphoreRequestId
    }
}

data class PermitHandle(
    val objectFingerprint: String,
    val ownerId: SemaphoreOwnerId,
    val generation: Long,
    val requestId: SemaphoreRequestId,
    val permits: Int,
    val token: String,
): Serializable

data class ExpirablePermitHandle(
    val permit: PermitHandle,
    val leases: List<ExpirablePermitLease>,
): Serializable

data class ExpirablePermitLease(
    val permitId: String,
    val deadlineMillis: Long,
): Serializable

class LatchRequestId private constructor(internal val value: String): Serializable {
    companion object {
        @JvmStatic fun random(): LatchRequestId
        @JvmStatic fun from(value: String): LatchRequestId
    }
}

data class LatchGeneration(val value: Long): Comparable<LatchGeneration>, Serializable
```

The following operation-specific result families remain identical across blocking, async, and suspend surfaces:

```kotlin
sealed interface SemaphoreInitializationResult: Serializable
sealed interface PermitAcquireResult<out H: Serializable>: Serializable
sealed interface PermitMutationResult<out H: Serializable>: Serializable
sealed interface PermitInspectResult<out H: Serializable>: Serializable
sealed interface PermitRenewResult<out H: Serializable>: Serializable
sealed interface LatchSetCountResult: Serializable
sealed interface LatchCountResult: Serializable
sealed interface LatchInspectResult: Serializable
sealed interface LatchAwaitResult: Serializable
sealed interface LatchMutationResult: Serializable
```

Every family has a stable `Closed` result where object or runtime closure can terminate an operation. Backend and integrity failures carry bounded enums plus an allowed recovery action, never caller-controlled reason strings. Secret-bearing values render only a stable redacted digest; raw names, keys, owner/request IDs, permit IDs, and generations never enter messages, labels, or logs.

Required outcome mapping:

| Operation | Success | Normal negative | Integrity/lifecycle |
|---|---|---|---|
| semaphore initialize | `Initialized(generation)` | `AlreadyInitialized`, `InvalidCapacity` | `IntegrityFailure`, `BackendFailure` |
| semaphore acquire | `Acquired(handle)` | `Unavailable`, `TimedOut`, `CapacityExceeded` | `Ambiguous`, `IntegrityFailure`, `BackendFailure` |
| permit inspect | `Owned(handle, remainingPermits)` | `Released`, `Expired` | `StaleGeneration`, `IntegrityFailure`, `BackendFailure` |
| permit release | `Released(handle, remainingPermits)` | `AlreadyReleased`, `Expired` | `StaleGeneration`, `Ambiguous`, `IntegrityFailure`, `BackendFailure` |
| expirable renew | `Renewed(handle, deadlines)` | `Released`, `Expired`, `OwnershipLost` | `StaleGeneration`, `Ambiguous`, `IntegrityFailure`, `BackendFailure` |
| latch create | `Created(generation)` | `ActiveGeneration(generation, count)`, `InvalidCount` | `IntegrityFailure`, `BackendFailure` |
| latch get/inspect | `Active(generation, count, waiters)`, `Completed(generation)` | `Deleted` | `StaleGeneration`, `IntegrityFailure`, `BackendFailure` |
| latch count-down | `Decremented(remaining)`, `Completed` | `AlreadyCompleted`, `Deleted` | `StaleGeneration`, `Ambiguous`, `IntegrityFailure`, `BackendFailure` |
| latch await | `Completed` | `TimedOut`, `Deleted` | `StaleGeneration`, `Ambiguous`, `IntegrityFailure`, `BackendFailure` |
| latch delete | `Deleted` | `NotFound`, `ActiveWaiters(count)` | `StaleGeneration`, `Ambiguous`, `IntegrityFailure`, `BackendFailure` |

Configuration validation before dispatch:

```kotlin
data class SemaphoreConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val pollInterval: Duration = Duration.ofMillis(25),
)

data class ExpirableSemaphoreConfig(
    val semaphore: SemaphoreConfig = SemaphoreConfig(),
    val leaseTime: Duration = Duration.ofSeconds(30),
)

data class LatchConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val pollInterval: Duration = Duration.ofMillis(25),
)
```

Bounds are fixed at: resource name 1..128 UTF-8 bytes, namespace 1..128 bytes, identity/token 1..256 bytes, capacity/request count 1..1,000,000, generation 1..`Long.MAX_VALUE`, wait time 0..24 hours, lease 100 ms..24 hours, and poll interval 5 ms..1 second. Duration conversion uses checked whole milliseconds and rejects overflow.

Each public object has no-config/config standalone and Cluster `@JvmStatic create` overloads. Scheduler/observation overloads reuse the merged `CoordinationRuntime`; injected schedulers and Redis connections remain caller-owned. Observation dimensions are limited to object kind, operation, outcome, failure kind, and lease policy.

### 2.2 Key layout and Redis protocol

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/internal/SynchronizerProtocol.kt`
- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerScripts.kt`
- Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerProtocolTest.kt`

All keys for one object share one Redis Cluster hash tag:

```text
{namespace}:{hashTag}:semaphore:{resource}:available
{namespace}:{hashTag}:semaphore:{resource}:generation
{namespace}:{hashTag}:semaphore:{resource}:holds
{namespace}:{hashTag}:semaphore:{resource}:requests
{namespace}:{hashTag}:semaphore:{resource}:deadlines

{namespace}:{hashTag}:latch:{resource}:count
{namespace}:{hashTag}:latch:{resource}:generation
{namespace}:{hashTag}:latch:{resource}:waiters
```

If `hashTag` is absent, the validated resource name is the tag. Derived keys are checked against Redis’s 512 MiB limit using the project’s narrower 512-byte bound. Lua replies are flat arrays of at most six UTF-8 fields and 2 KiB total; the decoder rejects unknown tags, wrong arity, invalid numbers, negative counts, and oversized fields as `IntegrityFailure`.

The semaphore generation key is incremented only when a previously absent logical semaphore is initialized. Non-expirable holds are stored without deadline state. Expirable acquisition creates one allocation record with `allocationId -> permits|generation|owner|request`, plus exactly `N` unit lease records `permitId -> allocationId|generation` and one deadline score per unit. Cleanup or whole-handle release removes the allocation and all of its unit leases atomically and restores the allocation's total permit count exactly once; unit lease records never repeat the allocation total. Cleanup is invoked only by expirable operations. Latch delete removes count and waiter keys but never generation.

## 3. Ordered TDD execution

### Task 1: Freeze the public contract

**Files:**

- Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerTypesTest.kt`
- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerTypes.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerObservation.kt`

- [ ] **Step 1 — Write the failing type and validation tests**

```kotlin
@Test
fun `identities handles and generations redact secrets and reject invalid state`() {
    SemaphoreOwnerId.from("owner").toString() shouldBeEqualTo "SemaphoreOwnerId(<redacted>)"
    invoking { PermitHandle("fp", SemaphoreOwnerId.from("o"), 0, SemaphoreRequestId.from("r"), 1, "t") }
        .shouldThrow<IllegalArgumentException>()
    invoking { LatchGeneration(0) }.shouldThrow<IllegalArgumentException>()
}

@Test
fun `configs reject overflow and out of range durations before dispatch`() {
    invoking { SemaphoreConfig(pollInterval = Duration.ofMillis(4)) }
        .shouldThrow<IllegalArgumentException>()
    invoking { ExpirableSemaphoreConfig(leaseTime = Duration.ofHours(25)) }
        .shouldThrow<IllegalArgumentException>()
}
```

- [ ] **Step 2 — Verify RED**

Run:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerTypesTest' --no-parallel --max-workers=1
```

Expected: FAIL because the final config bounds/result types are missing or inconsistent.

- [ ] **Step 3 — Implement the minimal types**

Keep constructors validated, all secret-bearing `toString()` output redacted, Java factories explicit, serialized restoration fail-closed, and observation dimensions limited to object kind, operation, outcome, and execution mode.

- [ ] **Step 4 — Verify GREEN**

Run the same targeted command. Expected: PASS with no new compiler warning.

### Task 2: Lock the key and Lua protocol

**Files:**

- Test: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerProtocolTest.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/internal/SynchronizerProtocol.kt`
- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerScripts.kt`

- [ ] **Step 1 — Write failing protocol tests**

```kotlin
@Test
fun `every object key shares one cluster slot`() {
    val keys = semaphoreKeys("orders", SemaphoreConfig(hashTag = "orders"))
    keys.all.map(::redisSlot).distinct().size shouldBeEqualTo 1
}

@Test
fun `latch delete preserves generation monotonicity`() {
    LatchScripts.DELETE_SCRIPT.source shouldContain "redis.call('del', KEYS[1], KEYS[3])"
    LatchScripts.DELETE_SCRIPT.source shouldNotContain "redis.call('del', KEYS[1], KEYS[2], KEYS[3])"
}

@Test
fun `reply decoder rejects unknown and oversized replies`() {
    decodePermitReply(listOf("UNKNOWN")).shouldBeInstanceOf<PermitProtocolReply.IntegrityFailure>()
}

@Test
fun `three expirable unit leases restore exactly three permits`() {
    acquireExpirable(permits = 3).leases shouldHaveSize 3
    cleanupExpired()
    availablePermits() shouldBeEqualTo initialCapacity
}
```

- [ ] **Step 2 — Verify RED**

Run:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerProtocolTest' --no-parallel --max-workers=1
```

Expected: FAIL on missing key layout/decoder and the copied latch delete bug.

- [ ] **Step 3 — Implement minimal protocol and repair scripts**

Use `RedisScriptRunner` only. Keep all multi-key scripts atomic, preserve the generation key on latch deletion, split non-expirable and expirable permit paths, cap cleanup work at configured capacity, and never include raw owner/request/token values in exceptions or observations.

- [ ] **Step 4 — Verify GREEN**

Run the same targeted command. Expected: PASS.

### Task 3: Implement `LettuceDistributedSemaphore`

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/internal/DistributedSemaphoreClient.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceDistributedSemaphore.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceSuspendDistributedSemaphore.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/DistributedSemaphoreContract.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceDistributedSemaphoreTest.kt`

Public operations:

```kotlin
fun trySetPermits(permits: Int): SemaphoreInitializationResult
fun tryAcquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int): PermitAcquireResult<PermitHandle>
fun acquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int, waitTime: Duration): PermitAcquireResult<PermitHandle>
fun inspect(handle: PermitHandle): PermitInspectResult<PermitHandle>
fun reconcile(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId): PermitInspectResult<PermitHandle>
fun release(handle: PermitHandle): PermitMutationResult<PermitHandle>
fun availablePermits(): Int
```

The blocking class also exposes matching `*Async` methods returning `CompletableFuture`; the suspend class exposes matching suspend functions. One handle always represents atomic acquisition of all requested permits and releases all of them exactly once.

- [ ] **Step 1 — Write failing three-mode contract tests**

Tests must cover initialization result variants, forbidden capacity shrink while holds are active, capacity, unavailable/timeout distinction, exact N acquire/release, request replay, cross-owner rejection, wrong object fingerprint, double release, same-identity reconcile after ambiguous acquire/release, owned/released/stale reconcile states, wait timeout, stable `Closed` outcomes, async cancellation, suspend cancellation, close ownership, standalone factory, Cluster factory, and sync pre-dispatch validation.

- [ ] **Step 2 — Verify RED**

Run:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceDistributedSemaphoreTest' --no-parallel --max-workers=1
```

Expected: compilation/test failure because the public objects do not exist.

- [ ] **Step 3 — Implement minimal blocking/async/suspend surfaces**

Use one internal client with mode adapters, the merged `CoordinationRuntime` and `CoordinationDeadline`, runtime task registration for async and suspend polling, leaf future cancellation, idempotent request replay, hard registration caps, and non-owning close semantics. Production suspend code never uses `runBlocking`, `Thread.sleep`, or an IO-dispatcher wrapper around a blocking Redis call.

- [ ] **Step 4 — Verify GREEN**

Run the same targeted command. Expected: all blocking, future, suspend, standalone, and Cluster contract cases PASS.

### Task 4: Implement `LettucePermitExpirableSemaphore`

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/internal/ExpirableSemaphoreClient.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettucePermitExpirableSemaphore.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceSuspendPermitExpirableSemaphore.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/ExpirableSemaphoreContract.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettucePermitExpirableSemaphoreTest.kt`

Additional public operation:

```kotlin
fun renew(handle: ExpirablePermitHandle, extension: Duration): PermitRenewResult<ExpirablePermitHandle>
```

Each acquired permit has one cryptographically opaque `permitId` and its own deadline. An atomic acquisition of `N` permits returns one `ExpirablePermitHandle` whose `leases` list has exactly `N` unique entries and whose embedded `PermitHandle.permits` is `N`. Release and renew consume the complete handle; partial release/renew is deferred. Renewal atomically replaces every still-owned entry deadline with Redis `TIME + extension`; a missing or mismatched entry returns ownership/integrity failure without partially renewing the allocation.

- [ ] **Step 1 — Write failing expiry contract tests**

Cover `N > 1` unique permit IDs and per-entry deadlines, fixed lease expiry, cleanup restoring capacity exactly once, inspect-before/after-expiry, release-before/after-expiry, atomic replacement renew, no partial renew, stale generation, double release, request replay/reconcile, concurrent expiry/acquire, clock-boundary behavior, async cancellation, suspend cancellation, and raw-ID redaction.

- [ ] **Step 2 — Verify RED**

Run:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettucePermitExpirableSemaphoreTest' --no-parallel --max-workers=1
```

Expected: FAIL because expirable public/client behavior is absent.

- [ ] **Step 3 — Implement minimal expirable behavior**

Execute cleanup and mutation atomically in Lua, use Redis server time where the script needs one authoritative deadline comparison, keep renewal fixed rather than watchdog-based, and return `Expired` without restoring capacity twice. Permanent permits are not exposed in Delivery 2; documentation names them as unsupported rather than silently using an infinite or sentinel deadline.

- [ ] **Step 4 — Verify GREEN**

Run the same targeted command. Expected: PASS.

### Task 5: Implement `LettuceCountDownLatch`

**Files:**

- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/internal/CountDownLatchClient.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceCountDownLatch.kt`
- Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceSuspendCountDownLatch.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/CountDownLatchContract.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/LettuceCountDownLatchTest.kt`

Public operations:

```kotlin
fun trySetCount(count: Int, requestId: LatchRequestId): LatchSetCountResult
fun getCount(generation: LatchGeneration): LatchCountResult
fun inspect(generation: LatchGeneration): LatchInspectResult
fun countDown(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult
fun await(generation: LatchGeneration, requestId: LatchRequestId, waitTime: Duration): LatchAwaitResult
fun delete(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult
```

The blocking class exposes matching async methods; the suspend class exposes matching suspend functions.

- [ ] **Step 1 — Write failing latch contract tests**

Cover request-idempotent create after ambiguous completion, active-generation rejection, `getCount`, `inspect`, zero floor, completion, stale generation, explicit delete, `Deleted` versus `NotFound`, generation increase after delete, per-request mutation replay/reconcile, waiter accounting, active-waiter delete rejection, timeout, stable `Closed` outcomes, async cancellation, suspend cancellation, no count mutation from cancellation, cross-client completion, Cluster factory, and invalid pre-dispatch inputs.

- [ ] **Step 2 — Verify RED**

Run:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceCountDownLatchTest' --no-parallel --max-workers=1
```

Expected: FAIL because latch objects and corrected monotonic-generation protocol do not exist.

- [ ] **Step 3 — Implement minimal latch behavior**

Increment generation on every successful create, never delete generation, clamp count at zero, register/unregister waiters with compare-generation Lua, reject delete while waiters remain, and ensure local cancellation only removes its waiter registration.

- [ ] **Step 4 — Verify GREEN**

Run the same targeted command. Expected: PASS.

### Task 6: Sequential Redis, Cluster, protocol, and lifecycle verification

**Files:**

- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerRedisProtocolTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerCancellationTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerClusterTest.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerPerformanceStabilityTest.kt`
- Create: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/synchronizer/LettuceSynchronizerJavaDocumentationTest.java`

- [ ] **Step 1 — Run Redis protocol tests alone**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerRedisProtocolTest' --no-parallel --max-workers=1
```

Expected: PASS for EVALSHA, NOSCRIPT fallback, malformed replies, generation persistence, expiry cleanup, and atomic contention.

The command observer must prove warm immediate operations use one Redis command and cold `NOSCRIPT` uses exactly two. Cleanup work is bounded by capacity/batch limits and malformed or oversized responses fail closed.

- [ ] **Step 2 — Run cancellation/lifecycle tests alone**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerCancellationTest' --no-parallel --max-workers=1
```

Expected: PASS; cancelled future/coroutine leaves Redis ownership/count unchanged and cancels the leaf command/wait.

- [ ] **Step 3 — Run Cluster tests alone**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerClusterTest' --no-parallel --max-workers=1
```

Expected: PASS without CROSSSLOT or redirect-loop failure. Cover standalone and Cluster dispatch for semaphore, expirable semaphore, and latch. For every multi-key family, add a custom `RedisCodec` fixture that encodes otherwise similar derived keys into different wire-byte hash slots; construction or pre-dispatch validation must return a sanitized cross-slot failure before Redis receives a command.

- [ ] **Step 4 — Compile Java public usage**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceSynchronizerJavaDocumentationTest' --no-parallel --max-workers=1
```

Expected: PASS for no-config/config standalone/Cluster factories and handle lifecycle.

- [ ] **Step 5 — Run bounded performance/stability checks alone**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerPerformanceStabilityTest' --no-parallel --max-workers=1
```

Expected: PASS for 100 concurrent contenders without capacity drift, 10,000 waiter/task cap fail-closed behavior, bounded cleanup batches, no scheduler-thread blocking, and five sequential Testcontainers lifecycle repetitions without leaked objects or tasks.

### Task 7: English/Korean KDoc, selection docs, and diagram

**Files:**

- Modify: `infra/lettuce/README.md`
- Modify: `infra/lettuce/README.ko.md`
- Modify: `infra/lettuce/CoordinationLocks.md`
- Modify: `infra/lettuce/CoordinationLocks.ko.md`
- Create: `infra/lettuce/images/coordination-synchronizers-architecture.svg`
- Create: `infra/lettuce/images/coordination-synchronizers-architecture.png`
- Create: `infra/lettuce/images/coordination-synchronizers-architecture.ko.svg`
- Create: `infra/lettuce/images/coordination-synchronizers-architecture.ko.png`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/synchronizer/SynchronizerDocumentationTest.kt`
- Create: `docs/superpowers/reviews/2026-07-27-issue-1080-synchronizer-diagram-review.md`

The selection table must distinguish:

| Choose | When | Handle/generation rule | Do not choose when |
|---|---|---|---|
| `LettuceDistributedSemaphore` | bounded concurrent admission without lease expiry | atomic N-permit handle, exact-once full release | abandoned callers must self-heal by time |
| `LettucePermitExpirableSemaphore` | permits must recover after fixed lease | opaque permit ID, fixed deadline, explicit renew | watchdog semantics are required |
| `LettuceCountDownLatch` | one generation waits for monotonic count-to-zero | generation never reused; explicit delete | permits must be reacquired/released repeatedly |
| legacy `LettuceSemaphore` | compatibility with existing local-stack contract | legacy owner/local behavior | new explicit handle lifecycle is required |

- [ ] **Step 1 — Write failing documentation tests**

Tests assert English/Korean class rows, equivalent headings, selection-table terms, root-relative diagram embeds, KDoc coverage, Java snippets, and legacy/deferred boundaries.

- [ ] **Step 2 — Verify RED**

```bash
./gradlew :bluetape4k-lettuce:test --tests '*SynchronizerDocumentationTest' --no-parallel --max-workers=1
```

Expected: FAIL because new docs/assets are absent.

- [ ] **Step 3 — Write English KDoc/docs from final APIs**

Document same-mode parity, handle exact-once rules, expiry and renew, generation non-reuse, cancellation, Redis Cluster same-slot, connection ownership, failure/reconciliation, and unsupported watchdog/API-convergence behavior. The operator section must also state minimum Redis scripting/key ACL expectations, TLS/credential responsibility, namespace isolation, bounded metric dimensions, alert signals for timeout/integrity/capacity/cleanup, and rollback/key-cleanup guidance.

- [ ] **Step 4 — Localize Korean docs naturally**

Keep API names exact while using natural Korean technical prose; preserve heading, table, example, link, and limitation parity.

- [ ] **Step 5 — Create and validate diagrams sequentially**

For each locale: source-read → one SVG edit → `xmllint` → CairoSVG scale 2 → common/architecture audits → original PNG inspection → evidence ledger. Do not edit or render both locales concurrently.

- [ ] **Step 6 — Verify GREEN**

Run the same documentation test. Expected: PASS.

### Task 8: Final review, lessons, Lore commit, PR, and CI

**Files:**

- Create: `docs/superpowers/reviews/2026-07-27-issue-1080-synchronizer-implementation-review.md`
- Create or update after lesson gate: `docs/lessons/2026-07-27-lettuce-synchronizer-generation-and-cancellation.md`
- Modify: `docs/superpowers/checklists/2026-07-27-issue-1080-lettuce-synchronizers.md`

- [ ] **Step 1 — Run final sequential verification**

```bash
./gradlew :bluetape4k-lettuce:test --no-parallel --max-workers=1
./gradlew :bluetape4k-lettuce:compileKotlin :bluetape4k-lettuce:compileTestKotlin --no-parallel --max-workers=1
git diff --check
```

Expected: PASS with no new diagnostics attributable to this delivery.

- [ ] **Step 2 — Run independent reviews**

Independent read-only lenses cover security, performance, stability, API/Kotlin, user/docs, and tests/cancellation. Main-session integration deduplicates findings and blocks on any P0/P1. Every repair reruns its affected test and lens until `P0=0 / P1=0`.

- [ ] **Step 3 — Pass the lesson gate**

Record a lesson only if the work exposes a reusable rule not already covered by current lessons. At minimum evaluate latch generation persistence, expirable cleanup idempotency, and cancellation-without-mutation.

- [ ] **Step 4 — Create exact Lore commit**

Commit message:

```text
Preserve explicit ownership across Lettuce synchronizer lifecycles

Constraint: Delivery 2 follows merged Lock contracts and keeps legacy semaphore APIs compatible
Rejected: Reusing latch generations after delete | stale waiters could observe a different lifecycle
Rejected: Local permit bookkeeping | Redis must remain authoritative across clients and cancellation
Confidence: high
Scope-risk: moderate
Directive: Complete cross-family API convergence only in Delivery 3
Tested: Targeted synchronizer contracts, Redis protocol, cancellation, Cluster, Java/KDoc, module tests, docs and diagram audits
Not-tested: Merge and post-merge synchronization require fresh user approval
```

- [ ] **Step 5 — Push and create the issue-linked PR**

Refresh authority, push exact head, and create an English PR with base `develop`, head `codex/issue-1080-lettuce-synchronizers`, issue #1080 linkage, assignee/labels/milestone parity, and `## DoD Status` as the final H2 section.

- [ ] **Step 6 — Poll CI, reviews, and threads**

Poll until required checks are terminal and green, all P0/P1 findings are zero, and no review thread remains unresolved. Stop at merge-ready with `CG-16` pending; never merge or enable auto-merge.

## 4. Risk-to-test matrix

| Risk | Required proof |
|---|---|
| copied latch script reuses generation | delete/create Redis test proves strictly increasing generation |
| expiry cleanup restores twice | concurrent cleanup/release test proves capacity never exceeds configured total |
| non-expirable path accidentally expires | clock-advance test proves ownership persists until release |
| blocking validation dispatches Redis | command observer remains zero for invalid input |
| future/suspend cancellation mutates state | cancellation tests inspect unchanged Redis handle/count |
| multi-key Cluster script crosses slots | slot unit test plus real Cluster contract |
| malformed Lua reply becomes exception leak | bounded decoder returns sanitized integrity result |
| raw identities leak | `toString`, exception, observation, and captured log assertions |
| request replay allocates twice | same request returns same handle and unchanged capacity |
| expirable `N` acquisition aliases permit identity | handle contains exactly `N` unique unit permit IDs and per-entry deadlines; cleanup/release of `N=3` restores exactly 3, never 9 |
| custom codec changes derived-key wire slots | custom `RedisCodec` split-slot fixtures reject semaphore, expirable semaphore, and latch scripts before dispatch |
| stale handle affects new lifecycle | generation mismatch returns `StaleGeneration` without mutation |
| latch count drops below zero | repeated count-down remains zero and returns terminal outcome |
| delete races active await | waiter-count script blocks delete until waiter unregisters |
| async polling blocks scheduler thread | timing/thread probe and absence of `Thread.sleep`/`runBlocking` in production |
| docs select wrong primitive | selection-table test and user-review lens |

## 5. Completion DoD

- Every approved Delivery 2 requirement maps to a passing test or documentation assertion.
- All public APIs have English KDoc and Java-callable factories.
- Blocking, async, and suspend outcome taxonomies match.
- Redis/Testcontainers and protocol checks ran sequentially with heavy-command concurrency one.
- English/Korean README and coordination guide have equivalent selection tables and localized diagrams.
- Independent final review is `P0=0 / P1=0`.
- Exact Lore commit is pushed and PR metadata/body are verified live.
- CI, reviews, and threads are clean at exact head.
- The only merge blocker is fresh `CG-16` approval.
