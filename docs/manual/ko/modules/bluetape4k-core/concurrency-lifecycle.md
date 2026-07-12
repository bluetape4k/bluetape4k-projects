---
title: Concurrency와 lifecycle
description: ConcurrentReducer capacity와 ShutdownQueue 종료 순서를 다룹니다.
manualId: bluetape4k-core
chapterId: concurrency-lifecycle
---

# Concurrency와 lifecycle

## 해결할 문제

외부 비동기 API 앞에서 동시 실행 수만 제한하면 producer가 더 빠를 때 대기열이 무한히 자랄 수 있습니다. 종료 시점에는 새 작업을 막고, 대기 중 작업과 이미 실행 중인 외부 작업을 구분하며, 의존 자원을 반대 순서로 정리해야 합니다.

`ConcurrentReducer`는 **active capacity**와 **queue capacity**를 함께 제한합니다. `ShutdownQueue`는 JVM 종료 시 남아 있는 process-wide 자원을 등록 역순으로 닫는 최후 안전망입니다.

![ConcurrentReducer capacity and failure paths](../../../assets/core/concurrent-reducer-capacity.svg)

## ConcurrentReducer의 상태 모델

| 상태/신호 | 의미 |
| --- | --- |
| `activeCount` | 현재 permit을 사용 중인 작업 수 |
| `queuedCount` | 아직 시작되지 않은 작업 수 |
| `remainingActiveCapacity` | 즉시 시작 가능한 permit 수 |
| `remainingQueueCapacity` | 추가로 대기시킬 수 있는 작업 수 |
| closed | 새 submission을 거부하고 queued promise를 취소한 상태 |

`maxConcurrency`와 `maxQueueSize`는 모두 양수여야 합니다. 내부 queue는 `ArrayBlockingQueue`, 실행 제한은 `Semaphore`, completion 이후 다음 작업을 시작하는 pump는 single-thread executor를 사용합니다.

## 완전한 사용 예제

```kotlin
import io.bluetape4k.concurrent.concurrentReducerOf
import java.util.concurrent.CompletionStage

fun <T> fetchAll(
    ids: List<String>,
    fetchAsync: (String) -> CompletionStage<T>,
): List<T> = concurrentReducerOf<T>(
    maxConcurrency = 8,
    maxQueueSize = 64,
).use { reducer ->
    val promises = ids.map { id -> reducer.add { fetchAsync(id) } }
    promises.map { it.join() }
}
```

`add`가 반환한 모든 promise를 관찰해야 합니다. queue가 가득 차거나 reducer가 닫혀도 호출 지점에서 동기 예외가 발생하는 것이 아니라 이미 실패한 `CompletableFuture`가 반환되기 때문입니다.

## Submission과 completion 계약

| 상황 | 반환 promise |
| --- | --- |
| queue에 수용됨 | task의 결과/오류로 완료 |
| queue full | `CapacityReachedException`으로 실패 |
| reducer closed | `RejectedExecutionException`으로 실패 |
| task lambda가 throw | 같은 오류로 실패하고 permit 반환 |
| task가 `null` stage 반환 | `NullPointerException`으로 실패 |
| caller가 queued promise cancel | 시작 전에 건너뛰고 permit 반환 |

`join()`은 원래 cause를 `CompletionException`으로 감쌀 수 있습니다. overload rejection, task failure, caller cancellation을 cause 기준으로 분리해 정책을 적용합니다.

## Close는 무엇을 보장하는가

`close()`는 idempotent합니다. 첫 호출은 closed flag를 세우고 queue에 남은 promise를 `cancel(false)`로 취소한 뒤 내부 pump executor를 shutdown합니다. 이미 시작된 외부 `CompletionStage`는 reducer가 소유하지 않으므로 강제로 취소하지 않습니다.

따라서 종료 순서는 다음처럼 설계합니다.

1. producer가 새 작업을 만들지 못하게 합니다.
2. 필요한 만큼 running 작업을 기다리거나, 외부 client 자체의 cancellation API를 사용합니다.
3. reducer를 닫아 queued work를 취소합니다.
4. reducer가 호출하는 client/executor를 닫습니다.

## ShutdownQueue의 역할

![Shutdown resources in reverse registration order](../../../assets/core/shutdown-order.svg)

`ShutdownQueue.register(closeable)`은 같은 객체의 중복 등록을 무시하고, JVM shutdown hook에서 `pollLast()`로 역순(LIFO) close합니다. `closeSafe`를 사용하므로 한 자원의 close 실패가 뒤의 cleanup을 막지 않습니다.

```kotlin
val client = ExternalClient()
val service = Service(client)

ShutdownQueue.register(client)   // dependency first
ShutdownQueue.register(service)  // dependent wrapper later; closes first
```

이 queue를 정상 lifecycle 대신 사용하지 않습니다. Spring bean, request, test fixture처럼 더 이른 명시적 종료 시점이 있으면 그곳에서 직접 닫고, `ShutdownQueue`는 process exit 누락을 막는 안전망으로 둡니다.

## 선택 기준

| 요구 | 선택 |
| --- | --- |
| `CompletionStage` 작업의 active와 waiting 수를 모두 제한 | `ConcurrentReducer` |
| suspend 함수 동시성만 제한 | coroutine semaphore 또는 `mapParallel` |
| durable delivery/retry | broker 또는 durable queue |
| component가 소유한 자원의 정상 종료 | component lifecycle에서 직접 close |
| process-wide 자원의 JVM 종료 안전망 | `ShutdownQueue` |

## 운영과 문제 진단

- `activeCount`, `queuedCount`, queue-full rejection, task failure를 별도 metric으로 둡니다.
- queue capacity는 숨은 latency budget입니다. `maxQueueSize / 처리율`로 최악 대기 시간을 추정합니다.
- shutdown에서는 producer stop, queued cancellation, running drain, dependency close 시간을 나눠 기록합니다.
- full queue를 무제한 retry하면 overload가 증폭됩니다. retry budget, backoff, 429/503 같은 caller-visible 정책을 둡니다.

## Source와 representative test

- [`ConcurrentReducer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ConcurrentReducer.kt)
- [`ConcurrentReducerTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/concurrent/ConcurrentReducerTest.kt)
- [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt)
- [`ShutdownQueueTest.kt`](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k/utils/ShutdownQueueTest.kt)

Process-local bounded state는 [Bounded collections](./bounded-collections.md), 전체 조립은 [Core 실전 레시피](./recipes.md)에서 이어집니다.
