# #1474 bounded admission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `test-driven-development` and the matching Kotlin/domain skills. Execute the checked steps in order and keep each RED/GREEN result as evidence.

**Goal:** `SuspendJCacheEntryEventListener`가 callback burst에서 listener별 in-flight job 수를 bounded non-blocking admission으로 제한하고, accepted event·overflow·close cancellation 계약을 결정적으로 검증하도록 만든다.

**Architecture:** 기존 callback별 child fan-out을 유지하되 listener 내부 `Semaphore` permit으로 admission을 선형화한다. `tryAcquire()` 성공 callback만 `scope.launch`를 만들고, 포화 callback은 queue/coalescing 없이 즉시 거부한다. child `finally`에서 permit을 반환하고 `close()`는 기존처럼 `scope.cancel()`만 호출한다.

**Tech Stack:** Kotlin 2.4, kotlinx.coroutines `Semaphore`, `CoroutineScope`, `SupervisorJob`, `kotlinx-coroutines-test`, JUnit 5, MockK, bluetape4k assertions, Gradle 9.7.0.

---

## 승인된 근거와 공통 경계

- Spec: `docs/superpowers/specs/2026-08-23-issue-1474-bounded-admission-design.md`
- Spec review: `docs/review/2026-08-23-issue-1474-bounded-admission-spec-review.md`
- Issue: [#1474](https://github.com/bluetape4k/bluetape4k-projects/issues/1474)
- Base: `origin/develop` at `f7c1cf1bb9da7ea2d6811f2833582ab0f434d15d`
- Spec branch: `feat/issue-1474-spec`
- Implementation branch/worktree: `feat/issue-1474-bounded-admission`
- Verification branch/worktree: `feat/issue-1474-verification`
- Public contract to preserve: `SuspendJCacheEntryEventListener(SuspendJCache<K, V>)`
  JVM descriptor, #1360 cancellation propagation, immutable event copy,
  event-kind invalidation semantics, sanitized logs, and
  `SuspendNearJCache` provider registration.
- Explicit exclusions: public capacity configuration, coalescing, global ordering,
  new dependency/module, Testcontainers changes, release/tag/publish/merge.

## File map

| File | Responsibility | Planned change |
| --- | --- | --- |
| `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt` | JCache callback admission, event copy, lifecycle, target-cache apply | Add private bounded `Semaphore`, internal test-only cap injection, submit helper, overflow log, Korean KDoc update; preserve public constructor and event operations. |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt` | Deterministic listener behavior proof | Replace unbounded 1,000-child characterization with bounded finite-burst tests; add overflow/redaction, permit-return, positive-cap validation, and close race cases; retain all #1360 tests. |
| `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCacheContractTest.kt` | Provider registration lifecycle contract | Read-only review and targeted execution; modify only if the existing assertion cannot prove registration remains unchanged. |
| `docs/superpowers/specs/2026-08-23-issue-1474-bounded-admission-design.md` | Approved design source | No implementation edits; reopen only if a P0/P1 or factual drift requires reapproval. |
| `docs/review/2026-08-23-issue-1474-bounded-admission-spec-review.md` | Six-lens spec review evidence | Append only if plan/implementation evidence changes the spec verdict. |

README, localized README, module registration, BOM/catalog, CI workflow, Kover
configuration, and Testcontainers fixtures are scoped N/A: no public API surface,
module, dependency, registration, or container behavior changes.

## Task 1: Prepare implementation worktree and record risk controls

**Files:** no tracked source change.

- [ ] **Step 1: Create the implementation worktree from the committed spec branch.**

  Run from the canonical repository worktree:

  ```bash
  git worktree add -b feat/issue-1474-bounded-admission \
    .worktrees/feat/issue-1474-bounded-admission feat/issue-1474-spec
  ```

  Expected: new linked worktree is on `feat/issue-1474-bounded-admission`, its
  HEAD equals the spec branch HEAD, and canonical `develop` remains clean.

- [ ] **Step 2: Re-read authority and record the concurrency risk checklist.**

  In the implementation worktree, re-read the applicable `AGENTS.md` files,
  `bluetape-workflow` common gates, `bluetape-kotlin-patterns`,
  `references/testing.md`, `kotlin-coroutines-skill`, and this plan. Record these
  controls before editing:

  1. `tryAcquire()` never suspends the JCache callback thread.
  2. A permit is released on normal completion, ordinary exception, and child
     cancellation; a lazy job that cannot start releases its permit explicitly.
  3. `CancellationException` is rethrown before broad `Exception` handling.
  4. `close()` calls `scope.cancel()` without join/drain.
  5. The public constructor and listener registration are unchanged.

  Expected: clean feature worktree, no unrelated dirty paths, and a written
  implementation risk note in the workflow evidence.

## Task 2: RED — add bounded admission and lifecycle tests before production edits

**Files:**

- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt`

- [ ] **Step 1: Add the positive-cap test seam expectation.**

  Add a test that calls:

  ```kotlin
  assertFailsWith<IllegalArgumentException> {
      SuspendJCacheEntryEventListener.forTest(
          targetCache = targetCache,
          scope = listenerScope,
          maxInFlightCallbacks = 0,
      )
  }
  ```

  Use `io.bluetape4k.assertions.assertFailsWith`, not JUnit or Kotlin test
  `assertFailsWith`. The expected RED failure is that `forTest` does not yet
  accept `maxInFlightCallbacks`.

- [ ] **Step 2: Replace the unbounded 1,000-child characterization with a bounded burst test.**

  Rename the test to
  ``bounded admission은 callback job 수와 accepted operation 수를 제한한다``
  and use this setup:

  ```kotlin
  val maxInFlight = 2
  val callbackCount = 8
  val startedCount = AtomicInteger()
  val allStarted = CompletableDeferred<Unit>()
  val release = CompletableDeferred<Unit>()
  coEvery { targetCache.putAll(any()) } coAnswers {
      if (startedCount.incrementAndGet() == maxInFlight) {
          allStarted.complete(Unit)
      }
      release.await()
  }
  val listener = SuspendJCacheEntryEventListener.forTest(
      targetCache = targetCache,
      scope = listenerScope,
      maxInFlightCallbacks = maxInFlight,
  )
  repeat(callbackCount) { index ->
      listener.onCreated(mutableListOf(mockEvent("burst-$index", "value", EventType.CREATED)))
  }
  runCurrent()
  allStarted.await()
  listenerJob.children.toList().size.shouldBeEqualTo(maxInFlight)
  release.complete(Unit)
  runCurrent()
  coVerify(exactly = maxInFlight) { targetCache.putAll(any()) }
  listener.close()
  ```

  The test must assert that only two callbacks are accepted and applied once,
  while six overflow callbacks do not create child jobs. The RED result must be
  the old implementation creating eight children and eight calls.

- [ ] **Step 3: Add a permit-return test for child cancellation.**

  Configure `maxInFlightCallbacks = 1`. The first `putAll` completes its
  `started` deferred and then completes exceptionally with
  `CancellationException`. After `runCurrent()` observes the first child
  cancellation, submit a second created event whose `putAll` completes a second
  deferred. Assert the second event runs and `coVerify(exactly = 2)` succeeds.
  This proves `finally { admission.release() }` rather than only checking a job
  status.

- [ ] **Step 4: Prove the lazy close-before-start permit release.**

  Create the listener with a cancelled `SupervisorJob` in its scope and
  `maxInFlightCallbacks = 1`. Submit two callbacks while capturing the
  sanitized overflow logger. Both lazy jobs must fail to start, neither backend
  operation may run, and the second submission must not emit an admission-full
  log. This deterministically exercises the `job.start() == false` branch and
  proves that the caller-side release does not strand the permit.

- [ ] **Step 5: Add ordinary-exception sibling isolation.**

  Configure `maxInFlightCallbacks = 2`. Make the first `putAll` throw a regular
  `IllegalStateException` and the second `putAll` complete a deferred. Submit
  both events before `runCurrent()`, then assert the second operation completes,
  the listener scope remains active, and the error log contains only the
  operation and sanitized cache id. This preserves the existing broad-exception
  isolation contract while the semaphore permit is returned.

- [ ] **Step 6: Add deterministic close and overflow-redaction cases.**

  - Use `maxInFlightCallbacks = 2`, eight `awaitCancellation()` callbacks, and
    a barrier that completes after two callbacks start. Assert the listener job
    has two children, call `close()` without joining in the production path, and
    then join only in the test to assert both children are cancelled.
  - Attach a `ListAppender<ILoggingEvent>` at `TRACE`, configure
    `maxInFlightCallbacks = 1`, block the first callback with
    `awaitCancellation()`, submit a second callback containing secret key,
    value, and source tokens, and assert the overflow log contains only
    operation/cache id/cap and none of those raw tokens.

  The RED result must either lack an overflow log or show the old unbounded
  second child; no timing sleeps are allowed.

- [ ] **Step 7: Run only the new tests to verify RED.**

  Run from the implementation worktree:

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
    --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
    --rerun-tasks
  ```

  Expected: compilation may fail until the test seam exists, or the new tests
  fail with the old unbounded behavior. Do not edit production code until the
  failure is attributable to the missing bounded admission behavior.

## Task 3: GREEN — implement the smallest bounded admission change

**Files:**

- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt`

- [ ] **Step 1: Add the private admission state and test-only constructor seam.**

  Preserve the public constructor delegation and add an internal-only parameter:

  ```kotlin
  private const val DEFAULT_MAX_IN_FLIGHT_CALLBACKS = 64

  private constructor(
      private val targetCache: SuspendJCache<K, V>,
      private val scope: CoroutineScope,
      private val maxInFlightCallbacks: Int,
      @Suppress("UNUSED_PARAMETER") marker: Unit,
  ) : ...

  constructor(targetCache: SuspendJCache<K, V>) : this(
      targetCache,
      CoroutineScope(SupervisorJob() + Dispatchers.IO),
      DEFAULT_MAX_IN_FLIGHT_CALLBACKS,
      Unit,
  )
  ```

  Validate `maxInFlightCallbacks > 0` with `require` before constructing the
  `Semaphore`, then create `private val admission = Semaphore(maxInFlightCallbacks)`.
  Import `io.bluetape4k.logging.debug`, `kotlinx.coroutines.CoroutineStart`, and
  `kotlinx.coroutines.sync.Semaphore`; add no dependency.

- [ ] **Step 2: Add the submit helper with explicit close and permit-race handling.**

  Replace the four repeated `scope.launch` blocks with one helper equivalent to:

  ```kotlin
  private fun submit(operation: String, block: suspend () -> Unit) {
      if (!shouldAcceptCallback()) return
      if (!admission.tryAcquire()) {
          log.debug {
              "Reject callback because admission is full. " +
                  "operation=$operation cache=$cacheIdentifier maxInFlight=$maxInFlightCallbacks"
          }
          return
      }
      if (!shouldAcceptCallback()) {
          admission.release()
          return
      }

      val job = scope.launch(start = CoroutineStart.LAZY) {
          try {
              if (!closed.get()) {
                  applyEvent(operation, block)
              }
          } finally {
              admission.release()
          }
      }
      if (!job.start()) {
          admission.release()
      }
  }
  ```

  `CoroutineStart.LAZY` plus `Job.start()` handles the close-before-start race:
  a job that never starts cannot execute `finally`, so the caller releases its
  permit when `start()` returns `false`. A started job releases exactly once in
  `finally`. Keep `CancellationException` handling inside the existing
  `applyEvent` unchanged.

- [ ] **Step 3: Route each callback through `submit` without changing snapshots.**

  Keep the synchronous copy and trace log exactly before admission:

  ```kotlin
  val eventCopies = events.map { EventCopy(it.key, it.value) }
  log.trace { "BackCache cache entry created. cache=$cacheIdentifier count=${eventCopies.size}" }
  submit("put all created cache entries") {
      targetCache.putAll(eventCopies.associate { it.key to it.value })
  }
  ```

  Apply the same shape to updated, removed, and expired. Removed/expired must
  keep `toCollection(LinkedHashSet())`; created/updated must keep `associate`.
  `shouldAcceptCallback()` remains the pre-admission `closed` and target-cache
  check, and no event payload is added to any log.

- [ ] **Step 4: Extend the internal test factory and Korean KDoc.**

  Add `maxInFlightCallbacks: Int = DEFAULT_MAX_IN_FLIGHT_CALLBACKS` to the
  `@JvmSynthetic internal forTest` factory only. Update the class KDoc to state
  that callback admission is non-blocking, the default cap is `64` in-flight
  callback jobs, overflow is rejected without an internal queue, and `close()`
  requests cancellation without waiting. Do not add a public configuration
  parameter or change the example registration code.

- [ ] **Step 5: Run the focused tests to verify GREEN.**

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
    --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
    --rerun-tasks
  ```

  Expected: every existing #1360 test plus the new bounded admission, permit
  return, close, and redaction tests pass. If the result flakes, stop and
  diagnose coroutine scheduling or mock synchronization before changing the
  assertions.

## Task 4: Verify provider registration and module behavior

**Files:** no expected source change; modify only if an existing assertion is
insufficient to prove the unchanged registration contract.

- [ ] **Step 1: Run listener and provider contract tests sequentially.**

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
    --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
    --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheContractTest' \
    --tests 'io.bluetape4k.cache.nearcache.jcache.SuspendNearJCacheBackFirstContractTest' \
    --rerun-tasks
  ```

  Expected: listener tests and registration/back-first contract tests pass with
  the same listener factory path and no registration diff.

- [ ] **Step 2: Run the complete affected module test before coverage.**

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:test --rerun-tasks
  ```

  Expected: `BUILD SUCCESSFUL`; any Testcontainers-backed task is kept
  sequential and diagnosed from raw output rather than treated as skipped
  success.

## Task 5: Static, API, and source-contract verification

**Files:** no expected source change.

- [ ] **Step 1: Run Kotlin static analysis and module build.**

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:detekt --rerun-tasks
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:build -x test --rerun-tasks
  ```

  Expected: both commands pass without new detekt findings or compilation
  warnings that hide a lifecycle problem.

- [ ] **Step 2: Prove the public constructor descriptor is unchanged.**

  ```bash
  jar_path=$(find cache/cache-core/build/libs -maxdepth 1 \
    -name 'bluetape4k-cache-core-*.jar' ! -name '*sources*' ! -name '*javadoc*' \
    ! -name '*test-fixtures*' \
    | head -1)
  test -n "$jar_path"
  javap -classpath "$jar_path" \
    io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListener \
    | rg '^  public .*SuspendJCacheEntryEventListener\(io\.bluetape4k\.cache\.jcache\.SuspendJCache<.*>\);$'
  ```

  Expected: exactly the one-argument public constructor remains present. Kotlin's
  synthetic `DefaultConstructorMarker` bridge is ignored by the exact signature
  filter; no public capacity constructor or public metrics type appears.

- [ ] **Step 3: Run source/documentation hygiene checks.**

  ```bash
  git diff --check
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
    cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt \
    cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt
  ```

  Use the installed path `/Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs`. Expected: diff check clean and terminology findings either zero or explicitly dispositioned in the final review. No README parity task is required because public usage/API is unchanged.

## Task 6: Performance/stability scan and final review

**Files:**

- Review: implementation source/test diff and approved spec/plan.
- Create if needed: `docs/review/2026-08-23-issue-1474-bounded-admission-code-review.md`

- [ ] **Step 1: Inspect the scoped diff and run the performance/stability checklist.**

  Confirm all of the following from source and tests:

  - no `runBlocking`, `GlobalScope`, blocking wait, or `Thread.sleep` was added;
  - `tryAcquire()` is the only admission wait point and never suspends;
  - max child jobs never exceed the injected cap;
  - lazy-start close race releases a permit exactly once;
  - ordinary exceptions do not cancel sibling children;
  - `CancellationException` remains visible to the child job;
  - no raw payload enters logs;
  - no public constructor, registration, module, dependency, or README drift.

- [ ] **Step 2: Run final targeted proof after any review repair.**

  ```bash
  repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
    --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
    --tests 'io.bluetape4k.cache.nearcache.jcache.NearJCacheContractTest' \
    --rerun-tasks
  git diff --check
  git status --short --branch
  ```

  Expected: all targeted tests pass, diff check is clean, and only the planned
  source/test/KDoc paths are dirty.

- [ ] **Step 3: Commit the implementation slice using Lore trailers.**

  ```bash
  git add cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt \
    cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt
  git commit -m "callback burst admission을 bounded non-blocking 정책으로 제한한다" \
    -m "#1474의 in-flight callback 상한과 overflow 거부를 구현해 listener fan-out을 제한한다." \
    -m "Constraint: JCache callback은 동기 API이고 #1360의 cancellation·ABI·registration 계약을 유지해야 한다.
Rejected: 내부 queue/coalescing | callback ordering과 cancellation 의미를 변경한다.
Confidence: high
Scope-risk: moderate
Directive: permit 선형화와 close race 검증을 유지하고 public capacity API를 추가하지 않는다.
Tested: focused listener tests, provider contract tests, full cache-core test, detekt, build, javap, diff check.
Not-tested: live CI and PR review remain pending."
  ```

## Task 7: Create the verification train slice

**Files:** no implementation change unless Task 6 review requires a narrowly
scoped doc/test repair.

- [ ] **Step 1: Create verification worktree from the exact implementation head.**

  ```bash
  git worktree add -b feat/issue-1474-verification \
    .worktrees/feat/issue-1474-verification feat/issue-1474-bounded-admission
  git -C .worktrees/feat/issue-1474-verification rev-parse HEAD
  git -C .worktrees/feat/issue-1474-bounded-admission rev-parse HEAD
  ```

  Expected: both SHAs match before verification edits; preserve both linked
  worktrees until exact-head delivery and cleanup authority is complete.

- [ ] **Step 2: Re-read current issue metadata and prepare the no-PR/PR boundary.**

  ```bash
  gh issue view 1474 --json number,title,state,assignees,milestone,labels,url
  git status --short --branch
  ```

  Keep PR creation pending until common gate CG-11 has fresh authority naming
  `bluetape4k/bluetape4k-projects`, base `develop`, and the exact head branch.
  Never merge or enable auto-merge from this plan.

## Task 8: Final DoD and rollback

- [ ] **Step 1: Map every spec criterion to fresh evidence.**

  The final report must include: cap/linearization/overflow proof; accepted
  call count and no duplication; redaction; close cancellation/no join; provider
  registration; public constructor `javap`; full module test; detekt/build;
  diff check; exact local head; P0/P1 verdict; and any P2 disposition.

- [ ] **Step 2: Roll back only by branch-local commit revert if a proof fails.**

  Do not reset or mutate `develop`. If implementation proof fails, retain the
  spec and review commits, revert only the implementation commit on
  `feat/issue-1474-bounded-admission`, repair the tests/design, and rerun the
  RED/GREEN sequence. If the approved semantics need coalescing or a public
  API, stop and return to spec approval instead of widening this train.

## Plan self-review

- [x] Every spec acceptance criterion maps to Tasks 2–6 and a final DoD row.
- [x] Task order is executable: worktree → RED → GREEN → module/contract tests →
  static/API proof → review/commit → verification train.
- [x] No implementation task depends on a later artifact.
- [x] Success, overflow, edge, cancellation, close race, exception, logging,
  provider registration, and ABI paths are explicit.
- [x] README/module/catalog/CI/Testcontainers hazards are explicitly N/A with
  scope evidence.
- [x] Rollback and reapproval conditions are explicit.
- [x] Placeholder scan is clean; commands use actual paths and task names.
