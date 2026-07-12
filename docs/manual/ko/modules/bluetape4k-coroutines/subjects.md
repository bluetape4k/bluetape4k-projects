---
title: Subject와 이벤트 계약
description: 이벤트, 최신 상태, replay, fan-out, work-sharing 계약을 구분합니다.
manualId: bluetape4k-coroutines
chapterId: subjects
---

# Subject와 이벤트 계약

## 해결할 문제

Callback이나 외부 이벤트를 Flow로 바꿀 때 구독자에게 무엇을 전달하고 얼마를 보관할지 명시해야 합니다.

## Mental model

Publish는 새 이벤트, Behavior는 최신 상태, Replay는 제한된 이력, Multicast는 조정된 fan-out, UnicastWork는 consumer 사이의 작업 분배를 표현합니다.

## 최소 API surface

`PublishSubject`, `BehaviorSubject`, `ReplaySubject`, `MulticastSubject`, `UnicastWorkSubject` 중 delivery contract에 맞는 하나를 선택합니다.

## 완전한 예제

Callback bridge는 collector를 시작하고 `awaitCollector()`로 준비를 확인한 뒤 event를 전달하며, 종료 시 `complete()` 또는 `error()`와 source cleanup을 함께 수행합니다.

## 선택 기준

모든 subscriber가 같은 값을 받아야 하는지, 늦은 subscriber에게 상태나 이력을 줄지, 하나의 worker만 처리할지를 기준으로 선택합니다.

## 실패·취소·수명주기 계약

Terminal event 이후 추가 complete/error 호출은 상태를 되돌리지 않습니다. Bounded history와 buffer capacity를 무제한으로 가정하지 않습니다.

## 운영과 문제 진단

Collector 수, dropped/queued event, terminal state, replay 크기를 관찰합니다. 시작 직후 유실은 `awaitCollector()` 호출 순서부터 확인합니다.

## Source와 representative test

근거는 [`subject`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/subject) 구현과 대응 test입니다.

## 이어 읽기와 runnable workshop

Callback 변환은 `flow-extensions-subject-bridge`, fan-out 선택은 [Operations](./operations.md)와 함께 봅니다.
