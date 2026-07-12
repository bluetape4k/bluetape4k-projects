---
title: Core 실전 레시피
description: 검증, encoding, bounded history, async capacity, deterministic shutdown을 조립합니다.
manualId: bluetape4k-core
chapterId: recipes
---

# Core 실전 레시피

## 이 장의 목적

Core는 utility catalog이지만 실제 설계는 함수 목록이 아니라 경계의 흐름으로 결정됩니다.

```text
caller input
  -> validate once
  -> choose representation
  -> mutate bounded local state
  -> submit through bounded async capacity
  -> observe result or rejection
  -> stop producers and close in dependency order
```

이 장은 앞선 계약을 하나의 작은 component로 조립합니다. 각 helper가 꼭 필요한 것은 아닙니다. Kotlin/JDK 표준 API만으로 계약이 충분히 명확하다면 그것을 유지합니다.

## Recipe 1: 경계에서 한 번 검증하기

![Validate at the boundary and preserve invariants](../../../assets/core/validation-boundary.svg)

```kotlin
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank

data class EventCommand(
    val tenantId: String,
    val payload: String,
    val priority: Int,
)

fun eventCommand(tenantId: String?, payload: String?, priority: Int) =
    EventCommand(
        tenantId = tenantId.requireNotBlank("tenantId"),
        payload = payload.requireNotBlank("payload"),
        priority = priority.requireInRange(0, 9, "priority"),
    )
```

Validation을 side effect 앞에 둡니다. 검증된 non-null value object가 내부 경계를 통과하도록 하면 이후 코드가 caller 오류를 반복해서 방어하지 않아도 됩니다.

## Recipe 2: 표현과 bounded history 선택하기

```kotlin
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.collections.RingBuffer

data class AcceptedEvent(val tenantId: String, val encodedPayload: String)

class RecentEvents(capacity: Int) {
    private val events = RingBuffer<AcceptedEvent>(capacity)

    fun accept(command: EventCommand): AcceptedEvent =
        AcceptedEvent(command.tenantId, command.payload.encodeBase64String())
            .also(events::add)

    fun snapshot(): List<AcceptedEvent> = events.toList() // oldest -> newest
}
```

![BoundedStack and RingBuffer retain different read orders](../../../assets/core/bounded-collection-ordering.svg)

`RingBuffer`는 가장 오래된 값부터 읽는 최근 이력에 맞습니다. undo처럼 최신 값을 먼저 pop해야 한다면 `BoundedStack`을 사용합니다. 둘 다 capacity 초과 시 가장 오래된 값을 버리므로 eviction 자체가 허용되는 데이터에만 사용합니다. Audit log와 delivery queue에는 맞지 않습니다.

Base64는 transport 표현일 뿐 보안 기능이 아닙니다. 위 payload가 secret이면 snapshot/log의 접근 정책도 원본과 같아야 합니다.

## Recipe 3: 외부 작업에 두 개의 capacity를 두기

```kotlin
import io.bluetape4k.concurrent.ConcurrentReducer
import io.bluetape4k.concurrent.concurrentReducerOf
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

interface EventSink : AutoCloseable {
    fun send(event: AcceptedEvent): CompletionStage<String>
}

class EventDispatcher(private val sink: EventSink) : AutoCloseable {
    private val reducer: ConcurrentReducer<String> =
        concurrentReducerOf(maxConcurrency = 8, maxQueueSize = 64)

    fun dispatch(event: AcceptedEvent): CompletableFuture<String> =
        reducer.add { sink.send(event) }

    val active: Int get() = reducer.activeCount
    val queued: Int get() = reducer.queuedCount

    override fun close() = reducer.close()
}
```

![Bound active work and waiting work independently](../../../assets/core/concurrent-reducer-capacity.svg)

호출자는 `dispatch`의 future를 반드시 관찰합니다. queue full은 `CapacityReachedException`, close 이후 submission은 `RejectedExecutionException`으로 완료됩니다. 이 실패를 task 자체의 실패와 구분해 retry/HTTP overload 정책에 연결합니다.

## Recipe 4: 정상 종료와 JVM 안전망을 분리하기

```kotlin
import io.bluetape4k.utils.ShutdownQueue

class EventRuntime(
    private val sink: EventSink,
    private val dispatcher: EventDispatcher,
) : AutoCloseable {
    override fun close() {
        dispatcher.close() // producers stopped before this call
        sink.close()
    }
}

val sink: EventSink = createEventSink()
val dispatcher = EventDispatcher(sink)
val runtime = EventRuntime(sink, dispatcher)

ShutdownQueue.register(sink)
ShutdownQueue.register(dispatcher)
ShutdownQueue.register(runtime)
```

![Register dependencies first so dependents close first](../../../assets/core/shutdown-order.svg)

실제 application lifecycle에서는 `runtime.close()`를 직접 호출합니다. ShutdownQueue 등록은 비정상적인 process exit에서도 역순 cleanup을 시도하기 위한 보조 장치입니다. 동일 객체 중복 등록은 무시되지만 wrapper와 내부 resource를 각각 등록하면 각 객체는 한 번씩 close될 수 있으므로 close 구현은 idempotent해야 합니다.

## End-to-end 검증 시나리오

| 시나리오 | 고정할 assertion |
| --- | --- |
| null/blank tenant | side effect 전 `IllegalArgumentException` |
| payload round-trip | UTF-8 원문과 decode 결과 동일 |
| history capacity + 1 | oldest eviction, oldest-to-newest snapshot |
| reducer queue full | 반환 future의 cause가 `CapacityReachedException` |
| task error | permit 반환 후 다음 queued task 진행 |
| reducer close | queued promise cancelled, new submission rejected |
| shutdown | producer stop 후 dispatcher, sink 순서로 close |

## 운영 체크리스트

- capacity는 메모리 한도와 허용 대기 시간에서 역산했는가?
- eviction과 rejection을 metric으로 구분했는가?
- 모든 returned future를 관찰하는가?
- 정상 lifecycle이 `ShutdownQueue`보다 먼저 자원을 닫는가?
- encoded secret을 일반 text처럼 로그에 남기지 않는가?
- timezone/range가 포함된 downstream query는 endpoint test가 있는가?

## Source와 이어 읽기

- [검증과 불변식](./validation.md)
- [Encoding과 데이터 경계](./encoding-data.md)
- [Bounded collections](./bounded-collections.md)
- [시간과 범위](./time-ranges.md)
- [Concurrency와 lifecycle](./concurrency-lifecycle.md)
- [`bluetape4k-core` tests](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)

별도의 workshop이 없어도 위 representative test를 runnable specification으로 사용할 수 있습니다. Consumer module에서는 필요한 helper만 묶은 작은 integration test를 추가하고, 이 매뉴얼을 계약의 source of truth로 유지합니다.
