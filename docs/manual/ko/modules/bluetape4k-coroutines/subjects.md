---
title: Subject와 이벤트 계약
description: late collector, replay, fan-out, work-sharing, terminal event를 명시적으로 설계합니다.
manualId: bluetape4k-coroutines
chapterId: subjects
---

# Subject와 이벤트 계약

Subject는 “Flow로 바꾸는 도구”가 아니라 subscriber에게 무엇을 전달할지 정하는 delivery contract입니다. Hot stream을 선택하기 전에 late collector가 받을 값과 item 하나의 consumer 수를 답해야 합니다.

![Publish, Behavior, Replay, Multicast, UnicastWork Subject 계약 비교](../../../assets/coroutines/subject-contracts.svg)

## 선택표

| 의미 | Subject | 늦은 collector | 대표 용도 |
| --- | --- | --- | --- |
| 현재 붙어 있는 subscriber에게 새 event | `PublishSubject` | 과거 값 없음 | callback event |
| 현재 상태와 이후 update | `BehaviorSubject` | 최신 값 | 상태 관찰 |
| 제한된 이력과 이후 update | `ReplaySubject` | 설정한 이력 | reconnect/catch-up |
| 조정된 fan-out | `MulticastSubject` | implicit replay 없음 | 여러 consumer 경계 |
| work item을 consumer 사이에 분배 | `UnicastWorkSubject` | queued work | worker pool |

`StateFlow`/`SharedFlow`로 충분하면 표준 API를 우선합니다. `complete()`와 `emitError()` 같은 명시적인 terminal contract가 필요할 때 Subject가 의미를 더 잘 드러냅니다.

## 첫 event를 잃지 않는 callback bridge

`PublishSubject`는 collector 등록 전 값을 보관하지 않습니다. 시작 순서가 계약이면 등록을 기다립니다.

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

실제 adapter에서는 callback thread에서 suspend `emit`을 어떻게 schedule할지, registration과 launched job을 누가 닫을지 명시해야 합니다. `awaitCollector()`는 startup race를 해결하지만 무제한 buffer를 제공하지 않습니다.

## Terminal state

`complete()`는 정상 종료, `emitError(cause)`는 실패 종료입니다. terminal state가 정해진 뒤 추가 terminal 호출은 이전 결과를 뒤집지 않습니다. Collector cancellation은 subject error로 바꾸지 말고 그대로 전파합니다.

다음 세 경로를 각각 테스트합니다.

1. producer가 정상 complete하고 collector가 끝난다.
2. producer error가 collector에게 전달된다.
3. collector/parent cancellation이 `CancellationException`으로 유지되고 collector registry에서 제거된다.

## Replay와 memory budget

Replay는 편리하지만 history가 곧 memory retention입니다. Size-bound와 size+time-bound 구현 중 business requirement에 맞는 것을 선택합니다. “최근 상태 하나”라면 `BehaviorSubject` 또는 `StateFlow`가 더 명확합니다.

## Work-sharing은 broadcast가 아니다

`UnicastWorkSubject`는 같은 item을 모든 collector에게 복제하지 않습니다. 여러 worker 중 하나가 처리하는 queue 의미입니다. 모든 subscriber가 같은 event를 받아야 한다면 Publish/Multicast 계열을 선택합니다.

## 문제 진단

| 증상 | 원인 후보 | 확인 |
| --- | --- | --- |
| 첫 event 유실 | emit이 collector 등록보다 빠름 | `awaitCollector()` 또는 replay 계약 |
| late collector에 값이 없음 | Publish를 상태처럼 사용 | Behavior/Replay/StateFlow 검토 |
| memory 증가 | replay/history 또는 느린 collector | bound와 queue depth 관찰 |
| 한 collector 취소가 다른 collector에 영향 | cancellation 처리/registry cleanup | `SubjectCancellationTest` |
| 모든 worker가 같은 job 처리 | broadcast와 work-sharing 혼동 | `UnicastWorkSubject` 검토 |

## Source와 representative tests

- [`subject` implementations](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject)
- [`SubjectCancellationTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/SubjectCancellationTest.kt)
- [`PublishSubjectTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/PublishSubjectTest.kt)
- [`ReplaySubjectSizeBoundTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/subject/ReplaySubjectSizeBoundTest.kt)

다음은 child failure의 의미를 scope 수준에서 정하는 [Structured concurrency 정책](./structured-concurrency.md)입니다.
