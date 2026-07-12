---
title: Subjects and event contracts
description: Design late-collector, replay, fan-out, work-sharing, and terminal semantics explicitly.
manualId: bluetape4k-coroutines
chapterId: subjects
---

# Subjects and event contracts

A Subject is a delivery contract, not merely a Flow adapter. Before selecting one, decide what a late collector receives and how many consumers handle each item.

![Comparison of Publish, Behavior, Replay, Multicast, and UnicastWork contracts](../../../assets/coroutines/subject-contracts.svg)

## Decision table

| Meaning | Subject | Late collector | Typical use |
| --- | --- | --- | --- |
| new events for active subscribers | `PublishSubject` | no history | callback events |
| current state plus updates | `BehaviorSubject` | latest value | state observation |
| bounded history plus updates | `ReplaySubject` | configured history | reconnect/catch-up |
| coordinated fan-out | `MulticastSubject` | no implicit replay | multi-consumer boundary |
| distribute work between consumers | `UnicastWorkSubject` | queued work | worker pool |

Prefer `StateFlow` or `SharedFlow` when they express the contract. Subjects are useful when explicit `complete()` and `emitError()` terminal events matter.

## Avoid the first-event race

`PublishSubject` does not retain values emitted before collector registration.

```kotlin
coroutineScope {
    val subject = PublishSubject<SensorEvent>()
    val collector = launch { subject.collect(::handle) }

    subject.awaitCollector()
    subject.emit(SensorEvent.Started)
    subject.complete()
    collector.join()
}
```

Real adapters must also define how callback threads schedule suspending emission and who closes both registration and launched jobs. `awaitCollector()` fixes startup ordering; it is not an unbounded buffer.

## Terminal state

`complete()` is normal termination; `emitError(cause)` is failed termination. Later terminal calls do not reverse the first terminal state. Collector cancellation remains cancellation and must not be converted into a Subject error.

Test three paths separately: normal completion, producer failure, and collector/parent cancellation with registry cleanup.

## Replay and memory budget

History is retained memory. Choose size-bound or size-and-time-bound replay from a real requirement. If only the latest state matters, `BehaviorSubject` or `StateFlow` is clearer.

## Work-sharing is not broadcast

`UnicastWorkSubject` delivers each queued item to one worker. Use Publish or Multicast semantics when every active subscriber needs the same event.

## Troubleshooting

| Symptom | Likely cause | Check |
| --- | --- | --- |
| first event missing | emission beat registration | `awaitCollector()` or replay policy |
| late collector has no value | Publish used as state | Behavior/Replay/StateFlow |
| memory grows | retained history or slow collector | bounds and queue depth |
| one cancelled collector affects others | cancellation/registry cleanup | `SubjectCancellationTest` |
| every worker handles the same job | broadcast confused with work-sharing | `UnicastWorkSubject` |

## Source and representative tests

- [`subject` implementations](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject)
- [`SubjectCancellationTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/SubjectCancellationTest.kt)
- [`PublishSubjectTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/PublishSubjectTest.kt)
- [`ReplaySubjectSizeBoundTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/ReplaySubjectSizeBoundTest.kt)

Next: choose child-failure semantics at scope level in [Structured concurrency policies](./structured-concurrency.md).
