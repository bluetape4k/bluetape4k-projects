# NearJCache 표준 read/clear 계약 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `NearJCache`가 `javax.cache.Cache`로 참조될 때 front/back을 하나의 논리적 2-tier 캐시로 제공하도록 표준 `get`, `containsKey`, `getAll`, `clear` 계약을 구현·검증·문서화한다.

**Architecture:** 기존 `NearJCache<K,V> : JCache<K,V> by backCache` 공개 타입과 `getDeeply`/`clearAllCache` 이름은 유지한다. 표준 read는 front 우선, back fallback, back 값의 front populate로 통일하고, 표준 `clear`는 현재 near-cache의 front와 back을 동기적으로 지운다. Compound operation 원자성은 이 계획에서 변경하지 않고 #1355로 분리한다.

**Tech Stack:** Kotlin, JCache 1.1 (`javax.cache.Cache`), JUnit 5, MockK, Awaitility, Gradle/Kover.

---

## 파일 영향과 소유권

- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt` — 표준 read/clear 구현과 Korean KDoc.
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt` — 기본 front cache를 store-by-reference로 고정.
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt` — `Cache<K,V>` 타입 경계의 결정적 mock 기반 회귀 테스트.
- Modify: `cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/jcache/AbstractNearJCacheTest.kt` — 기존 front-only `clear` 가정을 표준 front/back 계약과 peer 전파 경계로 전환.
- Modify: 영향이 확인된 backend tests, 최소 `cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt` — factory degraded 조합의 clear 기대치를 새 계약에 맞춤.
- Modify: `cache/cache-core/README.md`, `cache/cache-core/README.ko.md` — JCache 표준 메서드 의미와 `getDeeply`/`clearAllCache` 호환 alias, peer 전파 제한.
- Modify: `docs/cache/near-cache-capability-matrix.md` — JCache read/clear 계약과 listener/degraded 전파 경계를 추가.
- Create: `docs/lessons/2026-08-12-issue-1363-nearjcache-contract.md` only if the lesson gate finds reusable new guidance; otherwise record concrete N/A evidence in the workflow report.

No new module, dependency, catalog version, public type removal, or compound-operation
atomicity implementation is in scope. Do not touch unrelated dirty files. The
front-capacity limit and shared-back tenant/owner authority model remain explicit
follow-up work; this change documents those boundaries but does not add a public
configuration surface for them.

## Task 1: Add deterministic standard-Cache regression tests (RED)

**Files:**
- Create: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt`

- [x] **Step 1: Add a `Cache<String, String>` read-through test**

Use the existing `JCache` typealias, relaxed MockK caches, and a `NearJCacheConfig(isSynchronous = true)` constructor. Stub `front.get("remote")` to return null, `back.get("remote")` to return `"value"`, assign the near cache to `val standardCache: Cache<String, String>`, call `standardCache.get("remote")`, then assert the value and verify `front.put("remote", "value")` once.

- [x] **Step 2: Add contains-key and mixed getAll tests**

Stub front `containsKey("remote")` false and back true, then assert `standardCache.containsKey("remote")` true. For `getAll`, stub front to return `mutableMapOf("front" to "front-value")`, back to return `mutableMapOf("remote" to "back-value")` for the missing-key set, call through `Cache`, assert both mappings, and verify only the back value is populated into front.

- [x] **Step 3: Add clear and compatibility-alias tests**

Stub `front.clear()` and `back.clear()`, call `standardCache.clear()`, and verify both exactly once. Add a test that `getDeeply` follows the standard `get` path and a test that `clearAllCache` clears both layers through the same implementation. Use existing Bluetape assertions and `io.mockk.verify` rather than raw JUnit assertions where ecosystem helpers cover the assertion.

- [x] **Step 4: Run the new tests before implementation**

Run:

```bash
./gradlew :bluetape4k-cache-core:test --tests '*NearJCacheContractTest' --no-configuration-cache
```

Expected: FAIL because standard `get`, `containsKey`, `getAll`, and `clear` currently remain front-only/clear-front behavior. Preserve the raw failure evidence in the task log; do not implement before observing RED.

- [x] **Step 5: Add concurrency, failure, and boundary tests**

  Add deterministic `CountDownLatch` tests for `get`/`getAll` racing with
  `put`, `replace`, `remove`, and `clear`; the stale back value may be returned
  to the in-flight caller but must never be populated into front after the
  epoch changes. Add a timeout-late asynchronous write test proving `clear()`
  waits for the actual backend call before returning. Add tests that front
  population catches `RuntimeException` but rethrows `Error`, and that captured
  logs contain operation/provider metadata only (no key/value/payload). Add
  call-count tests for `getAndRemove` and `getAndReplace` proving their
  front-only helpers do not introduce the new standard back read round trips.

- [x] **Step 6: Add serialization trust-boundary tests**

  Assert the default `NearJCacheConfig` sets `isStoreByValue == false` and that
  constructing `NearJCache` with a custom store-by-value front configuration
  fails closed with a clear `IllegalArgumentException` before any back value is
  read or populated. This keeps the default auto-populate path away from an
  unfiltered Java serialization copier.

## Task 2: Implement the two-tier contract with epoch and backend barriers (GREEN)

**Files:**
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt` around lines 173-247.
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheConfig.kt` default front configuration.

- [x] **Step 1: Add mutation gate, epoch, and backend write lock**

  Introduce a `ReentrantLock` mutation gate, a monotonic `AtomicLong`
  `mutationEpoch` for read-through fencing, a separate `AtomicLong`
  `backWriteGeneration` advanced only by `clear`, and a `ReentrantLock` around
  every backend mutation. Every front mutation increments `mutationEpoch` before
  changing front state. A backend task captures the current generation and
  skips itself only when a later clear has advanced it; different-key async
  writes must not be discarded merely because another mutation occurred. Keep
  the existing timeout and retry behavior, but ensure the lock is held until
  the real backend call ends, including after an async completion timeout.

- [x] **Step 2: Implement standard get with back fallback and fenced populate**

  Make `get(key)` read front first, capture the observed epoch, then read back on
  miss. When back returns a value, populate front only if the epoch is unchanged;
  return the back value regardless of a local `RuntimeException` during
  populate, but propagate `Error` and back-read failures. Warning logs contain
  only stable operation/provider/cache metadata and never raw key/value/payload.
  Keep the code non-suspending and do not introduce `!!` or new dependencies.

- [x] **Step 3: Implement containsKey and getAll over the logical cache**

  Implement `containsKey` as front lookup followed by back lookup without calling
  `get` or triggering population. Implement `getAll(keys)` by fetching front
  values once, calculating missing keys, calling `backCache.getAll(missedKeys)`
  once, and calling `frontCache.putAll(backValues)` once when the epoch is still
  current. Merge front and back maps without changing the existing `MutableMap`
  return type; return early without a back call when all keys are front hits.

- [x] **Step 4: Make clear and aliases share one implementation**

 Implement `clear()` as a mutation-gate operation: publish a new epoch, clear
 front, then synchronously acquire the backend-write lock and clear back. This
 waits for any timed-out async backend call that is still executing, so a late
 write cannot resurrect a value after `clear()` returns. Preserve visible
 front/back exceptions and the documented partial-state order. Make
 `clearAllCache()` delegate to `clear()` and make `getDeeply(key)` delegate to
 `get(key)`. Update Korean KDoc to describe standard semantics, peer-front
 propagation limits, and the alias relationship.

- [x] **Step 5: Preserve compound-operation round trips**

  Add private `frontContainsKey`/`frontGet` helpers and update
  `getAndRemove`/`getAndReplace` to use them, leaving #1355's cross-tier
  atomicity boundary unchanged. Their subsequent mutation still participates
  in the epoch/backend barrier, but they must not call the newly logical
  standard `containsKey`/`get` and add an extra back round trip.

- [x] **Step 6: Enforce the serialization boundary in configuration**

  Set `setStoreByValue(false)` on the default front configuration. In the
  `NearJCache` constructor reject a custom front configuration whose
  `isStoreByValue` is true. Do not add a bypass flag; providers that need
  serialized values require a separate filtered-copy design.

- [x] **Step 7: Run the focused tests GREEN**

Run:

```bash
./gradlew :bluetape4k-cache-core:test --tests '*NearJCacheContractTest' --no-configuration-cache
```

Expected: all contract tests pass. If a test fails, inspect the exact mock interaction before changing production code; do not weaken assertions.

## Task 3: Reconcile shared conformance fixtures and backend-specific expectations

**Files:**
- Modify: `cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/jcache/AbstractNearJCacheTest.kt` around the existing `clear` test.
- Modify: any exact callers found by `git grep -n 'clear().*getDeeply\|getDeeply.*clear' -- cache`.
- Modify: `cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt` if the factory test still expects a back value after `clear()`.

- [x] **Step 1: Replace front-only fixture assumptions**

Rename the existing front-only `clear` test to assert that the invoking near cache has no front or back mapping after `clear()`. Keep the explicit peer-front assertion only where it proves the documented non-guarantee: a peer may retain a stale front entry when listener propagation is unavailable. Add a standard `Cache`-typed fixture assertion if the deterministic contract test does not cover a real provider path.

- [x] **Step 2: Update factory/degraded backend tests**

For Hazelcast factory degraded mode, assert `cache.clear()` removes the local wrapper’s front and back value and that `cache.getDeeply(key)` is null afterward. Preserve the separate unsupported direct-listener test and do not claim peer propagation for the degraded factory.

- [x] **Step 3: Run cache-core and affected backend tests sequentially**

Run in this order, waiting for each command to finish before the next:

```bash
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
./gradlew :bluetape4k-cache-hazelcast:test --no-configuration-cache
./gradlew :bluetape4k-cache-lettuce:test --no-configuration-cache
./gradlew :bluetape4k-cache-redisson:test --no-configuration-cache
```

If a Testcontainers-backed command is unavailable or fails for environment reasons, capture the exact failure and run the provider-neutral contract tests plus compile checks; do not label the backend proof PASS.

- [x] **Step 4: Recheck timeout and peer propagation boundaries**

  Confirm a shared-back peer can retain a stale front entry after another near
  cache calls `clear()`, while the invoking wrapper's own front and back are
  empty. Confirm a timed-out asynchronous write cannot repopulate that back
  after the clear barrier. These are distinct guarantees and must not be
  conflated in the fixtures or documentation.

## Task 4: Align public documentation and capability matrix

**Files:**
- Modify: `cache/cache-core/README.md` in the JCache NearCache feature section.
- Modify: `cache/cache-core/README.ko.md` lines 101-156 and the class table.
- Modify: `docs/cache/near-cache-capability-matrix.md` JCache NearCache section.

- [x] **Step 1: Document identical standard read/clear semantics in both locales**

State that `NearJCache` is a JCache-compatible logical 2-tier cache: standard `get`/`containsKey`/`getAll` read front then back, back hits populate front, and `clear` clears the wrapper’s front and back. State that `getDeeply` and `clearAllCache` remain compatibility aliases and that compound operations are tracked separately by #1355.

- [x] **Step 2: Document peer propagation and degraded boundaries**

Explain that JCache `clear` does not promise listener-based peer-front invalidation; use `removeAll` when per-entry propagation is required. Keep Hazelcast factory degraded/unsupported rows precise and ensure English and Korean claims match.

- [x] **Step 3: Verify docs and link contracts**

Run:

```bash
git diff --check
rg -n 'front-only|Front Cache만|JCache 호환|JCache-compatible|clearAllCache|getDeeply' cache/cache-core/README.md cache/cache-core/README.ko.md docs/cache/near-cache-capability-matrix.md
```

Expected: no stale statement says standard `clear` or standard reads are front-only; `getDeeply`/`clearAllCache` are described as compatibility names, not required alternatives.

## Task 5: Final verification, lesson gate, and branch commit

- [x] **Step 1: Run proportional Kotlin verification**

Run:

```bash
./gradlew :bluetape4k-cache-core:test :bluetape4k-cache-core:detekt --no-configuration-cache
git diff --check
git status --short
```

Read all results. The cache-core test task, detekt, and diff check must pass before pre-PR review. No new warnings, deprecated imports, or `!!` may be introduced in touched Kotlin files.

- [x] **Step 2: Complete the Kotlin checklist and lesson decision**

Load `bluetape-kotlin-patterns/references/checklist.md` and the testing reference because Kotlin tests/fixtures are touched. Record KT-01 through KT-05 evidence, including public KDoc/README parity, mock contract coverage, and exact backend-test gaps. This change yielded reusable read-epoch/backend-generation and clear-barrier guidance, recorded in `docs/lessons/2026-08-12-issue-1363-nearjcache-contract.md`.

- [ ] **Step 3: Commit the converged implementation**

Use a Lore-formatted Korean commit message after final diff review:

```text
JCache 표준 read/clear 호출이 2-tier 캐시를 일관되게 관찰하도록 보장한다

Constraint: javax.cache.Cache 공개 호환성과 기존 alias 사용자를 유지해야 한다.
Rejected: compound operation 원자성 변경 | #1355 범위와 중복된다.
Confidence: high
Scope-risk: moderate
Directive: 새 compound API 수정은 #1355의 원자성 계약을 먼저 따른다.
Tested: cache-core focused/full tests, affected backend tests, detekt, git diff --check
Not-tested: 명시한 환경 제약으로 실행하지 못한 backend 증거를 실제 결과로 기록한다.
```

Expected DoD: exact local head SHA recorded, only approved files changed, P0=0/P1=0, and branch ready for independent pre-PR review.

## Task 6: PR delivery and closeout gates (separate approvals)

- [ ] **Step 1: Pre-PR review and PR creation**

Complete Type-A final checklist and independent 7-Tier review on the exact head. Create PR only after CG-10 and CG-11 authority are PASS, target `bluetape4k/bluetape4k-projects`, base `develop`, head `fix/nearcache-jcache-contract`, assign `debop`, mirror issue #1363 milestone/labels, and end the Korean PR body with `## DoD Status`.

- [ ] **Step 2: CI and merge-ready report**

Wait for required CI on the exact PR head, reread reviews/threads, and report `Required checks: X/Y; N/A: N; Blocked: 0`, exact PR/head, P0/P1 status, test evidence, and unchecked merge rows. Do not merge at this step.

- [ ] **Step 3: Fresh merge approval, merge, sync, cleanup**

After a fresh user approval tied to the exact merge-ready PR/head, merge using the approved strategy, verify the live merge SHA, sync local `develop`, and remove only the proven merged feature worktree/branch. Preserve any ambiguous or dirty state.

## Traceability

| Design requirement | Plan task |
| --- | --- |
| Standard `get` fallback/populate | Task 1, Task 2 |
| Standard `containsKey` back visibility | Task 1, Task 2 |
| Standard `getAll` merge/populate | Task 1, Task 2 |
| `clear` front/back and failure visibility | Task 1, Task 2, Task 3 |
| Epoch fencing and timeout-late barrier | Task 1, Task 2, Task 3 |
| Serialization trust boundary | Task 1, Task 2 |
| `getDeeply`/`clearAllCache` compatibility | Task 1, Task 2, Task 4 |
| Peer propagation/degraded boundaries | Task 3, Task 4 |
| Compound round-trip boundary / #1355 handoff | Task 1, Task 2 |
| README locale parity and capability matrix | Task 4 |
| Backend and static verification | Task 3, Task 5 |
| P0/P1 convergence, lesson, rollback, PR gates | Task 5, Task 6 |
