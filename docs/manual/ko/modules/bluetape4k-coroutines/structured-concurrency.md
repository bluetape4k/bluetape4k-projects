---
title: Structured concurrency 정책
description: Fail-fast, first-success, supervised partial result 정책을 선택합니다.
manualId: bluetape4k-coroutines
chapterId: structured-concurrency
---

# Structured concurrency 정책

## 해결할 문제

Child 작업 하나가 실패했을 때 siblings와 최종 결과를 어떻게 처리할지 scope 정책으로 고정해야 합니다.

## Mental model

Fail-fast는 한 실패가 전체 결과를 무효화하고, first-success는 먼저 성공한 값을 채택하며, supervised policy는 독립 실패를 격리합니다.

## 최소 API surface

`taskScope`, `firstSuccessTaskScope`, `supervisedTaskScope`를 결과 의미에 따라 선택합니다.

## 완전한 예제

두 provider가 모두 필요한 조합은 fail-fast, 여러 replica 중 하나면 되는 조회는 first-success, 독립 widget 집계는 supervised partial-result로 구성합니다.

## 선택 기준

Business result가 atomic인지, 일부 결과가 유효한지, 가장 먼저 성공한 하나만 필요한지를 먼저 답합니다.

## 실패·취소·수명주기 계약

Scope가 끝날 때 child가 남지 않아야 합니다. Loser cancellation과 failure ordering을 호출자에게 보이는 exception 계약과 맞춥니다.

## 운영과 문제 진단

Policy별 성공·실패·취소 수와 전체 latency를 분리합니다. Supervision을 실패 은폐 수단으로 사용하지 않습니다.

## Source와 representative test

[`StructuredConcurrency.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/StructuredConcurrency.kt)와 policy test가 기준입니다.

## 이어 읽기와 runnable workshop

실제 race 구성은 [Deferred 조정](./deferred.md), 관측 규칙은 [Operations](./operations.md)에서 이어집니다.
