---
title: 운영과 관측 가능성
description: In-flight, pressure, latency, cancellation, readiness, shutdown을 하나의 lifecycle로 관찰합니다.
manualId: bluetape4k-coroutines
chapterId: operations
---

# 운영과 관측 가능성

Coroutine 개수 하나로는 문제를 설명할 수 없습니다. 수요(in-flight), 압력(queue/buffer), 처리 시간, cancellation 이유, resource owner를 같은 경계에서 관찰해야 합니다.

![Coroutine boundary의 최소 운영 신호와 deterministic shutdown](../../../assets/coroutines/observability-boundaries.svg)

## 최소 signal set

| 신호 | 질문 | 권장 차원 |
| --- | --- | --- |
| in-flight jobs/workers | 지금 처리 중인 수요가 얼마인가 | operation, owner |
| queue/buffer depth | downstream보다 producer가 빠른가 | component, capacity |
| latency P50/P95/P99 | 느린 tail이 어디서 생기는가 | operation, outcome |
| cancellation | caller 포기, timeout, shutdown 중 무엇인가 | reason, owner |
| shutdown duration | intake 중단부터 resource 해제까지 얼마나 걸리는가 | component, phase |

High-cardinality request ID나 exception message를 metric label로 사용하지 않습니다. Trace/log에 남기고 metric은 bounded dimension을 유지합니다.

## Cancellation을 error로 세지 않기

```kotlin
suspend fun <T> Timer.recordSuspend(block: suspend () -> T): T {
    val sample = Timer.start(registry)
    try {
        return block()
    } catch (e: CancellationException) {
        cancellationCounter.increment()
        throw e
    } catch (e: Exception) {
        failureCounter.increment()
        throw e
    } finally {
        sample.stop(this)
    }
}
```

정상 caller cancellation과 timeout을 분리하면 사용자 포기와 service latency 문제를 구분할 수 있습니다. Cancellation을 error span으로 바꾸지 않되 reason과 owner는 기록합니다.

## Readiness와 liveness

- Readiness: 새 작업을 안전하게 받을 수 있는가.
- Liveness: process가 회복 가능한가.

Downstream connection pool이 고갈되거나 queue가 capacity에 붙어 있으면 intake를 줄이거나 readiness를 내리는 정책이 필요할 수 있습니다. 단순 CPU 사용률만으로 판단하지 않습니다.

## Deterministic shutdown

1. readiness를 내려 새 traffic routing을 막습니다.
2. listener/consumer/intake를 중단합니다.
3. 정한 timeout 안에서 bounded work를 drain합니다.
4. channel/subject를 terminal state로 만들고 owned scope와 dispatcher를 닫습니다.
5. 남은 job/thread/resource 수를 기록합니다.

```kotlin
override fun close() = runBlocking {
    accepting.set(false)
    withTimeoutOrNull(shutdownTimeout) { pendingJobs.joinAll() }
    subject.complete()
    workerScope.close()
}
```

Production code에서는 `runBlocking`을 호출할 thread와 framework shutdown timeout을 함께 검토합니다. 동일 dispatcher 안에서 자신을 기다리는 구조를 만들지 않습니다.

## 문제 진단 순서

| 증상 | 먼저 볼 신호 | 다음 확인 |
| --- | --- | --- |
| latency 증가 | queue wait와 downstream latency | parallelism/capacity mismatch |
| timeout 증가 | in-flight, retry count | retry × race 폭증 |
| shutdown 지연 | 남은 job과 dispatcher thread | owner/close 경로 |
| 첫 event 유실 | collector count와 registration 시점 | Subject startup contract |
| cancellation이 error dashboard를 오염 | exception 분류 | `CancellationException` 재전파 |

## Source와 verification anchors

- Lifecycle 구현: [`CloseableCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt)
- Flow pressure: [`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt)
- Subject cancellation tests: [`SubjectCancellationTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/SubjectCancellationTest.kt)

끝으로 이 계약들을 실행 가능한 조합으로 묶는 [실전 레시피와 Workshop](./recipes.md)을 봅니다.
