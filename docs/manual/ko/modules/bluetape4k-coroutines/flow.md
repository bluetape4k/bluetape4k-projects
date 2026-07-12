---
title: 순서 보장과 병렬 Flow
description: 순서, 병렬도, downstream capacity를 기준으로 Flow 연산을 선택합니다.
manualId: bluetape4k-coroutines
chapterId: flow
---

# 순서 보장과 병렬 Flow

## 해결할 문제

처리량을 높이면서 입력 순서를 보존할지, 완료 순서로 결과를 내보낼지 결정해야 합니다.

## Mental model

`flow.async`는 내부 작업을 겹쳐도 emission 순서를 유지합니다. `mapParallel`은 제한된 병렬도로 처리하고 완료된 결과부터 내보낼 수 있습니다.

## 최소 API surface

순차 변환은 표준 `map`, ordered concurrency는 `flow.async`, throughput-first 변환은 `mapParallel`을 사용합니다.

## 완전한 예제

입력 ID를 원격 조회로 enrichment할 때 결과 순서가 API 계약이면 ordered path를 사용하고, 독립 저장 작업이면 bounded `mapParallel`을 사용합니다.

## 선택 기준

Ordering 계약, 작업 latency 분산, downstream 처리량을 함께 봅니다. `parallelism <= 1`에서는 sequential path가 사용되며 불필요한 worker를 만들지 않습니다.

## 실패·취소·수명주기 계약

Upstream 취소는 진행 중인 child 작업에 전파되어야 합니다. 병렬도를 외부 connection pool이나 downstream capacity보다 크게 잡지 않습니다.

## 운영과 문제 진단

In-flight 수, buffer 크기, item latency 분포를 관찰합니다. 평균 latency만으로 head-of-line blocking을 숨기지 않습니다.

## Source와 representative test

[`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt), [`mapParallel.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/mapParallel.kt)와 test를 근거로 합니다.

## 이어 읽기와 runnable workshop

`flow-extensions-parallel-enrichment`, `flow-extensions-race-fallback`, `flow-extensions-search-pipeline` workshop에서 실행 흐름을 확인합니다.
