---
title: Structured concurrency 정책
description: Fail-fast, first-success, supervised partial-result 정책을 business result에 맞춰 선택합니다.
manualId: bluetape4k-coroutines
chapterId: structured-concurrency
---

# Structured concurrency 정책

Child failure를 어떻게 처리할지는 exception handler의 세부 구현이 아니라 **최종 결과의 의미**입니다. 모든 branch가 필요한지, 하나만 성공하면 되는지, 일부 결과도 유효한지를 먼저 정합니다.

![taskScope, firstSuccessTaskScope, supervisedTaskScope 정책 비교](../../../assets/coroutines/structured-policies.svg)

## 정책 선택

| Business result | API | Child failure | 결과 읽기 |
| --- | --- | --- | --- |
| atomic: 모두 필요 | `taskScope` / `failFastTaskScope` | unfinished sibling 취소 | `join().throwIfFailed()` 후 `get()` |
| any: 하나의 성공이면 충분 | `firstSuccessTaskScope` | 다른 성공 후보는 계속, winner 뒤 정리 | `join().result(mapper)` |
| partial: 일부 결과도 유효 | `supervisedTaskScope` | sibling 계속 | `results()`, `successfulResults()`, `failedExceptions()` |

세 helper는 virtual-thread dispatcher bridge에서 `StructuredTaskScope`를 실행합니다. Coroutine caller 취소가 scope 밖에 orphan task를 남겨서는 안 됩니다.

## Fail-fast: atomic composition

```kotlin
suspend fun loadCheckout(id: String): Checkout = taskScope("checkout") {
    val cart = fork { cartClient.load(id) }
    val price = fork { pricingClient.calculate(id) }
    val stock = fork { inventoryClient.reserve(id) }

    join().throwIfFailed()
    Checkout(cart.get(), price.get(), stock.get())
}
```

하나라도 실패하면 전체 checkout result가 무효라는 계약입니다. `throwIfFailed()` 전에 subtask `get()`을 호출하거나 failure를 개별적으로 삼키지 않습니다.

## First-success: replica/provider fallback

```kotlin
suspend fun resolveAddress(query: String): Address = firstSuccessTaskScope {
    fork { primary.resolve(query) }
    fork { secondary.resolve(query) }
    fork { archive.resolve(query) }
    join().result { cause -> AddressNotFound(query, cause) }
}
```

첫 failure는 전체 failure가 아닙니다. 모든 provider가 실패했을 때만 mapper exception이 호출자에게 전달됩니다. 동일 side effect를 여러 provider에서 실행하면 중복 실행과 idempotency 문제를 먼저 해결해야 합니다.

## Supervised: partial result

```kotlin
suspend fun loadWidgets(userId: String): List<Result<Widget>> =
    supervisedTaskScope<Widget, List<Result<Widget>>>("widgets") {
        fork { recommendations.load(userId) }
        fork { messages.load(userId) }
        fork { promotions.load(userId) }
        join()
        results()
    }
```

Supervision은 실패 은폐가 아닙니다. API response에서 실패 widget을 어떻게 표현할지, metric/log에서 어떤 exception을 남길지 함께 정의합니다.

## Async bridge

`CoroutineScope.asyncTaskScope`와 `asyncSupervisedTaskScope`는 즉시 `Deferred`를 반환합니다. 두 structured scope를 더 큰 coroutine composition 안에서 병렬 실행할 때 사용합니다. 반환 deferred의 owner는 receiver `CoroutineScope`입니다.

## 검증 체크리스트

1. 한 branch failure 뒤 sibling state가 policy와 맞는가.
2. first-success에서 첫 failure가 아니라 첫 성공이 선택되는가.
3. 모든 provider failure가 domain exception으로 매핑되는가.
4. supervised result가 성공과 실패를 모두 노출하는가.
5. parent cancellation과 deadline 뒤 unfinished task가 남지 않는가.
6. virtual-thread 호환 JDK/runtime 조건을 배포 환경에서 충족하는가.

## Source와 representative tests

- [`StructuredConcurrency.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/StructuredConcurrency.kt)
- [`StructuredConcurrencyTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/StructuredConcurrencyTest.kt)
- Core virtual-thread scope 구현은 `bluetape4k-core`의 concurrency package가 제공합니다.

선택한 정책을 production에서 관찰하고 종료하는 방법은 [운영과 관측 가능성](./operations.md)에서 다룹니다.
