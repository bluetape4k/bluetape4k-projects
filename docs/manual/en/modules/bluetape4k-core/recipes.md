---
title: Practical Core recipes
description: Assemble validation, encoding, bounded history, async capacity, and deterministic shutdown.
manualId: bluetape4k-core
chapterId: recipes
---

# Practical Core recipes

## Purpose

Core is a utility catalog, but real design follows boundary flow rather than a function list.

```text
caller input
  -> validate once
  -> choose representation
  -> mutate bounded local state
  -> submit through bounded async capacity
  -> observe result or rejection
  -> stop producers and close in dependency order
```

This chapter assembles earlier contracts into a small component. Not every helper is mandatory. Keep the Kotlin/JDK solution when it already expresses the contract clearly.

## Recipe 1: Validate once at the boundary

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

Validate before side effects. Passing a validated, non-null value object across the internal boundary avoids repeated defense against caller errors.

## Recipe 2: Choose representation and bounded history

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

`RingBuffer` fits recent history read oldest-first. Use `BoundedStack` for undo state that pops newest-first. Both evict the oldest value beyond capacity, so use them only where eviction is acceptable—not for audit logs or delivery queues.

Base64 is a transport representation, not security. If the payload is secret, snapshots and logs keep the same access policy as the source.

## Recipe 3: Put two capacities in front of external work

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

The caller must observe every `dispatch` future. Queue-full completes with `CapacityReachedException`; submission after close completes with `RejectedExecutionException`. Map these separately from task failure to retry or HTTP overload policy.

## Recipe 4: Separate normal shutdown from the JVM safety net

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

Call `runtime.close()` from the normal application lifecycle. ShutdownQueue registration is a backup attempt at reverse-order cleanup during process exit. Duplicate registration of the same object is ignored, but registering wrappers and their inner resources can still close each distinct object; make close implementations idempotent.

## End-to-end verification scenarios

| Scenario | Assertion to lock |
| --- | --- |
| Null/blank tenant | `IllegalArgumentException` before side effect |
| Payload round-trip | Decoded UTF-8 equals source text |
| History capacity + 1 | Oldest eviction, oldest-to-newest snapshot |
| Reducer queue full | Returned future cause is `CapacityReachedException` |
| Task error | Permit released and next queued task advances |
| Reducer close | Queued promise cancelled and new submission rejected |
| Shutdown | Dispatcher then sink after producer stop |

## Operations checklist

- Was capacity derived from memory limits and allowed wait time?
- Are eviction and rejection separate metrics?
- Is every returned future observed?
- Does normal lifecycle close resources before `ShutdownQueue` is needed?
- Are encoded secrets kept out of ordinary logs?
- Do downstream time/range queries have endpoint tests?

## Source and further reading

- [Validation and invariants](./validation.md)
- [Encoding and data boundaries](./encoding-data.md)
- [Bounded collections](./bounded-collections.md)
- [Time and ranges](./time-ranges.md)
- [Concurrency and lifecycle](./concurrency-lifecycle.md)
- [`bluetape4k-core` tests](../../../../../bluetape4k/core/src/test/kotlin/io/bluetape4k)

Even without a dedicated workshop, the representative tests are runnable specifications. Add a small consumer integration test for only the helpers you compose, and keep this manual as the contract source of truth.
