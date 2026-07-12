---
title: Concurrency와 lifecycle
description: ConcurrentReducer capacity와 ShutdownQueue 종료 순서를 다룹니다.
manualId: bluetape4k-core
chapterId: concurrency-lifecycle
---

# Concurrency와 lifecycle

## 해결할 문제

비동기 aggregation의 capacity와 종료 시 queued/running 작업의 처리를 명시해야 합니다.

## Mental model

`ConcurrentReducer`는 running과 queued work를 제한하고, `ShutdownQueue`는 등록 역순으로 resource를 닫습니다.

## 최소 API surface

Bounded aggregation에는 `ConcurrentReducer`, 여러 close action의 deterministic LIFO 실행에는 `ShutdownQueue`를 사용합니다.

## 완전한 예제

Reducer를 작은 capacity로 만들고 full 상태의 failed future, close 시 queued cancellation, 이미 실행 중인 external stage의 별도 lifecycle을 확인합니다.

## 선택 기준

Aggregation 자체가 필요할 때만 reducer를 사용합니다. 단순 queue나 coroutine coordination을 대신하는 범용 executor로 사용하지 않습니다.

## 실패·취소·수명주기 계약

`add`는 full/closed에서 동기 throw 대신 failed future를 반환합니다. Close는 queued work를 취소하지만 외부에서 이미 실행 중인 stage까지 강제 취소하지 않습니다.

## 운영과 문제 진단

Running, queued, rejected, close latency를 분리해 관찰합니다. Shutdown failure는 LIFO cleanup의 어느 action에서 발생했는지 남깁니다.

## Source와 representative test

[`ConcurrentReducer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/concurrent/ConcurrentReducer.kt), [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt)와 test가 기준입니다.

## 이어 읽기와 runnable workshop

Bounded state는 [Bounded collections](./bounded-collections.md), 조립 패턴은 [Recipes](./recipes.md)에서 이어집니다.
