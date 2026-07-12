---
title: 운영과 관측 가능성
description: Job, queue, latency, cancellation, readiness를 운영 신호로 연결합니다.
manualId: bluetape4k-coroutines
chapterId: operations
---

# 운영과 관측 가능성

## 해결할 문제

Coroutine 수가 아니라 어떤 작업이 지연되고 취소되며 종료되지 않는지를 관찰해야 합니다.

## Mental model

Active job은 수요, queue/buffer는 pressure, latency는 service time, cancellation은 정상 종료 또는 caller 포기를 나타냅니다.

## 최소 API surface

Coroutine context의 trace propagation, Micrometer timer/counter, component readiness와 lifecycle hook을 조합합니다.

## 완전한 예제

Request span 아래에 suspend 작업 시간을 기록하고 `CancellationException`은 재전파합니다. Component close에서는 intake를 멈춘 뒤 channel과 owned scope를 종료합니다.

## 선택 기준

Readiness는 새 요청을 받을 수 있는 상태, liveness는 process 회복 가능성을 나타냅니다. Queue가 찬 상태를 단순 CPU 사용률로 판단하지 않습니다.

## 실패·취소·수명주기 계약

정상 cancellation을 error span으로 바꾸지 않습니다. Shutdown timeout이 끝나면 남은 작업과 외부 resource를 명시적으로 정리합니다.

## 운영과 문제 진단

P50/P95/P99 latency, in-flight, queue depth, timeout, cancellation reason을 함께 대시보드에 둡니다.

## Source와 representative test

Owned scope와 Subject 구현, `observability/micrometer-tracing-coroutines` workshop을 현재 source와 함께 검증합니다.

## 이어 읽기와 runnable workshop

운영 예제는 observability workshop, 종료 ownership은 [수명주기와 취소](./lifecycle.md)에서 확인합니다.
