# Epic #1419 코루틴 계약 안정화 train Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 네 child issue를 strict linear stacked PR train으로 구현해 payload 보존, terminal exactly-once, cancellation 전파, bounded NATS backpressure를 결정적 테스트와 exact-head CI로 증명한다.

**Architecture:** `#1341 -> #1349 -> #1360 -> #1350` 순서의 branch containment를 유지한다. 각 child는 선행 PR의 required CI green과 blocker 0을 확인한 뒤에만 다음 branch와 PR을 만들며, merge 뒤에는 downstream을 최신 `develop` 위에 restack하고 모든 증거를 새 head에서 다시 수집한다. 구현은 기존 모듈과 공개 타입을 재사용하고, 동시성 계약은 atomic linearization point와 유한 queue로 고정한다.

**Tech Stack:** Kotlin 2.4, Java 25, Gradle 9.7.0, kotlinx.coroutines 1.11.0, kotlinx.atomicfu, Okio, JCache, jnats 2.26.1, JUnit 5, kotlinx-coroutines-test, MockK, Testcontainers, GitHub Actions.

---

## 1. 실행 계약

### 1.1 train과 승인 gate

| 순서 | Issue | head | PR base | 시작 조건 | PR 생성 조건 |
| --- | --- | --- | --- | --- | --- |
| 1 | #1341 | `test/1341-buffered-sink-payload` | `develop` | 이 계획 승인 | targeted/module 검증 green |
| 2 | #1349 | `fix/1349-buffered-collector-terminal-race` | `test/1341-buffered-sink-payload` | #1341 required CI green, blocker 0 | #1341 head를 포함한 exact-head 검증 green |
| 3 | #1360 | `fix/1360-suspend-jcache-cancellation` | `fix/1349-buffered-collector-terminal-race` | #1349 required CI green, blocker 0 | #1349 head를 포함한 exact-head 검증 green |
| 4 | #1350 | `feat/1350-nats-consumer-flow` | `fix/1360-suspend-jcache-cancellation` | #1360 required CI green, blocker 0 | #1360 head를 포함한 exact-head 검증 green |

- PR은 predecessor의 required CI가 성공하고 unresolved blocker가 0일 때만 순서대로 생성한다.
- PR 생성 전 `git rev-parse HEAD`, `git rev-parse <base>`, `git merge-base --is-ancestor <base> HEAD`를 receipt에 남긴다.
- merge는 별도 승인 gate다. exact head/base, checks, review·thread, mergeability, issue metadata, `## DoD Status`를 fresh-read한 뒤 사용자에게 승인받는다.
- auto-merge, tag, release, publish, branch 삭제, force cleanup은 수행하지 않는다.
- predecessor merge 뒤 downstream은 `--force-with-lease=<branch>:<observed-remote-sha>`로만 갱신한다. remote SHA가 달라지거나 conflict가 생기면 push하지 않고 `PENDING`으로 중단한다.

### 1.2 작업 방식과 공통 품질 gate

- 구현 전 `$bluetape-workflow`, `$bluetape-kotlin-patterns`, `$test-driven-development`를 적용한다.
- README/KDoc/CHANGELOG를 수정하는 단계는 `$bluetape-writer`의 한국어 자연스러움과 용어 일관성 검사를 적용한다.
- 각 테스트는 먼저 새 계약을 표현하도록 작성하고 실패 원인이 의도한 결함인지 확인한 뒤 최소 구현으로 green으로 만든다.
- Testcontainers 기반 NATS 검증은 다른 worktree/module의 container test와 병렬 실행하지 않는다.
- 각 child PR의 마지막에는 targeted test, module test, `detekt`, `git diff --check`를 새 head에서 실행한다.
- `repo-test-summary -- <command>`는 요약을 돕는 보조 증거일 뿐이다. CI 판정은 raw process exit와 GitHub check conclusion을 기준으로 한다.

## 2. Task 0 — 기준선과 train receipt 고정

**Files:**

- Read: `docs/superpowers/specs/2026-08-22-issue-1419-coroutine-contract-train-design.md`
- Read: `gradle.properties`
- Read: `gradle/libs.versions.toml`
- Read: `.github/workflows/ci.yml`
- Create per branch: `.bluetape` helper가 관리하는 run/lane evidence만 사용

- [ ] **Step 1: live issue와 branch 기준선을 읽는다.**

```bash
gh issue view 1419 --json number,title,state,assignees,labels,milestone,body
for issue in 1341 1349 1360 1350; do
  gh issue view "$issue" --json number,title,state,assignees,labels,milestone,body
done
git fetch origin develop
git status --short --branch
git rev-parse HEAD
git rev-parse origin/develop
```

Expected: Epic과 네 child가 `OPEN`, assignee `debop`, milestone `2.0.0`이며 현재 worktree가 clean이다. live 상태가 설계와 다르면 구현하지 않고 계획을 갱신한다.

- [ ] **Step 2: 모든 child의 baseline targeted test를 강제 재실행한다.**

```bash
repo-test-summary -- ./gradlew \
  :bluetape4k-okio:test \
  --tests 'io.bluetape4k.okio.coroutines.BufferedSuspendedSinkTest' \
  --rerun-tasks

repo-test-summary -- ./gradlew \
  :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.subject.BufferedResumableCollectorTest' \
  --rerun-tasks

repo-test-summary -- ./gradlew \
  :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.SuspendNearJCacheTest' \
  --rerun-tasks

repo-test-summary -- ./gradlew \
  :bluetape4k-nats:test \
  --tests 'io.bluetape4k.nats.client.ConsumerExtensionsTest' \
  --rerun-tasks \
  --no-configuration-cache
```

Expected: 각 command의 raw exit가 0이다. NATS baseline은 active Colima/Docker를 확인한 뒤 순차 실행한다. 실패하면 제품 회귀, 환경 실패, skipped coverage를 분리해 진단하고 train을 시작하지 않는다.

- [ ] **Step 3: #1341 branch containment를 증명한다.**

```bash
git merge-base --is-ancestor origin/develop HEAD
git log --oneline --decorate origin/develop..HEAD
```

Expected: 현재 branch는 `origin/develop`을 포함하며 승인된 설계 문서 commit만 추가로 가진다.

## 3. Task 1 — #1341 exact payload oracle

**Files:**

- Modify: `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/BufferedSuspendedSinkTest.kt`
- Modify only if the new oracle reveals a defect: `io/okio/src/main/kotlin/io/bluetape4k/okio/coroutines/RealBufferedSuspendedSink.kt`
- Update: `CHANGELOG.md`

- [ ] **Step 1: delegate 상태를 관찰할 수 있는 fake를 보강한다.**

`FakeSuspendedSink`에 `flushCount`, `closeCount`를 추가하고 delegate buffer는 읽지 않고 `snapshot()`으로 비교할 수 있게 유지한다.

```kotlin
private class FakeSuspendedSink : SuspendedSink {
    val buffer = Buffer()
    var flushCount = 0
        private set
    var closeCount = 0
        private set

    override suspend fun flush() { flushCount++ }
    override suspend fun close() { closeCount++ }
}
```

- [ ] **Step 2: 모든 overload의 서로 다른 sentinel과 exact expected bytes를 작성한다.**

기존 `(fakeSink.buffer.size > 0L)` assertion을 제거한다. 별도 `Buffer`에 동일 호출을 기록해 byte order와 endian을 Okio 자체 계약으로 계산하되, `SuspendedSource`별 `writeAll` 반환 count는 독립 assertion한다.

```kotlin
val expected = Buffer()
expected.write("bytes-A".encodeUtf8())
expected.write(byteArrayOf(0x41, 0x00, 0x7f))
expected.writeUtf8("utf8-한글")
// writeByte/Short/Int/Long/LE/decimal/hex와 source payload를 호출 순서대로 추가

val written = bufferedSink.writeAll(bufferOf("all-sentinel").asSuspended())
written shouldBeEqualTo "all-sentinel".encodeUtf8().size.toLong()
```

- [ ] **Step 3: flush 전후와 close 상태를 별도 test로 고정한다.**

```kotlin
bufferedSink.writeUtf8("tail")
fakeSink.buffer.snapshot() shouldBeEqualTo ByteString.EMPTY
bufferedSink.flush()
fakeSink.buffer.snapshot() shouldBeEqualTo "tail".encodeUtf8()
fakeSink.flushCount shouldBeEqualTo 1

bufferedSink.writeUtf8("-close")
bufferedSink.close()
fakeSink.closeCount shouldBeEqualTo 1
fakeSink.buffer.snapshot() shouldBeEqualTo "tail-close".encodeUtf8()
```

완전 segment가 delegate에 먼저 전달되는 case는 `SEGMENT_SIZE`보다 작은 tail과 큰 sentinel을 분리해 internal buffer와 delegate buffer의 기대값을 각각 비교한다.

- [ ] **Step 4: 회귀 oracle을 RED로 증명한다.**

테스트 작성 중 한 overload 호출의 expected byte를 의도적으로 다르게 두어 새 exact assertion이 실패함을 확인한 뒤 fixture를 바로 복원한다. production source를 no-op으로 편집하는 mutation은 남기지 않는다.

```bash
./gradlew :bluetape4k-okio:test \
  --tests 'io.bluetape4k.okio.coroutines.BufferedSuspendedSinkTest.all buffered write overloads preserve exact payload' \
  --rerun-tasks
```

Expected RED: byte 배열 mismatch. 복원 뒤 Expected GREEN: test 1개 성공.

- [ ] **Step 5: #1341 검증과 commit을 수행한다.**

```bash
repo-test-summary -- ./gradlew :bluetape4k-okio:test \
  --tests 'io.bluetape4k.okio.coroutines.BufferedSuspendedSinkTest' --rerun-tasks
./gradlew :bluetape4k-okio:detekt :bluetape4k-okio:build
git diff --check
git status --short
```

Expected: targeted test, detekt, module build가 성공하고 변경은 test와 필요한 경우 최소 production/KDoc, `CHANGELOG.md`에 한정된다.

Commit intent 예시:

```text
버퍼 쓰기 누락을 payload 단위 회귀로 탐지한다

Constraint: 모든 write overload와 flush close 경계를 독립적으로 증명한다
Confidence: high
Scope-risk: narrow
Tested: BufferedSuspendedSink targeted test와 okio module build
Not-tested: 전체 저장소 build
```

## 4. Task 2 — #1341 PR gate와 #1349 branch 생성

- [ ] **Step 1: #1341 PR 본문을 한국어로 작성한다.**

PR body는 `Closes #1341`, 실제 test 명령·결과, risk, stacked train 다음 head를 포함하고 마지막 section을 `## DoD Status`로 둔다.

- [ ] **Step 2: PR 생성 직전 exact head/base를 재확인한다.**

```bash
git fetch origin develop
head_sha=$(git rev-parse HEAD)
base_sha=$(git rev-parse origin/develop)
git merge-base --is-ancestor "$base_sha" "$head_sha"
git status --porcelain
```

Expected: clean, ancestor check 0. 이 계획에서 승인된 head/base는 `test/1341-buffered-sink-payload -> develop`이다.

- [ ] **Step 3: #1341 branch를 push하고 PR을 생성한 뒤 body를 read-back한다.**

```bash
git push -u origin test/1341-buffered-sink-payload
gh pr create --base develop --head test/1341-buffered-sink-payload --title '<한국어 제목>' --body-file <prepared-body>
gh pr view <number> --json headRefOid,baseRefName,body,mergeable,reviewDecision,statusCheckRollup
```

Expected: body가 비어 있지 않고 `## DoD Status`로 끝난다. required CI가 모두 terminal success이고 blocker 0이 되기 전 #1349 branch를 만들지 않는다.

- [ ] **Step 4: predecessor green 뒤 #1349 linked worktree를 만든다.**

```bash
CANONICAL_REPO=/Users/debop/work/bluetape4k/bluetape4k-projects
TRAIN_WORKTREE_ROOT=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees
cd "$CANONICAL_REPO"
git fetch origin test/1341-buffered-sink-payload
git worktree list --porcelain
git worktree add "$TRAIN_WORKTREE_ROOT/fix/1349-buffered-collector-terminal-race" \
  -b fix/1349-buffered-collector-terminal-race \
  origin/test/1341-buffered-sink-payload
git worktree list --porcelain
```

Expected: canonical repository root 아래 정확한 absolute path와 branch/head가 porcelain read-back에 한 번 나타나고 parent가 #1341 exact head다. linked worktree 안에 중첩 `.worktrees`를 만들지 않으며 기존 worktree나 dirty path는 제거하지 않는다. 이후 child worktree도 같은 canonical root와 absolute `TRAIN_WORKTREE_ROOT` 규칙을 사용한다.

## 5. Task 3 — #1349 terminal arbitration RED tests

**Files:**

- Modify: `bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/BufferedResumableCollectorTest.kt`
- Modify: `bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/BufferedResumableCollector.kt`
- Update: `CHANGELOG.md`

- [ ] **Step 1: 기존 순차 producer tests를 유지하고 결정적 race fixture를 추가한다.**

`CompletableDeferred`, `Channel`, `CoroutineStart.UNDISPATCHED`, `withTimeout`을 barrier로 사용한다. 정확성을 `delay`, `yield` 횟수, `Thread.sleep`에 의존하지 않는다.

```kotlin
private class RaceBarrier {
    val admitted = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
}
```

- [ ] **Step 2: capacity 1 full-buffer에서 terminal이 blocked producer를 깨우는 RED test를 작성한다.**

Case를 분리한다.

1. terminal CAS가 admission보다 먼저면 `next`가 `IllegalStateException`.
2. admission 뒤 terminal, offer 전이면 enqueue 없이 `IllegalStateException`.
3. offer 뒤 terminal이면 accepted value를 전달하고 `next`는 정상 반환.
4. admission 뒤 producer cancellation이면 `CancellationException`과 미전달.
5. offer 뒤 cancellation이면 accepted value 전달과 정상 `next` 반환.

```kotlin
withTimeout(2.seconds) {
    terminal.complete()
    blockedProducer.join()
    drainJob.join()
}
```

- [ ] **Step 3: first-terminal-wins와 drain ordering RED test를 작성한다.**

`complete()` 대 `error(expected)`, `complete()` 대 `error(null)`, `error(first)` 대 `error(second)`를 barrier에서 동시에 시작한다. 수집된 값, terminal count, 동일 cause identity를 assertion한다.

```kotlin
actualValues shouldBeEqualTo acceptedValues
assertSame(firstError, thrown)
terminalObservations shouldBeEqualTo 1
```

- [ ] **Step 4: collector failure와 producer wake-up RED test를 작성한다.**

collector `emit`이 `CancellationException` 또는 ordinary exception을 던질 때 대기 producer가 bounded time 안에 깨고, collector 원인이 그대로 전파되며 active admission이 누수되지 않는지 확인한다.

- [ ] **Step 5: 현재 구현에서 의도한 failure를 확인한다.**

```bash
./gradlew :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.subject.BufferedResumableCollectorTest' \
  --rerun-tasks
```

Expected RED: 기존 `done/error` 분리와 terminal wake-up 부재 때문에 최소 한 race oracle이 timeout, 값 손실 또는 잘못된 terminal cause로 실패한다. 실패가 barrier fixture 오류면 구현하지 말고 test를 먼저 고친다.

## 6. Task 4 — #1349 atomic state machine 구현

- [ ] **Step 1: terminal kind, cause, active admission count를 단일 immutable state로 만든다.**

method-local atomicfu를 만들지 않고 class property로 둔다.

```kotlin
private sealed interface Terminal {
    data object Open : Terminal
    data object Complete : Terminal
    data class Error(val cause: Throwable?) : Terminal
    data class Cancelled(val cause: Throwable) : Terminal
}

private data class State(
    val terminal: Terminal,
    val activeAdmissions: Int,
)

private val state = atomic(State(Terminal.Open, 0))
```

상태 표현은 실제 atomicfu/JVM 성능을 고려해 내부 class 또는 packed primitive로 바꿀 수 있으나, terminal과 count가 서로 다른 atomics로 갈라지면 안 된다.

- [ ] **Step 2: admission CAS와 정확히 한 번 decrement를 구현한다.**

```kotlin
suspend fun next(value: T) {
    admitOrThrow()
    try {
        producerMutex.withLock {
            awaitCapacityOrTerminal()
            if (!queue.offer(value)) error("capacity signal violated")
            signalValueReady()
        }
    } finally {
        releaseAdmission()
    }
}
```

- `queue.offer`가 acceptance linearization point다.
- offer 전 cancellation은 그대로 재전파하고 enqueue하지 않는다.
- offer 성공 뒤 cancellation point를 두지 않는다.
- `finally` 한 곳에서 count를 정확히 한 번 감소시킨다.
- `releaseAdmission()`이 terminal state의 마지막 count를 `1 -> 0`으로 바꾸면 `valueReady`를 다시 깨운다. terminal이 먼저 깨운 drain이 `queue empty + activeAdmissions > 0`에서 재대기한 경우에도 마지막 producer 반환 뒤 종료할 수 있어야 한다.

- [ ] **Step 3: non-suspending terminal CAS와 양쪽 waiter wake-up을 구현한다.**

```kotlin
fun complete() = terminate(Terminal.Complete)
fun error(ex: Throwable?) = terminate(Terminal.Error(ex))

private fun terminate(terminal: Terminal) {
    if (trySetFirstTerminal(terminal)) {
        valueReady.resume()
        resume() // capacity/producers
    }
}
```

첫 terminal만 state를 바꾼다. terminal 이후 새 producer는 즉시 거부한다. active admission이 0이고 queue가 빈 뒤에만 drain을 종료한다.

- [ ] **Step 4: drain cancellation과 error precedence를 고정한다.**

collector failure는 `Cancelled(cause)`를 선점할 수 있는 open state에서 기록하고 producer를 깨운 뒤 같은 throwable을 drain 호출자에게 다시 던진다. producer에는 `CancellationException("Collector drain failed", cause)`로 전달하되 cause identity를 보존한다. collector cause 자체가 `CancellationException`이면 변환하지 않고 그대로 유지한다. 이미 first terminal이 있으면 terminal kind를 덮어쓰지 않되 collector 자신의 cancellation은 호출자에게 유지한다.

barrier test는 terminal wake-up 뒤 drain이 active admission을 기다리게 한 다음 마지막 producer가 `releaseAdmission()`을 수행했을 때 bounded time 안에 drain이 끝나는지 별도로 검증한다.

- [ ] **Step 5: KDoc을 새 concurrent contract로 교체한다.**

기존 “모든 producer 후 terminal 호출” 제약을 제거하고 admission, offer linearization, first-terminal-wins, buffered-before-error, cancellation precedence를 한국어 KDoc으로 명시한다.

- [ ] **Step 6: targeted test와 stress 반복을 green으로 만든다.**

```bash
repo-test-summary -- ./gradlew :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.subject.BufferedResumableCollectorTest' \
  --rerun-tasks

for run in 1 2 3; do
  ./gradlew :bluetape4k-coroutines:test \
    --tests 'io.bluetape4k.coroutines.flow.extensions.subject.BufferedResumableCollectorTest' \
    --rerun-tasks || exit 1
done
```

Expected: 세 번 모두 raw exit 0, timeout 0, accepted set와 collected set 일치.

- [ ] **Step 7: module gate와 Lore commit을 수행한다.**

```bash
./gradlew :bluetape4k-coroutines:detekt :bluetape4k-coroutines:build
git diff --check
```

Commit intent: `종료 경합에서도 수락된 값을 잃지 않게 한다`.

## 7. Task 5 — #1349 PR gate와 #1360 branch 생성

- [ ] #1349 PR body를 한국어로 작성하고 `Closes #1349`, 상태 전이 표, stress evidence, 잔여 risk, `## DoD Status`를 포함한다.
- [ ] `fix/1349-buffered-collector-terminal-race -> test/1341-buffered-sink-payload` exact base/head를 검증해 push·PR 생성 후 body를 read-back한다.
- [ ] #1349 required CI green과 blocker 0을 확인한 뒤에만 `fix/1360-suspend-jcache-cancellation` worktree를 #1349 exact head에서 만든다.

## 8. Task 6 — #1360 deterministic lifecycle RED tests

**Files:**

- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCacheTest.kt`
- Modify: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCacheBackFirstContractTest.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt`
- Modify: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt`
- Update: `CHANGELOG.md`

- [ ] **Step 1: listener test를 `runTest`와 injected scope로 전환한다.**

```kotlin
@Test
fun `created batch를 순서대로 복사해 반영한다`() = runTest {
    val listener = SuspendJCacheEntryEventListener.forTest(targetCache, backgroundScope)
    listener.onCreated(events)
    advanceUntilIdle()
    coVerify(exactly = 1) { targetCache.putAll(expected) }
}
```

`junit-platform.properties`가 class 간 병렬 실행을 허용하므로 각 test는 독립 injected scope/listener를 만들고 `finally` 또는 fixture teardown에서 닫는다. 전역 test scope나 다른 test의 scheduler를 공유하지 않는다.

public one-arg constructor는 유지하고 test는 internal two-arg constructor 또는 internal factory를 사용한다.

- [ ] **Step 2: 네 callback의 batch·immutable copy·closed admission tests를 작성한다.**

- created/updated는 ordered map 사본으로 `putAll`.
- removed/expired는 ordered key set 사본으로 `removeAll`.
- callback 반환 직후 원본 mutable iterable/event를 바꿔도 target에는 동기 복사본이 전달된다.
- close 뒤 callback은 target을 호출하지 않는다.
- close와 launch가 교차해도 cancelled scope의 job body가 target을 호출하지 않는다.

close/launch race는 두 barrier case로 나눈다. (a) child body 진입 전 barrier에서 `close()`로 gate를 먼저 선점한 뒤 release해 target 미호출을 확인한다. (b) target의 cooperative suspend 지점 진입을 `CompletableDeferred`로 확인한 뒤 `close()`를 호출해 동일 child가 `CancellationException`으로 종료되는지 확인한다. `advanceUntilIdle()`만으로 두 순서를 추정하지 않는다.

- [ ] **Step 3: cancellation과 ordinary failure를 분리하는 RED tests를 작성한다.**

listener callback은 non-suspend JCache API이므로 child의 `CancellationException`을 callback 호출자에게 직접 던진다고 주장하지 않는다. injected scope의 `CoroutineExceptionHandler` 또는 명시적 child job 상태로 cancellation이 ordinary failure log 경로에 흡수되지 않음을 관찰한다. raw key/value/source가 log message에 포함되지 않는지는 appender 또는 logger seam으로 확인한다. ordinary backend failure는 callback thread로 던지지 않고 error log로 남는다.

- [ ] **Step 4: `SuspendNearJCache.clearAll/close` cancellation RED tests를 작성한다.**

```kotlin
coEvery { backCache.clear() } throws CancellationException("cancel-clear")
assertFailsWith<CancellationException> { nearCache.clearAll() }

coEvery { frontCache.close() } throws CancellationException("cancel-close")
assertFailsWith<CancellationException> { nearCache.close() }
```

ordinary exception test는 기존에 없으므로 새로 추가한다. `clearAll()`의 `backCache.clear()`와 `close()`의 `frontCache.close()`에 각각 ordinary `Exception`을 주입하고 호출자가 정상 반환하며 error/debug log만 남는지 확인한다.
기존 mutation cancellation pattern이 있는 `SuspendNearJCacheBackFirstContractTest`에 cancellation 분류를 두고, 정상 clear/close semantics는 `SuspendNearJCacheTest`에 유지한다.

- [ ] **Step 5: bounded burst characterization을 추가한다.**

유한 개수(예: 1,000)의 callback을 barrier에 대기시킨 뒤 `close()`가 join 없이 반환하고 scheduler를 진행했을 때 모든 cooperative job이 취소되는지 확인한다. wall-clock threshold를 PASS 조건으로 쓰지 않고 측정값은 characterization으로만 기록한다. 이 test는 fan-out 상한을 주장하지 않는다.

별도 idempotency test는 `close(); close()` 뒤 scope cancel observation이 한 번뿐이고 post-close callback이 target을 호출하지 않음을 검증한다. 이미 target suspend 지점에 진입한 단일 callback의 cooperative cancellation은 burst test와 분리한다.

- [ ] **Step 6: 현재 구현에서 RED를 확인한다.**

```bash
./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.SuspendNearJCacheTest' \
  --rerun-tasks
```

Expected RED: `Thread.sleep` 제거를 위한 scope seam 부재, cancellation swallow, close admission race 중 하나 이상이 실패한다.

## 9. Task 7 — #1360 cancellation·close 구현

- [ ] **Step 1: one-arg ABI를 보존하는 internal scope seam과 closed gate를 추가한다.**

```kotlin
class SuspendJCacheEntryEventListener<K : Any, V : Any> private constructor(
    private val targetCache: SuspendJCache<K, V>,
    private val scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) : /* interfaces */ {
    constructor(targetCache: SuspendJCache<K, V>) :
        this(targetCache, CoroutineScope(SupervisorJob() + Dispatchers.IO), Unit)

    companion object {
        @JvmSynthetic
        internal fun <K : Any, V : Any> forTest(
            targetCache: SuspendJCache<K, V>,
            scope: CoroutineScope,
        ) = SuspendJCacheEntryEventListener(targetCache, scope, Unit)
    }
}
```

실제 구현은 primary constructor shape를 바꾸지 않거나 JVM signature read-back으로 `(SuspendJCache)` public constructor가 그대로인지 검증한다. `closed.compareAndSet(false, true)`가 close linearization point다.

- [ ] **Step 2: 이벤트를 callback thread에서 불변 이벤트 사본으로 복사한다.**

```kotlin
val eventCopies = events.map { EventCopy(it.key, it.value) }
if (closed.value || targetCache.isClosed()) return
scope.launch { /* eventCopies only */ }
```

log에는 event type, count, sanitized cache identifier만 남기고 key/value/source는 쓰지 않는다. `SuspendJCache`에 공개 name 계약이 없으므로 API를 넓히지 않고 target cache의 class name을 ASCII 허용 문자 `[A-Za-z0-9._$-]`로 제한하고 128자로 자른 identifier를 사용한다. raw event trace는 제거한다. secret 형태의 key/value/source, CR/LF·제어문자, 과도하게 긴 문자열이 어느 log level에도 남지 않는 appender test를 추가한다.

- [ ] **Step 3: cancellation을 명시적으로 재전파하고 ordinary failure만 기록한다.**

```kotlin
try {
    targetCache.putAll(eventCopies.associate { it.key to it.value })
} catch (ce: CancellationException) {
    throw ce
} catch (e: Exception) {
    log.error(e) { "캐시 생성 이벤트 반영 실패. count=${eventCopies.size}" }
}
```

`CancellationException` 외 ordinary `Exception`만 기존 정책으로 log/무시한다. `OutOfMemoryError`, `LinkageError` 같은 fatal `Error`는 잡지 않고 재전파한다.

- [ ] **Step 4: `close()`를 idempotent/non-blocking으로 구현한다.**

첫 close가 gate를 닫고 scope를 cancel한다. 재호출은 no-op이다. active backend 호출의 join을 기다리지 않으며 cooperative cancellation만 보장한다.

- [ ] **Step 5: `SuspendNearJCache`의 `runCatching`을 typed catch로 교체한다.**

```kotlin
try {
    backCache.clear()
} catch (ce: CancellationException) {
    throw ce
} catch (e: Exception) {
    log.debug(e) { "Back cache clear 실패를 기존 정책에 따라 무시합니다." }
}
```

`close()`도 같은 분류를 적용한다. ordinary failure를 새 public result나 exception으로 바꾸지 않고 fatal `Error`는 재전파한다.

- [ ] **Step 6: one-arg binary signature와 targeted tests를 검증한다.**

```bash
repo-test-summary -- ./gradlew :bluetape4k-cache-core:test \
  --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.SuspendNearJCacheTest' \
  --tests 'io.bluetape4k.cache.nearcache.jcache.SuspendNearJCacheBackFirstContractTest' \
  --rerun-tasks
./gradlew :bluetape4k-cache-core:apiCheck
```

`apiCheck` task가 없으면 `javap -p -v` 또는 저장소의 binary compatibility task로 public `(SuspendJCache)` constructor를 read-back한다. private 3-arg constructor는 비공개이고 `forTest` factory는 `ACC_SYNTHETIC`이며 Java/Kotlin published API dump에서 제외되는지 확인한다. 존재하지 않는 task를 PASS로 간주하지 않는다.

- [ ] **Step 7: fan-out follow-up issue를 #1360 merge 전에 등록한다.**

한국어 issue에 bounded admission/coalescing 목표, burst 측정치, scope, acceptance criteria를 기록하고 Epic #1419의 비범위임을 명시한다. assignee `debop`, milestone `2.0.0`, 관련 label을 적용한다. issue 생성 후 live read-back한다.

- [ ] **Step 8: module gate와 Lore commit을 수행한다.**

```bash
./gradlew :bluetape4k-cache-core:detekt :bluetape4k-cache-core:build
git diff --check
```

Commit intent: `캐시 취소를 실패 로그로 소실하지 않게 한다`.

## 10. Task 8 — #1360 PR gate와 #1350 branch 생성

- [ ] #1360 PR body에 `Closes #1360`, one-arg ABI evidence, deterministic test, follow-up issue, 잔여 fan-out risk, `## DoD Status`를 포함한다.
- [ ] `fix/1360-suspend-jcache-cancellation -> fix/1349-buffered-collector-terminal-race` exact base/head로 push·PR을 생성하고 read-back한다.
- [ ] #1360 required CI green과 blocker 0 뒤에만 `feat/1350-nats-consumer-flow` worktree를 #1360 exact head에서 만든다.

## 11. Task 9 — #1350 공개 API·validation unit RED tests

**Files:**

- Create: `infra/nats/src/main/kotlin/io/bluetape4k/nats/client/NatsConsumerFlow.kt`
- Create: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsConsumerFlowTest.kt`
- Modify: `infra/nats/build.gradle.kts`
- Modify: `infra/nats/README.md`
- Modify: `infra/nats/README.ko.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: 공개 signature compile fixture를 먼저 작성한다.**

```kotlin
val pushFlow: Flow<Message> = jetStream.consumeAsFlow(
    subject = "orders.created",
    capacity = 64,
    receiveTimeout = 1.seconds,
)
val pullFlow: Flow<Message> = consumerContext.consumeAsFlow(
    capacity = 64,
    receiveTimeout = 1.seconds,
)
```

- [ ] **Step 2: validation table을 parameterized RED test로 작성한다.**

| 입력 | 허용 |
| --- | --- |
| `capacity` 1, 1024 | yes |
| `capacity` 0, 1025 | no |
| `receiveTimeout` 100ms | yes |
| `receiveTimeout` 99ms | no |
| push pending messages 1, 65,536 | yes |
| push pending messages 0, 65,537 | no |
| push pending bytes 1, 64MiB | yes |
| push pending bytes 0, 64MiB+1 | no |
| pull `batchBytes > 0` | no — jnats 2.26.1은 byte mode에서 message count를 1,000,000으로 두므로 이 adapter의 count bound와 양립하지 않음 |
| pull batch size default/1..N | collection 시 `min(original, capacity + 1)`의 유한 effective batch로 정규화 |

invalid option은 subscription/consumer factory가 호출되기 전에 `IllegalArgumentException`이어야 한다.

- [ ] **Step 3: 실제 handle을 mock해 blocking receive 경계를 검증한다.**

새 JVM-visible test interface를 production에 추가하지 않는다. MockK로 `JetStreamSubscription`과 `IterableConsumer`의 `nextMessage(Duration)`를 stub하고 `CountDownLatch`/`CompletableDeferred`로 receive 시작, 반환, thread interruption, close 횟수를 관찰한다.

```kotlin
every { subscription.nextMessage(any<java.time.Duration>()) } answers {
    receiveStarted.countDown()
    try {
        awaitReleaseOrInterrupt()
    } finally {
        interrupted.set(Thread.currentThread().isInterrupted)
    }
}
```

production에서는 실제 handle method reference 또는 private lambda만 사용한다. API dump/`javap`로 새 public `consumeAsFlow`, `defaultNatsFlowPushOptions`, `NatsConsumerFlowException` 외 receiver seam 타입이 노출되지 않음을 확인한다.

- [ ] **Step 4: 현재 source에서 compile RED를 확인한다.**

```bash
./gradlew :bluetape4k-nats:test \
  --tests 'io.bluetape4k.nats.client.NatsConsumerFlowTest' \
  --rerun-tasks --no-configuration-cache
```

Expected RED: `consumeAsFlow`와 `NatsConsumerFlowException` unresolved reference.

## 12. Task 10 — #1350 push/pull cold Flow core 구현

- [ ] **Step 1: public API와 기본값을 구현한다.**

```kotlin
val defaultNatsFlowPushOptions: PushSubscribeOptions = PushSubscribeOptions.builder()
    .pendingMessageLimit(1_024)
    .pendingByteLimit(16L * 1024 * 1024)
    .build()

fun JetStream.consumeAsFlow(
    subject: String,
    options: PushSubscribeOptions = defaultNatsFlowPushOptions,
    capacity: Int = 64,
    receiveTimeout: kotlin.time.Duration = 1.seconds,
): Flow<Message>

fun ConsumerContext.consumeAsFlow(
    options: ConsumeOptions = ConsumeOptions.DEFAULT_CONSUME_OPTIONS,
    capacity: Int = 64,
    receiveTimeout: kotlin.time.Duration = 1.seconds,
): Flow<Message>
```

공개 KDoc에는 cold의 의미, same-instance concurrent collect 거부, manual ack, queue bound, timeout, cleanup, exception을 한국어로 설명한다.

- [ ] **Step 2: same Flow instance collection gate와 bounded channel을 구현한다.**

```kotlin
val collecting = AtomicBoolean(false) // consumeAsFlow 호출마다 새 gate
return channelFlow {
    check(collecting.compareAndSet(false, true)) {
        "같은 NATS Flow 인스턴스는 동시에 collect할 수 없습니다."
    }
    try {
        // one receiver coroutine; send provides adapter backpressure
    } finally {
        collecting.set(false)
    }
}.buffer(capacity)
```

gate는 top-level 전역이 아니라 각 `consumeAsFlow` 호출이 반환한 Flow instance에 하나씩 귀속한다. repo의 “atomicfu는 class property만” 규칙을 지키기 위해 여기서는 method-local `java.util.concurrent.atomic.AtomicBoolean`을 사용한다. 실제 구조에서 `channelFlow` 기본 buffer와 추가 `buffer`가 중복되지 않게 operator fusion을 test로 확인하고 단 하나의 capacity 경계를 사용한다. adapter가 동시에 보유할 수 있는 상한은 channel `capacity`와 receiver-held 1개를 합친 `capacity + 1`이다. push/pull client queue는 이 adapter 상한과 별도로 유한해야 한다.

- [ ] **Step 3: blocking receive를 interruptible IO로 감싼다.**

```kotlin
val message = runInterruptible(Dispatchers.IO) {
    receiver.receive(receiveTimeout.toJavaDuration())
}
```

`null`은 idle timeout이며 active 상태면 loop를 계속한다. stopped/finished/inactive면 정상 완료한다. busy polling과 무한 blocking은 금지한다.

- [ ] **Step 4: push lifecycle과 drop 검사 순서를 구현한다.**

collection 때 synchronous push subscription을 생성하고 pending limits를 read-back한다. drop baseline을 기록한 뒤 receive 전·후와 `finally`에서 `getDroppedCount()`를 확인한다. 증가분은 `NatsConsumerFlowException(droppedMessages, cause)`로 변환한다.

- [ ] **Step 5: pull lifecycle을 `IterableConsumer`로 구현한다.**

collection 때 `ConsumerContext.iterate(effectiveOptions)`로 handle을 만들고 `IterableConsumer.nextMessage(timeout)` 경계를 seam에 연결한다. callback `MessageConsumer`는 사용하지 않는다.

jnats 2.26.1 `ConsumeOptions.DEFAULT_CONSUME_OPTIONS`의 batch size는 500이고 re-pull threshold는 25%이므로 원본 options를 그대로 넘기지 않는다. `batchBytes > 0`은 count와 byte를 동시에 유한하게 제한할 수 없어 handle 생성 전에 거부한다. 그 외 field는 JSON builder copy로 보존하되 effective batch를 `min(options.batchSize, capacity + 1)`로 정규화한다. default 500도 같은 규칙으로 `capacity + 1`이 된다. threshold는 보존하며 jnats의 in-flight pull bound가 effective batch를 넘지 않는지 source read-back과 integration oracle로 확인한다.

```kotlin
require(options.batchBytes == 0L) { "pull batchBytes는 bounded Flow에서 지원하지 않습니다." }
val effectiveOptions = ConsumeOptions.builder()
    .json(options.toJson())
    .batchSize(minOf(options.batchSize, capacity + 1))
    .build()
```

`capacity=1`, default options, explicit oversized batch에서 server/client pending·in-flight가 effective batch 2를 넘지 않는지 검증한다. `Int.MAX_VALUE` batch와 `Long.MAX_VALUE` byte 입력도 allocation/handle 생성 전에 안전하게 거부 또는 정규화한다.

- [ ] **Step 6: cleanup과 failure precedence를 한 곳에 구현한다.**

우선순위는 다음과 같다.

1. collector cancellation
2. ordinary receive/collector failure
3. drop/read-back failure
4. cleanup failure

push subscription과 pull consumer는 자신이 생성한 handle만 `finally`에서 idempotent하게 닫는다. 관찰 가능한 push cleanup failure는 primary exception을 덮지 않고 suppressed로 보존한다. 취소 중 close는 `NonCancellable`에서 유한·non-blocking 호출만 수행한다.

jnats 2.26.1 `IterableConsumer.close()`는 내부 unsubscribe/heartbeat 예외를 삼키므로 pull cleanup failure를 adapter가 suppressed exception으로 관찰할 수 있다고 주장하지 않는다. pull close는 best-effort 계약으로 문서화하고, handle 획득 직후 baseline/read-back failure 때도 `finally`가 실행되는 unit test와 취소 후 server consumer/subscription 누수가 없는 integration test로 보완한다. internal close seam의 인위적 exception은 production에서 관찰 가능한 계약으로 승격하지 않는다.

- [ ] **Step 7: manual ack only를 코드와 KDoc에 고정한다.**

adapter는 `ack`, `nak`, `term`을 호출하지 않는다. downstream이 business 처리 성공 뒤 `Message.ack()`를 직접 호출한다.

README 예제와 integration test는 호출자 선택을 세 갈래로 고정한다: 성공은 `ack()`, 재시도 가능한 실패는 `nak()`와 redelivery, 재시도 불가능한 실패는 `term()`과 no-redelivery다. 아무 ack도 하지 않은 경우의 ack-wait redelivery도 별도로 유지한다.

- [ ] **Step 8: unit lifecycle tests를 green으로 만든다.**

test seam으로 다음을 결정적으로 검증한다.

- push/pull order
- exact capacity에서 producer suspend
- idle timeout 반복 후 정상 message
- stop/finish 정상 완료
- concurrent collect 거부와 종료 후 재수집 허용
- cancellation이 receive thread를 interrupt하고 handle을 정확히 한 번 close
- receive/drop/read-back/관찰 가능한 push cleanup 복합 실패 precedence와 suppressed cause
- pull close의 best-effort 수행 및 server-side handle 누수 부재
- drop 증가량과 `NatsConsumerFlowException.droppedMessages`

```bash
repo-test-summary -- ./gradlew :bluetape4k-nats:test \
  --tests 'io.bluetape4k.nats.client.NatsConsumerFlowTest' \
  --rerun-tasks --no-configuration-cache
```

## 13. Task 11 — #1350 NATS Testcontainers 통합 tests

**Files:**

- Create: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsConsumerFlowIntegrationTest.kt`
- Reuse: `infra/nats/src/test/kotlin/io/bluetape4k/nats/AbstractNatsTest.kt`
- Reuse helpers under: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/`

- [ ] **Step 1: push consumer order와 cancellation cleanup을 검증한다.**

고유 stream/subject/durable 이름을 test별 생성한다. 작은 capacity로 순서가 유지되고 `take(n)` 취소 뒤 subscription이 닫혀 새 active consumer가 남지 않는지 server/client state로 확인한다.

- [ ] **Step 2: pull consumer order와 idle timeout/normal completion을 검증한다.**

pull durable에 유한 message를 publish하고 `IterableConsumer` 기반 Flow가 순서대로 전달하는지 확인한다. idle timeout 자체는 completion이 아님을 검증한다.

- [ ] **Step 3: manual ack, redelivery, ack failure를 검증한다.**

- ack한 message는 ack wait 뒤 redelivery되지 않는다.
- ack하지 않은 message는 ack wait/max deliver 설정에 따라 redelivery된다.
- `nak()`한 message는 redelivery되고 `term()`한 message는 redelivery되지 않는다.
- connection/consumer 종료 뒤 ack failure는 jnats 원래 예외로 caller에게 관찰된다.
- adapter가 source-level auto ack하지 않았음을 server consumer info로 증명한다.

- [ ] **Step 4: client queue bound와 drop exception을 검증한다.**

작은 pending limit와 느린 collector로 overflow를 유도하고 dropped delta가 0보다 큰 `NatsConsumerFlowException`을 확인한다. timing 대신 server publish completion과 receiver barrier를 사용한다.

- [ ] **Step 5: Testcontainers suite를 순차 실행한다.**

```bash
colima status
docker context show
docker info
repo-test-summary -- ./gradlew :bluetape4k-nats:test \
  --tests 'io.bluetape4k.nats.client.NatsConsumerFlowIntegrationTest' \
  --rerun-tasks --no-configuration-cache --max-workers=1
```

Expected: raw exit 0, skipped test 0. Colima가 healthy인데 bind-mount `operation not supported`가 나면 VM을 재시작하지 않고 환경 실패로 분리 진단한다.

## 14. Task 12 — #1350 dependency·문서 계약

**Files:**

- Modify: `infra/nats/build.gradle.kts`
- Create: `infra/nats/src/test/resources/compat/issue-1350/settings.gradle.kts`
- Create: `infra/nats/src/test/resources/compat/issue-1350/build.gradle.kts`
- Create: `infra/nats/src/test/resources/compat/issue-1350/src/main/kotlin/NatsFlowPublishedConsumer.kt`
- Modify: `infra/nats/README.md`
- Modify: `infra/nats/README.ko.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: public Flow ABI에 맞게 coroutine dependency를 `api`로 바꾼다.**

`infra/nats/build.gradle.kts`:

```kotlin
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
compileOnly(libs.kotlinx.coroutines.reactor)
```

중복 전이 dependency가 실제로 필요한지 `dependencies`/POM read-back으로 확인한다. 중앙 catalog와 BOM version은 변경하지 않는다.

- [ ] **Step 2: published metadata를 검증한다.**

```bash
./gradlew :bluetape4k-nats:dependencies --configuration runtimeClasspath
./gradlew :bluetape4k-nats:generatePomFileForBluetape4kPublication \
  :bluetape4k-nats:generateMetadataFileForBluetape4kPublication \
  -PsnapshotVersion=-SNAPSHOT --no-configuration-cache
rg -n 'kotlinx-coroutines-core|bluetape4k-coroutines' \
  infra/nats/build/publications/bluetape4k/pom-default.xml \
  infra/nats/build/publications/bluetape4k/module.json
```

Expected: consumer runtime에 coroutines 1.11.0 계약이 노출되고 POM/module metadata가 publication validator를 통과한다. 이 검사는 metadata 구조 확인이며, 아래 독립 소비자 실행 검사를 대신하지 않는다.

- [ ] **Step 3: 게시 artifact만 사용하는 독립 소비자를 compile·실행한다.**

fixture는 root multi-project build에 include하지 않는 독립 Gradle project다. `build.gradle.kts`의 유일한 product 직접 의존성은 다음 한 줄이어야 하며 `kotlinx-coroutines-*`, `bluetape4k-coroutines`, `project(...)`를 직접 선언하면 실패다.

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-nats:2.0.0")
}
```

`NatsFlowPublishedConsumer.kt`는 JDK dynamic proxy로 `JetStream`/`JetStreamSubscription`과 `ConsumerContext`/`IterableConsumer`의 최소 receive·close 동작을 제공한다. `runBlocking`과 `withTimeoutOrNull`을 **전이된 coroutine API로 import**하고, push와 pull `consumeAsFlow`를 각각 collect해 receive 진입과 cancellation 후 close를 assertion한다. 단순 signature 참조, root project test classpath, source-set 직접 참조는 허용하지 않는다.

사용자 Maven local을 오염하지 않도록 전용 repository를 만들고 production dependency closure와 NATS artifact를 같은 좌표로 publish한 뒤, 별도 Gradle process로 fixture를 실행한다.

```bash
compat_repo="$PWD/infra/nats/build/compat/issue-1350-m2"
rm -rf "$compat_repo"

./gradlew \
  :bluetape4k-logging:publishBluetape4kPublicationToMavenLocal \
  :bluetape4k-virtualthread-api:publishBluetape4kPublicationToMavenLocal \
  :bluetape4k-core:publishBluetape4kPublicationToMavenLocal \
  :bluetape4k-io:publishBluetape4kPublicationToMavenLocal \
  :bluetape4k-coroutines:publishBluetape4kPublicationToMavenLocal \
  :bluetape4k-nats:publishBluetape4kPublicationToMavenLocal \
  -Dmaven.repo.local="$compat_repo" \
  -PsnapshotVersion= --no-configuration-cache

./gradlew \
  -p infra/nats/src/test/resources/compat/issue-1350 \
  run \
  -Dmaven.repo.local="$compat_repo" \
  -PcompatRepository="$compat_repo" \
  --no-configuration-cache
```

fixture repository는 `maven { url = uri(providers.gradleProperty("compatRepository").get()) }`만 local product source로 사용하고 Maven Central은 외부 dependency용으로만 둔다. 실행 전 dependency report에서 `bluetape4k-nats`만 first-level product dependency인지 확인한다. Expected: standalone compile와 `run` raw exit 0, push/pull 양쪽 `receiveEntered=true`, `closed=true`. coroutine dependency를 `compileOnly`로 되돌리면 fixture compile 또는 runtime resolution이 RED여야 한다. task name이나 artifact closure가 실제 Gradle model과 다르면 추측으로 PASS 처리하지 말고 `./gradlew tasks --all`과 생성 POM을 read-back해 명령을 수정한다.

- [ ] **Step 4: 영문·한국어 README와 CHANGELOG를 갱신한다.**

두 README에 같은 runnable example을 각 언어로 설명한다.

```kotlin
jetStream.consumeAsFlow("orders.created")
    .collect { message ->
        process(message.data)
        message.ack()
    }
```

반드시 다음 표와 절을 포함한다.

| 호출자 계약 | 문서 내용 |
| --- | --- |
| 메모리·in-flight 상한 | adapter message 보유 `capacity + 1`; pull jnats pending/in-flight `capacity + 1` 이하, 따라서 보수적 합계 `2 * (capacity + 1)` 이하; push는 `pendingMessageLimit + capacity + 1`; push pending byte 기본 16MiB는 client queue에만 적용되며 adapter-held payload bytes는 별도임; server `maxAckPending`은 caller가 consumer config에서 별도 설정하는 독립 상한 |
| cold와 소유권 | collect마다 새 handle 생성, replay 아님, 같은 Flow 동시 collect 거부, 별도 Flow가 같은 durable consumer를 쓰는 충돌은 caller/jnats 책임, adapter-owned subscription/consumer만 close하고 외부 `Connection`/`JetStream`/`ConsumerContext`는 닫지 않음 |
| manual ack | 성공 `ack()`, retryable failure `nak()`, non-retryable failure `term()`, 무응답 ack-wait redelivery 예제 |
| pull options | `batchBytes` 미지원, collection/handle 생성 전 `IllegalArgumentException`; batch size는 `capacity + 1` 이하로 정규화 |
| 예외 | cancellation > receive/collector failure > drop/read-back > observable push cleanup; pure drop의 cause는 `null`, read-back failure는 cause로 보존, cleanup은 primary를 덮지 않음 |

`NatsConsumerFlowException` KDoc과 README에는 `droppedMessages` catch 예제를 넣는다. `batchBytes=1`, `Long.MAX_VALUE`, oversized `batchSize`에서 factory가 호출되지 않는 test를 문서 acceptance와 연결한다. `CHANGELOG.md` Unreleased에는 #1349/#1360/#1350의 사용자 관찰 변경을 한국어 `추가`/`변경`/`버그 수정` 범주로 기록한다.

- [ ] **Step 5: 문서·API 용어를 검증한다.**

```bash
rg -n 'auto.?ack|자동 ack|compileOnly|동시 collect|NatsConsumerFlowException|droppedMessages' \
  infra/nats/README.md infra/nats/README.ko.md \
  infra/nats/src/main/kotlin/io/bluetape4k/nats/client/NatsConsumerFlow.kt CHANGELOG.md
git diff --check
```

Expected: 구현과 문서의 default/limit/ack/cleanup 표현이 일치하고 placeholder가 없다.

## 15. Task 13 — PR CI에서 NATS coverage를 실제 실행

**Files:**

- Modify: `.github/workflows/ci.yml`
- Modify: `.github/scripts/test-aggregate-kover-coverage.py`
- Inspect, modify only if parity requires: `.github/workflows/nightly-tests.yml`

- [ ] **Step 1: workflow contract RED test를 먼저 추가한다.**

`AggregateKoverCoverageTest`에 다음 string/structure contract를 추가한다.

- `changes.outputs.search-messaging` 존재
- filter에 `infra/elasticsearch/**`, `infra/nats/**` 포함
- `test-search-messaging`이 NATS test와 Kover report를 실행
- coverage manifest, expected module inventory, `coverage-report.needs`, `ci-status.needs`에 job 포함
- skipped `test-search-messaging`은 NATS coverage evidence로 계산하지 않음

```bash
python3 .github/scripts/test-aggregate-kover-coverage.py -v
```

Expected RED: 현재 `search-messaging` output/job이 없어 새 assertions가 실패한다.

- [ ] **Step 2: `changes` output과 path filter를 추가한다.**

```yaml
outputs:
  search-messaging: ${{ steps.filter.outputs.search-messaging }}
# ...
search-messaging:
  - 'infra/elasticsearch/**'
  - 'infra/nats/**'
```

- [ ] **Step 3: PR용 `test-search-messaging` job을 추가한다.**

nightly의 동명 matrix 구성을 재사용하되 PR CI에서는 Testcontainers를 순차 실행한다.

```yaml
test-search-messaging:
  name: Test / Search & Messaging
  runs-on: ubuntu-latest
  timeout-minutes: 20
  needs: [build, changes]
  if: ${{ needs.changes.outputs.search-messaging == 'true' || needs.changes.outputs.shared == 'true' || github.event_name == 'workflow_dispatch' }}
  steps:
    - uses: actions/checkout@v7
    - uses: actions/setup-java@v5.7.0
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: ${{ env.JAVA_DISTRIBUTION }}
    - uses: gradle/actions/setup-gradle@v6.3.0
      with:
        gradle-version: wrapper
        cache-read-only: true
    - name: Test search and messaging modules
      run: |
        ./gradlew :bluetape4k-elasticsearch:test --max-workers=1 --no-configuration-cache
        ./gradlew :bluetape4k-nats:test --max-workers=1 --no-configuration-cache
      env:
        GRADLE_OPTS: "-Dorg.gradle.daemon=false"
    - name: Generate Kover XML report
      if: always()
      run: |
        ./gradlew :bluetape4k-elasticsearch:koverXmlReport --max-workers=1 --no-configuration-cache
        ./gradlew :bluetape4k-nats:koverXmlReport --max-workers=1 --no-configuration-cache
      env:
        GRADLE_OPTS: "-Dorg.gradle.daemon=false"
```

NATS와 Elasticsearch의 Testcontainers/Kover 작업은 항상 별도 Gradle invocation과 `--max-workers=1`로 순차 실행한다.

- [ ] **Step 4: coverage fail-closed chain을 연결한다.**

- `coverage-report.needs`와 expected jobs manifest에 `test-search-messaging` 추가
- 성공 시 expected modules에 `infra/elasticsearch`, `infra/nats` 추가
- `ci-status.needs`에 `test-search-messaging` 추가
- raw test/Kover artifacts의 이름을 job 전용으로 추가

`ci-status`의 기존 “skipped jobs treated as success” 문구는 전체 workflow 결론일 뿐 NATS coverage 증거가 아님을 유지한다. #1350 PR DoD는 `test-search-messaging=success`와 NATS report 존재를 별도 요구한다.

- [ ] **Step 5: nightly parity를 확인한다.**

nightly는 이미 `search-messaging`에서 `:bluetape4k-nats:test`와 Kover를 실행하고 expected inventory에 `infra/nats`를 포함한다. PR CI 변경으로 명칭·script contract가 어긋날 때만 최소 수정한다.

- [ ] **Step 6: workflow tests와 YAML parse를 green으로 만든다.**

```bash
python3 .github/scripts/test-aggregate-kover-coverage.py -v
ruby -e 'require "yaml"; YAML.load_file(".github/workflows/ci.yml", aliases: true)'
git diff --check
```

Expected: Python tests 모두 성공, YAML parse 성공, `infra/nats/**` fixture가 `search-messaging=true`로 매핑된다.

## 16. Task 14 — #1350 전체 module gate와 commit

- [ ] **Step 1: targeted와 module tests를 순차 실행한다.**

```bash
repo-test-summary -- ./gradlew :bluetape4k-nats:test \
  --tests 'io.bluetape4k.nats.client.NatsConsumerFlowTest' \
  --tests 'io.bluetape4k.nats.client.NatsConsumerFlowIntegrationTest' \
  --rerun-tasks --no-configuration-cache --max-workers=1

repo-test-summary -- ./gradlew :bluetape4k-nats:test \
  --rerun-tasks --no-configuration-cache --max-workers=1
```

- [ ] **Step 2: static/publication gates를 실행한다.**

```bash
./gradlew :bluetape4k-nats:detekt :bluetape4k-nats:build --no-configuration-cache
ruby scripts/publication/publication_pom_audit_test.rb
ruby scripts/publication/publication_module_metadata_audit_test.rb
python3 .github/scripts/test-aggregate-kover-coverage.py -v
git diff --check
```

- [ ] **Step 3: 검증 결과를 기능과 CI commit으로 분리한다.**

권장 commit 1 intent: `NATS 수집을 유한한 취소 경계 안에 둔다`.

권장 commit 2 intent: `NATS 변경이 PR 검증을 건너뛰지 않게 한다`.

각 commit은 Lore trailers에 실제 `Tested`와 남은 `Not-tested`를 기록한다. CI commit이 기능 commit을 전제로 하므로 순서를 바꾸지 않는다.

## 17. Task 15 — #1350 PR 생성과 exact-head CI gate

- [ ] **Step 1: branch/base containment과 문서 상태를 확인한다.**

```bash
git fetch origin fix/1360-suspend-jcache-cancellation
git merge-base --is-ancestor origin/fix/1360-suspend-jcache-cancellation HEAD
git status --porcelain
git diff --check origin/fix/1360-suspend-jcache-cancellation...HEAD
```

- [ ] **Step 2: PR을 생성하고 metadata/body를 read-back한다.**

PR은 `feat/1350-nats-consumer-flow -> fix/1360-suspend-jcache-cancellation`, `Closes #1350`, 한국어 title/body, issue labels/milestone mirror, assignee `debop`, 마지막 `## DoD Status`를 사용한다.

- [ ] **Step 3: exact head에서 실제 NATS job을 확인한다.**

```bash
pr_json=$(gh pr view <number> --json headRefOid,baseRefName,mergeable,reviewDecision,statusCheckRollup,body)
head_sha=$(printf '%s' "$pr_json" | jq -r .headRefOid)
gh pr checks <number> --watch
gh run list --workflow CI --commit "$head_sha" --json databaseId,headSha,createdAt,status,conclusion,event
run_id=$(gh run list --workflow CI --commit "$head_sha" --json databaseId,headSha,createdAt,status,conclusion,event \
  --jq 'map(select(.event == "pull_request")) | sort_by(.createdAt) | last | .databaseId')
test -n "$run_id"
gh run view "$run_id" --json headSha,createdAt,status,conclusion,jobs
gh pr view <number> --json headRefOid,baseRefName,mergeable,reviewDecision,statusCheckRollup,body
```

Expected:

- run `headSha`가 PR `headRefOid`와 동일
- 선택한 run이 해당 head의 최신 `pull_request` CI run이며 terminal 상태
- `Test / Search & Messaging` conclusion `SUCCESS`이며 skipped가 아님
- raw NATS test process 성공
- NATS Kover report가 non-empty이고 coverage aggregation 성공
- review blocker 0, unresolved thread 0

required check 이름 목록과 각 conclusion, run ID/head SHA/createdAt을 receipt에 기록한다. 이 exact-run 선택 절차를 모든 predecessor gate와 restack 후 재사용한다. 하나라도 pending/skipped/wrong-head이면 #1350은 `PENDING`이다.

## 18. Task 16 — merge 전 train 운영과 restack

각 PR마다 다음 순서를 반복한다. 이 task는 별도 merge 승인을 받은 뒤에만 실행한다.

- [ ] exact head/base/checks/reviews/threads/mergeability/metadata/DoD를 fresh-read한다.
- [ ] 사용자에게 해당 exact head SHA의 merge 승인을 받는다.
- [ ] 승인된 SHA만 `--match-head-commit`으로 merge한다. auto-merge를 켜지 않는다.
- [ ] `git fetch origin develop` 뒤 merge SHA가 fresh `origin/develop`에 포함됐는지 `git merge-base --is-ancestor <merge-sha> origin/develop`로 확인한다.
- [ ] 이미 열린 downstream PR은 `gh pr edit <number> --base develop` 또는 REST `PATCH /repos/{owner}/{repo}/pulls/{number}`로 base를 먼저 retarget하고, `gh pr view --json baseRefName,baseRefOid,headRefOid`로 read-back한다.
- [ ] downstream remote head를 관찰하고 `--force-with-lease=<branch>:<observed-remote-sha>`로 최신 `origin/develop` 위에 restack한다.
- [ ] downstream targeted/module/required CI를 새 head에서 전부 재실행한다.
- [ ] cleanup 전에 canonical root에서 `git worktree list --porcelain`을 구조적으로 읽어 exact path/branch/head를 식별한다. default, detached, locked, ambiguous, dirty 대상은 보존한다.
- [ ] 대상 head가 fresh `origin/develop`의 ancestor이고 `git cherry -v origin/develop <branch>`의 `+` line이 0인지 확인한다.
- [ ] 증명된 대상만 `git worktree remove <absolute-path>`로 no-force 제거하고, porcelain listing과 filesystem 양쪽에서 사라졌는지 재확인한다. branch는 삭제하지 않는다.

## 19. Rollback과 rerun 규칙

| 실패 | 즉시 조치 | 재개 조건 |
| --- | --- | --- |
| targeted RED 원인이 fixture 오류 | production edit 중단, test 수정 | 의도한 기존 결함으로 RED 확인 |
| predecessor CI 실패 | downstream 생성/PR 중단 | exact predecessor head green, blocker 0 |
| restack conflict | push 금지, worktree 보존 | conflict 원인 검토와 새 계획 승인 |
| remote head drift | force push 금지 | observed SHA 갱신과 변경 출처 확인 |
| Testcontainers 환경 실패 | 제품 실패와 분리, raw log 보존 | healthy Docker에서 fresh rerun |
| NATS job skipped/wrong-head | PASS 금지 | exact head의 non-skipped job success |
| coverage report empty/missing | PASS 금지 | Kover 생성·inventory·aggregation success |
| merge 후 회귀 | downstream 중단 | revert 또는 fix 방향 별도 승인 |

## 20. 위험 예측과 완화

| 위험 | 가능성/영향 | 예방·탐지 |
| --- | --- | --- |
| #1349 admission decrement 누락 | 중/높음 | 모든 exit를 단일 `finally`, barrier test에서 active 0 확인 |
| #1349 terminal과 offer 사이 ghost enqueue | 중/높음 | offer를 유일 acceptance point로 고정, 전후 cancellation test 분리 |
| #1360 public constructor ABI drift | 낮음/높음 | one-arg signature 유지, API/javap read-back |
| #1360 callback burst job 폭증 | 중/중 | bounded burst 측정과 close cancellation, 별도 follow-up issue |
| JCache raw key/value log 노출·주입 | 중/높음 | raw trace 제거, class identifier 허용 문자·128자 제한, secret/CRLF logger assertion |
| NATS `channelFlow` 이중 buffer | 중/높음 | capacity 경계 하나만 사용, receiver-held +1을 test/KDoc에 명시 |
| pull default batch 500의 선취·메모리 증가 | 높음/높음 | byte mode 거부, effective batch를 `capacity + 1` 이하로 정규화, pending/in-flight oracle |
| NATS receive cancellation이 thread를 못 깨움 | 중/높음 | `runInterruptible(IO)` seam에서 interruption test |
| drop read-back이 primary failure를 덮음 | 중/중 | failure precedence와 suppressed cause test |
| public Flow인데 coroutines runtime 누락 | 높음/높음 | `api` 전환, runtimeClasspath/POM/module metadata와 독립 published-consumer compile·collect 검증 |
| PR path filter가 NATS test를 skip | 현재 확정/높음 | `search-messaging` output/job/needs/coverage chain contract test |
| stacked PR base 변경 후 stale green 재사용 | 중/높음 | restack마다 exact head required CI fresh rerun |

## 21. 문서·API·운영 영향

- KDoc: `BufferedResumableCollector` concurrency/terminal 계약, `SuspendJCacheEntryEventListener` lifecycle, 두 `consumeAsFlow` overload와 exception을 한국어로 갱신한다.
- README: `infra/nats/README.md`, `infra/nats/README.ko.md`에 동일 API/ack/backpressure/cleanup 예제를 양 언어 계약에 맞춰 반영한다.
- CHANGELOG: root `CHANGELOG.md` Unreleased에 #1349/#1360/#1350의 사용자 관찰 변경을 한국어로 기록한다. #1341은 test-only이므로 사용자 동작 변경이 없음을 구분한다.
- AGENTS: module layout, build command, durable repository rule을 바꾸지 않으므로 변경하지 않는다.
- Nightly: 이미 NATS search-messaging과 coverage inventory가 있으므로 PR CI parity가 깨질 때만 수정한다.
- 공개 API/ABI: #1350은 additive API와 exception, coroutine runtime dependency exposure다. #1360 public one-arg constructor와 기존 cache semantics는 유지한다.

## 22. 독립 계획 리뷰 결과

| 관점 | 최종 판정 | 계획에 고정한 핵심 근거 |
| --- | --- | --- |
| 성능·backpressure | P0=0, P1=0 | terminal drain의 admission 1→0 wake-up, pull batch `capacity + 1` 정규화, push/pull 총 보유 상한 |
| 보안·failure hygiene | P0=0, P1=0 | pull byte mode 선거부, JCache log 식별자 sanitize, fatal `Throwable` 비포획, cleanup failure 우선순위 |
| 안정성·race | P0=0, P1=0 | ordinary clear/close 실패, close idempotency, active callback cancellation, close/launch 두 barrier |
| 운영·stacked train | P0=0, P1=0 | exact base/head, 선행 CI gate, retarget 후 restack, raw run 선택, 순차 Testcontainers, 무강제 cleanup 증명 |
| 개발자·API | P0=0, P1=0 | 공개/내부 seam 경계, complete CI job chain, `batchBytes` rejection KDoc/test, API read-back |
| 호출자·사용자 | P0=0, P1=0 | cold/소유권, ack/nak/term, 총 backpressure와 `maxAckPending`, 예외 mapping, 독립 published-consumer compile·collect fixture |

초기 리뷰에서 발견된 모든 P1은 본 계획에 반영한 뒤 같은 관점으로 재검토했다. P2 잔여 관찰 항목은 #1349 state reference allocation, #1360 callback fan-out의 더 강한 bound, #1350 drop read-back 비용과 pull close의 jnats best-effort 제약이다. 이들은 현재 issue 수용 기준과 안전 계약을 막지 않지만 구현 중 측정값이나 실패 증거가 나오면 train을 중단하고 별도 issue 또는 계획 수정으로 승격한다.

## 23. 최종 DoD

### 계획 gate

- [x] 이 구현 계획이 6개 독립 관점에서 P0=0, P1=0이다.
- [ ] 사용자 구현 계획 승인을 받는다.

### #1341

- [ ] 모든 write overload exact payload, `writeAll` count, flush/close 상태가 green이다.
- [ ] #1341 exact head required CI green, blocker 0, PR body read-back 완료다.

### #1349

- [ ] accepted value 손실·교착 없이 first-terminal-wins와 cancellation 경계를 결정적으로 증명한다.
- [ ] targeted stress 3회와 module build가 green이다.
- [ ] #1349 exact head required CI green, blocker 0이다.

### #1360

- [ ] listener/clearAll/close cancellation을 재전파하고 ordinary failure 정책을 유지한다.
- [ ] `Thread.sleep` 없는 lifecycle tests, 불변 이벤트 사본, sanitized log, one-arg ABI가 green이다.
- [ ] bounded fan-out follow-up issue를 live read-back했다.
- [ ] #1360 exact head required CI green, blocker 0이다.

### #1350

- [ ] push/pull cold Flow, bounded queue, drop detection, manual ack, cancellation cleanup이 unit/integration test에서 green이다.
- [ ] coroutines `api` dependency와 publication metadata가 일치한다.
- [ ] README/KDoc/CHANGELOG가 구현과 일치한다.
- [ ] exact head에서 non-skipped `Test / Search & Messaging`과 NATS coverage aggregation이 성공한다.

### train 종료

- [ ] 각 merge는 별도 exact-head 승인을 받는다.
- [ ] merge 뒤 downstream restack과 fresh CI가 완료된다.
- [ ] Epic #1419의 live child/DoD metadata가 실제 상태와 일치한다.
- [ ] 안전성이 증명된 worktree만 no-force cleanup하고 branch는 보존한다.

현재 stop condition은 계획 P0/P1=0 검토와 사용자 승인이다. 승인 전에는 Kotlin 구현, push, PR 생성, issue 생성, merge를 수행하지 않는다.
