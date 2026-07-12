---
title: Deferred 조정
description: 첫 완료, 첫 성공, loser 취소 정책을 구분합니다.
manualId: bluetape4k-coroutines
chapterId: deferred
---

# Deferred 조정

## 해결할 문제

여러 비동기 작업 중 무엇을 기다리고 나머지를 어떻게 처리할지 의미를 먼저 정해야 합니다.

## Mental model

첫 완료는 실패나 취소도 결과 후보에 포함하지만 첫 성공은 성공 값이 나올 때까지 실패를 건너뜁니다. Winner 선택과 loser 정리는 서로 다른 정책입니다.

## 최소 API surface

단일 값은 `DeferredValue.await()`, 첫 완료는 `awaitAny`, loser를 취소하는 첫 완료는 `awaitAnyAndCancelOthers`, 첫 성공은 `firstSuccessTaskScope`를 사용합니다.

## 완전한 예제

Replica race는 모든 `Deferred`를 caller scope에서 시작하고 선택 API를 호출한 뒤, 선택 정책에 따라 loser를 유지하거나 취소하도록 구성합니다.

## 선택 기준

가장 빠른 응답이면 first completion, 장애 replica를 건너뛰어야 하면 first success를 선택합니다. Background warm-up처럼 loser 결과가 필요하면 자동 취소 API를 쓰지 않습니다.

## 실패·취소·수명주기 계약

`DeferredValue`는 eager하게 시작되는 owned scope를 가지므로 사용 후 닫아야 합니다. Blocking `value` 대신 suspend `await()`를 우선합니다.

## 운영과 문제 진단

Winner latency뿐 아니라 loser 실행 시간과 cancellation 완료 시간도 기록합니다. 반복되는 race가 취소되지 않으면 외부 연결과 thread를 고갈시킬 수 있습니다.

## Source와 representative test

[`DeferredValue.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt), [`DeferredSupport.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt), representative tests를 기준으로 합니다.

## 이어 읽기와 runnable workshop

Stream 단위 병렬화는 [Ordered & Parallel Flow](./flow.md), policy scope는 [Structured concurrency](./structured-concurrency.md)에서 이어집니다.
