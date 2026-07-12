---
title: 순서 보장과 병렬 Flow
description: 입력 순서, 완료 순서, 병렬도, buffer capacity를 기준으로 Flow 연산을 선택합니다.
manualId: bluetape4k-coroutines
chapterId: flow
---

# 순서 보장과 병렬 Flow

작업을 동시에 실행하는 것과 결과를 어떤 순서로 emit하는 것은 별개의 계약입니다. `flow.async`와 `mapParallel`의 가장 큰 차이는 속도가 아니라 **output ordering**입니다.

![flow.async의 ordered emission과 mapParallel의 completion-order emission](../../../assets/coroutines/ordered-parallel-flow.svg)

## 선택표

| 요구 | 선택 | Output order | 병렬도/압력 제어 |
| --- | --- | --- | --- |
| 단순 순차 변환 | 표준 `map` | 입력 순서 | 한 번에 하나 |
| 계산을 겹치되 응답 순서 유지 | `Flow.async` | 입력 순서 | collect buffer |
| 완료된 결과부터 처리 | `mapParallel(n)` | 완료 순서 가능 | `n`으로 제한 |

## Ordered concurrency

`Flow.async`는 각 input을 `LazyDeferred`로 바꾸고 collect scope에서 시작합니다. 여러 계산이 겹치지만 downstream은 deferred를 원래 순서대로 `await()`합니다.

```kotlin
val ordered: List<Product> = productIds.asFlow()
    .async(Dispatchers.IO) { id -> catalog.load(id) }
    .collect(capacity = 16) { product -> render(product) }
```

앞선 item이 느리면 뒤 item이 이미 완료돼도 emit을 기다립니다. 이것이 head-of-line waiting의 비용이며, API 응답과 같이 순서가 계약일 때 지불할 가치가 있습니다.

`capacity`는 `Channel.BUFFERED`, `Channel.CONFLATED`, 또는 0 이상이어야 합니다. 다른 음수는 `IllegalArgumentException`입니다. Capacity는 완료 결과를 보관하는 공간이지 무제한 병렬 실행 허가가 아닙니다.

## Throughput-first 병렬 변환

`mapParallel`은 `parallelism`을 최소 1로 보정합니다. 1이면 표준 `map`; 2 이상이면 `flatMapMerge(concurrency)`로 각 suspend transform을 병합합니다.

```kotlin
val stored = events.asFlow()
    .mapParallel(parallelism = 8, context = Dispatchers.IO) { event ->
        repository.persist(event)
    }
    .toList()
```

완료가 빠른 item이 먼저 내려올 수 있으므로 입력 순서를 public API 계약으로 삼으면 안 됩니다. 저장, 독립 enrichment, thumbnail 생성처럼 결과가 서로 독립적일 때 적합합니다.

## 병렬도 산정

CPU core 수만 보고 결정하지 않습니다.

```text
effective parallelism = min(
  application budget,
  connection pool capacity,
  remote concurrency limit,
  memory/buffer budget
)
```

Retry가 있다면 최악의 in-flight는 대략 `parallelism × attempts`까지 늘 수 있습니다. 동일한 downstream을 여러 pipeline이 공유하면 각 pipeline 숫자의 합도 계산합니다.

## 실패와 취소

- transform 예외는 collection을 실패시키고 sibling work가 구조화된 scope에서 취소됩니다.
- collector 취소는 upstream과 진행 중인 child에 전파돼야 합니다.
- `flowOn(context)`는 pipeline context를 바꾸지만 lifecycle owner를 바꾸지 않습니다.
- timeout 뒤에도 remote call이 남으면 Flow가 아니라 client cancellation bridge를 확인합니다.

## 테스트할 것

1. `mapParallel(1)`, `0`, 음수가 순차 path와 같은 순서를 내는지.
2. 2 이상에서 완료 순서가 달라질 수 있음을 테스트가 가정하는지.
3. active transform 수가 설정한 upper bound를 넘지 않는지.
4. collector 취소 뒤 child와 외부 request가 정리되는지.
5. ordered path에서 느린 첫 item이 head-of-line waiting을 만드는지.

## 운영 신호

Item latency의 평균만 보지 말고 P95/P99, in-flight transform, buffer 사용량, downstream wait를 함께 기록합니다. Buffer가 계속 차면 producer와 consumer의 capacity mismatch입니다. 병렬도를 올리기 전에 bottleneck boundary를 찾습니다.

## Source와 representative tests

- [`AsyncFlow.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/AsyncFlow.kt)
- [`mapParallel.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow/extensions/mapParallel.kt)
- [`AsyncFlowTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/AsyncFlowTest.kt)
- [`MapParallelTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/flow/extensions/MapParallelTest.kt)

Callback 또는 hot stream의 delivery 의미가 필요하면 [Subject와 이벤트 계약](./subjects.md)으로 이어집니다.
