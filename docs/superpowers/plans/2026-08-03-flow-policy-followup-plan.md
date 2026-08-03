# Flow 정책 후속 작업 실행 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Issue #1300의 delay-error/overflow 후보를 새 public API 없이 계약 매트릭스, 호출자 근거, 결정적 회귀 테스트로 정리한다.

**Architecture:** 현재 concat, merge, onBackpressureDrop 구현은 변경하지 않는다. 표준 Kotlin Flow의 buffer/conflate 계약과 기존 custom operator의 fail-fast/cancellation 경계를 테스트로 고정하고, 양국어 README에서 Reactive Streams demand와의 차이 및 #1300 비목표를 연결한다.

**Tech Stack:** Kotlin 2.3, kotlinx.coroutines Flow, JUnit 5, kotlinx-coroutines-test, bluetape assertions, Gradle.

---

## 파일 구조

| 파일 | 책임 |
|---|---|
| docs/superpowers/plans/2026-08-03-flow-policy-followup-plan.md | 실행 순서와 명령, 예상 결과를 고정한다. |
| bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/FlowPolicyContractTest.kt | 기존 Flow 정책의 실패·취소·bounded buffer·conflate 계약을 결정적으로 검증한다. |
| bluetape4k/coroutines/README.md | 영어 사용자에게 #1300 matrix와 비목표를 연결한다. |
| bluetape4k/coroutines/README.ko.md | 한국어 사용자에게 같은 계약과 비목표를 설명한다. |
| docs/lessons/2026-08-03-issue-1300-flow-policy.md | 증거, 선택, 재개 조건, 검증 결과를 한국어로 기록한다. |

production Kotlin source, dependency catalog, module registration, benchmark source는 수정하지 않는다.

### Task 1: Baseline과 RED 경계 고정

**Files:**

- Test: bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/ConcatTest.kt
- Test: bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MergeFlowsTest.kt
- Test: bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/OnBackpressureDropTest.kt

- [x] **Step 1: 기존 contract test를 먼저 실행한다.**

Run:

~~~
./gradlew :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.ConcatTest' \
  --tests 'io.bluetape4k.coroutines.flow.extensions.MergeFlowsTest' \
  --tests 'io.bluetape4k.coroutines.flow.extensions.OnBackpressureDropTest' \
  --no-configuration-cache --console=plain
~~~

Expected: BUILD SUCCESSFUL; 기존 API의 정상 순서·도착 순서·drop 결과가 통과한다. 이 단계는 no-production-change tranche의 baseline이며, 새 API가 없으므로 compile RED를 의도하지 않는다.

- [x] **Step 2: 새 테스트 파일을 만들기 전에 대상 범위와 dirty state를 확인한다.**

Run:

~~~
git status --short --branch
git diff --check
rg -n --glob '!build/**' \
  'concatDelayError|mergeDelayError|flatMapDelayError|onBackpressureBuffer|onBackpressureLatest|bufferWhen|windowWhen|bufferWhile|windowWhile' \
  bluetape4k/coroutines/src/main bluetape4k/coroutines/src/test
~~~

Expected: 기존 spec/matrix commit 외에 unrelated change가 없고, production 호출자 검색 결과가 비어 있으며, 후보 이름은 문서 또는 의도적 비목표에서만 나온다.

### Task 2: 결정적 Flow policy 회귀 테스트 작성

**Files:**

- Create: bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/FlowPolicyContractTest.kt

- [x] **Step 1: 다음 테스트를 작성한다.**

~~~
package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class FlowPolicyContractTest: AbstractFlowTest() {

    @Test
    fun concatIsFailFastAndSkipsLaterSources() = runTest {
        val secondCollected = AtomicBoolean(false)
        val failure = IllegalStateException("first")

        val actual = assertFailsWith<IllegalStateException> {
            concat(
                flow {
                    emit(1)
                    throw failure
                },
                flow {
                    secondCollected.set(true)
                    emit(2)
                },
            ).toList()
        }

        actual::class shouldBeEqualTo failure::class
        actual.message shouldBeEqualTo failure.message
        secondCollected.get().shouldBeFalse()
    }

    @Test
    fun mergeFailureCancelsSiblingAndPreservesOriginalFailure() = runTest {
        val siblingCancelled = CompletableDeferred<Unit>()
        val failure = IllegalStateException("merge")

        val actual = assertFailsWith<IllegalStateException> {
            merge(
                flow {
                    try {
                        awaitCancellation()
                    } finally {
                        siblingCancelled.complete(Unit)
                    }
                },
                flow<Int> { throw failure },
            ).collect()
        }

        actual::class shouldBeEqualTo failure::class
        actual.message shouldBeEqualTo failure.message
        siblingCancelled.await()
    }

    @Test
    fun bufferSuspendKeepsAtMostOnePendingValue() = runTest {
        val releaseCollector = CompletableDeferred<Unit>()
        val thirdEmit = CompletableDeferred<Unit>()

        val job = launch {
            flow {
                emit(1)
                emit(2)
                emit(3)
                thirdEmit.complete(Unit)
            }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.SUSPEND)
                .collect {
                    if (it == 1) releaseCollector.await()
                }
        }

        runCurrent()
        thirdEmit.isCompleted.shouldBeFalse()
        releaseCollector.complete(Unit)
        job.join()
        thirdEmit.isCompleted.shouldBeTrue()
    }

    @Test
    fun conflateKeepsLatestWhileCollectorIsSuspended() = runTest {
        val releaseCollector = CompletableDeferred<Unit>()
        val result = mutableListOf<Int>()

        val job = launch {
            flowOf(1, 2, 3).conflate().collect {
                result += it
                if (it == 1) releaseCollector.await()
            }
        }

        runCurrent()
        releaseCollector.complete(Unit)
        job.join()
        result shouldBeEqualTo listOf(1, 3)
    }

    @Test
    fun callerCancellationIsNotConvertedToDataError() = runTest {
        val upstreamCancelled = AtomicBoolean(false)
        val job = launch {
            flow<Int> {
                try {
                    awaitCancellation()
                } finally {
                    upstreamCancelled.set(true)
                }
            }.collect()
        }

        runCurrent()
        job.cancelAndJoin()
        job.isCancelled.shouldBeTrue()
        upstreamCancelled.get().shouldBeTrue()
    }

    @Test
    fun flowCatchDoesNotCatchCallerCancellation() = runTest {
        var caught = false
        val job = launch {
            flow<Int> { awaitCancellation() }
                .catch { caught = true }
                .collect()
        }

        runCurrent()
        job.cancelAndJoin()
        caught.shouldBeFalse()
    }
}
~~~

The test intentionally exercises only existing public operators and standard Flow operators. thirdEmit proves that a capacity-one buffer suspends rather than silently growing; the conflate test proves the latest-value loss policy. The two cancellation tests make CancellationException preservation explicit.

- [x] **Step 2: Run the new test class.**

Run:

~~~
./gradlew :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.FlowPolicyContractTest' \
  --no-configuration-cache --console=plain
~~~

Expected: BUILD SUCCESSFUL with six passing tests. If a test fails, diagnose the existing contract or test synchronization first; do not add a new public operator to make the test pass.

- [x] **Step 3: Run the focused regression set together.**

Run:

~~~
./gradlew :bluetape4k-coroutines:test \
  --tests 'io.bluetape4k.coroutines.flow.extensions.FlowPolicyContractTest' \
  --tests 'io.bluetape4k.coroutines.flow.extensions.ConcatTest' \
  --tests 'io.bluetape4k.coroutines.flow.extensions.MergeFlowsTest' \
  --tests 'io.bluetape4k.coroutines.flow.extensions.OnBackpressureDropTest' \
  --no-configuration-cache --console=plain
~~~

Expected: all selected classes pass with zero failures and zero errors.

- [x] **Step 4: Commit the test-only contract lock.**

~~~
git add bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/FlowPolicyContractTest.kt
git commit -m 'Lock Flow policy contracts without API expansion' -m $'Constraint: #1300 requires deterministic failure, cancellation, and bounded-memory evidence before new policy APIs.\nRejected: delay-error and Reactive Streams overflow wrappers | no caller evidence and no Flow demand protocol.\nConfidence: high\nScope-risk: narrow\nDirective: Keep these tests characterization-only until a separately approved caller-backed API contract exists.\nTested: focused Flow policy and existing concat/merge/drop tests.\nNot-tested: full module check is run in the next verification task.'
~~~

### Task 3: README parity와 비목표 링크

**Files:**

- Modify: bluetape4k/coroutines/README.md in the existing Rx/Reactor-style parity section
- Modify: bluetape4k/coroutines/README.ko.md in the existing Rx/Reactor 스타일 대응 section

- [x] **Step 1: 영어 README에 다음 내용을 추가한다.**

~~~
Delay-error and explicit overflow families are tracked in [follow-up issue #1300](https://github.com/bluetape4k/bluetape4k-projects/issues/1300); the current contracts, caller evidence, and re-open conditions are recorded in the [Flow operator policy matrix](../../docs/flow-operator-policy-matrix.md).
~~~

- [x] **Step 2: 한국어 README에 같은 계약을 한국어로 추가한다.**

~~~
delay-error와 명시적 overflow 정책은 [후속 이슈 #1300](https://github.com/bluetape4k/bluetape4k-projects/issues/1300)에서 다루며, 현재 계약·호출자 근거·재개 조건은 [Flow 연산자 정책 매트릭스](../../docs/flow-operator-policy-matrix.md)에 기록합니다.
~~~

- [x] **Step 3: 양국어 링크와 문서 diff를 검증한다.**

Run:

~~~
git diff --check
rg -n 'flow-operator-policy-matrix|Flow policy follow-up|Flow 정책 후속' \
  bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md
~~~

Expected: 두 README에 각각 한 개의 matrix link가 있고 whitespace 오류가 없다. README의 기존 예제·이미지 링크는 변경하지 않는다.

- [x] **Step 4: 문서 parity commit을 만든다.**

~~~
git add bluetape4k/coroutines/README.md bluetape4k/coroutines/README.ko.md
git commit -m 'Document Flow policy follow-up boundaries' -m $'Constraint: Public README locales must explain Kotlin Flow semantics without implying Reactive Streams demand.\nRejected: API alias documentation | the approved tranche adds no production operator.\nConfidence: high\nScope-risk: narrow\nDirective: Keep both README locales aligned with the policy matrix before reopening #1300 deferred families.\nTested: git diff --check and bilingual link search.\nNot-tested: full module verification remains pending.'
~~~

### Task 4: Lesson과 최종 no-production-change 증거

**Files:**

- Create: docs/lessons/2026-08-03-issue-1300-flow-policy.md

- [x] **Step 1: 검증 결과를 한국어 lesson에 기록한다.**

Lesson은 다음 구조를 사용하고 실제 결과를 채운다.

~~~
# Issue #1300 Flow 정책 후속 lesson

## 결정

- 새 production API와 dependency는 추가하지 않았다.
- concat/merge/onBackpressureDrop 및 표준 buffer/conflate 계약만 고정했다.

## 근거

- live issue #1300은 contract matrix, caller evidence, deterministic failure/cancellation/bounded-memory tests를 요구한다.
- production caller 검색 결과와 공식 Kotlin/RxJava/Reactor 문서의 demand 차이.

## 검증

- 실제 실행한 Gradle 명령과 결과
- git diff --check
- origin 대비 production Kotlin source diff가 없다는 명령과 결과

## 재개 조건

실제 caller, 오류 집계 순서, CancellationException 보존, 동시성/메모리 상한,
overflow 정책을 별도 승인한 뒤에만 delay-error 또는 explicit overflow API를
추가한다.
~~~

- [x] **Step 2: production API diff가 없는지 확인한다.**

Run:

~~~
git diff --name-only origin/develop...HEAD -- \
  'bluetape4k/coroutines/src/main/**' \
  'gradle/**' '**/build.gradle.kts' 'settings.gradle.kts'
~~~

Expected: 출력이 비어 있다. 출력이 생기면 원인을 확인한 뒤 A안 범위를 벗어난 변경으로 보고한다.

- [x] **Step 3: lesson commit을 만든다.**

~~~
git add docs/lessons/2026-08-03-issue-1300-flow-policy.md
git commit -m 'Record evidence-backed Flow policy lesson' -m $'Constraint: Milestone 1.12.0 requires durable Korean project lessons and no speculative API parity.\nRejected: closing #1300 as implemented | deferred families still lack caller-backed contracts.\nConfidence: high\nScope-risk: narrow\nDirective: Reopen only with live caller evidence and fresh cancellation/memory tests.\nTested: focused tests, module check, diff and scope audits.\nNot-tested: GitHub PR/merge/release gates were not requested.'
~~~

### Task 5: 최종 검증과 DoD

**Files:**

- Verify: all changed files in this branch

- [x] **Step 1: module check를 실행한다.**

Run:

~~~
./gradlew :bluetape4k-coroutines:check \
  --no-configuration-cache --console=plain
~~~

Expected: BUILD SUCCESSFUL, including test and applicable detekt/static checks.

- [x] **Step 2: 전체 coroutine module test를 실행한다.**

Run:

~~~
./gradlew :bluetape4k-coroutines:test \
  --no-configuration-cache --console=plain
~~~

Expected: BUILD SUCCESSFUL and zero failed/error tests.

- [x] **Step 3: final scope and whitespace audit를 실행한다.**

Run:

~~~
git diff --check
git diff --name-only origin/develop...HEAD
git status --short --branch
~~~

Expected: only the approved spec, matrix, plan, contract test, two README locales, and lesson are present; no untracked files, no production Kotlin source/dependency/module changes, and no whitespace errors.

- [x] **Step 4: workflow completion evidence를 기록한다.**

Record in the final DoD: plan item statuses, exact test/check results, commit heads, no-PR/no-merge scope, blocked independent gpt-5.6-luna max research lane, and any unchecked GitHub publication gates as PENDING.

## Self-review

### Spec coverage

- Current API semantics and Flow standard mappings are covered by Task 2 tests.
- Caller evidence and deferred families remain in the committed matrix and are linked from both README locales in Task 3.
- Failure, cancellation, and bounded-memory acceptance is covered by the six deterministic tests in Task 2.
- No production API/dependency/catalog change is enforced by Task 4 scope audit.
- Durable Korean lesson and no PR/merge boundary are covered by Task 4.

### Placeholder scan

No TBD, TODO, or unspecified implementation step is used. Every mutation step names an exact path, command, expected result, and Lore commit trailer.

### Type consistency

The test class uses only existing functions (concat, merge, standard Flow buffer/conflate, Flow.collect) and does not refer to any API proposed by the deferred families. The README link resolves to the matrix created in the approved design commit.
