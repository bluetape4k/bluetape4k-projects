---
title: Bounded collections
description: Stack과 ring buffer의 순서, 용량, eviction 계약을 비교합니다.
manualId: bluetape4k-core
chapterId: bounded-collections
---

# Bounded collections

## 해결할 문제

최근 값의 개수를 제한하면서 조회 순서와 overflow 시 제거 대상을 명확히 해야 합니다.

## Mental model

`BoundedStack`은 newest-first, `RingBuffer`는 oldest-first로 읽습니다. 둘 다 capacity를 넘으면 가장 오래된 값을 제거합니다.

## 최소 API surface

최근 작업을 역순으로 소비하면 `BoundedStack`, 시간 순서의 history를 순회하면 `RingBuffer`를 사용합니다.

## 완전한 예제

Capacity 3에 1, 2, 3, 4를 넣어 stack과 ring의 iteration 결과와 1의 eviction을 함께 검증합니다.

## 선택 기준

읽기 순서가 선택 기준입니다. 동시 producer/consumer나 backpressure가 필요하면 bounded collection 대신 concurrency primitive를 사용합니다.

## 실패·취소·수명주기 계약

Capacity는 memory bound이지 처리량 제어가 아닙니다. Invalid capacity는 생성 경계에서 거부합니다.

## 운영과 문제 진단

Capacity 도달 횟수와 eviction 수를 관찰해 데이터 손실이 의도한 정책인지 확인합니다.

## Source와 representative test

[`BoundedStack.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/BoundedStack.kt), [`RingBuffer.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/RingBuffer.kt), 대응 test를 근거로 합니다.

## 이어 읽기와 runnable workshop

Concurrent aggregation은 [Concurrency와 lifecycle](./concurrency-lifecycle.md)에서 다룹니다.
