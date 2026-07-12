---
title: Deferred 조정
description: 첫 완료, 첫 성공, loser 취소를 서로 다른 정책으로 구현합니다.
manualId: bluetape4k-coroutines
chapterId: deferred
---

# Deferred 조정

여러 비동기 작업 가운데 “가장 빠른 것”을 고른다는 말에는 두 의미가 있습니다. 먼저 **종료**한 작업은 실패일 수 있지만, 먼저 **성공**한 작업은 성공 값을 반환해야 합니다. loser를 취소할지도 별도의 결정입니다.

![awaitAny, awaitAnyAndCancelOthers, firstSuccessTaskScope 비교](../../../assets/coroutines/deferred-race-policy.svg)

## 선택표

| 요구 | API | 첫 failure | loser |
| --- | --- | --- | --- |
| 첫 종료 결과만 관찰 | `awaitAny` | 즉시 전파 | 계속 실행 |
| 첫 종료 결과를 채택하고 capacity 회수 | `awaitAnyAndCancelOthers` | 즉시 전파 | 취소 |
| 실패 replica를 건너뛰고 첫 성공 채택 | `firstSuccessTaskScope` | 다른 branch 대기 | winner 뒤 취소 |
| 두 결과가 모두 필요 | `zip` / `zipWith` | 전파 | 구조화된 parent 정책 |

빈 입력은 `IllegalArgumentException`입니다. 하나만 있으면 해당 `Deferred.await()`와 같은 의미입니다.

## 첫 완료와 loser 취소

다음 코드는 세 요청을 **같은 caller scope**에서 시작합니다. coordination helper가 새 장기 수명주기를 만들지 않습니다.

```kotlin
suspend fun <T> fastestReplica(
    replicas: List<suspend () -> T>,
): T = coroutineScope {
    replicas
        .map { load -> async { load() } }
        .awaitAnyAndCancelOthers()
}
```

구현은 각 `Deferred.await()` 결과를 `runCatching`으로 감싼 signal을 만들고, 가장 먼저 끝난 signal의 index를 선택합니다. winner 결과를 반환하거나 다시 던지기 전에 나머지 signal과 원본 deferred에 `cancel()`을 시도합니다.

중요한 계약은 다음과 같습니다.

- 첫 작업이 성공하면 그 값을 반환하고 loser를 취소합니다.
- 첫 작업이 실패하면 그 예외를 던지고 loser를 취소합니다.
- 첫 작업이 이미 취소됐다면 그 취소를 전파하고 loser를 취소합니다.
- coordinating caller 자체가 취소되면 `ensureActive()`가 caller cancellation을 내부 결과처럼 오인하지 않게 합니다.

## 첫 성공

실패 replica를 건너뛰어야 한다면 first-completion helper가 아닙니다.

```kotlin
suspend fun loadFromAnyProvider(): Quote = firstSuccessTaskScope {
    fork { primary.loadQuote() }
    fork { secondary.loadQuote() }
    fork { archive.loadQuote() }
    join().result { cause ->
        QuoteUnavailable("all providers failed", cause)
    }
}
```

첫 성공 뒤 나머지 task는 정리됩니다. 모든 task가 실패하면 `result`에 제공한 mapper가 domain exception을 만듭니다.

## DeferredValue를 소유할 때

`DeferredValue`는 생성과 동시에 자체 `DefaultCoroutineScope`에서 계산을 시작합니다. 따라서 단순한 caller-owned `async`와 달리 owner가 `close()`해야 합니다.

```kotlin
suspend fun loadAnswer(): Int {
    val value = deferredValueOf { repository.loadAnswer() }
    return try {
        value.await()
    } finally {
        value.close()
    }
}
```

Coroutine 안에서는 blocking `value` property 대신 `await()`를 사용합니다. 제한된 dispatcher에서 blocking access는 starvation 또는 deadlock을 만들 수 있습니다.

## 변환과 결합

`Deferred.map`, `mapAll`, `concatMap`, `zipWith`는 결과를 새 `Deferred`로 표현합니다. source failure/cancellation과 transform exception은 새 deferred에 그대로 전파됩니다.

```kotlin
val profile = async { loadProfile(id) }
val quota = async { loadQuota(id) }
val summary = profile.zipWith(quota) { p, q -> Summary(p, q) }
return summary.await()
```

helper를 호출한다고 source deferred의 owner가 바뀌지는 않습니다. 시작한 scope가 여전히 lifecycle을 소유합니다.

## 운영 점검

| 신호 | 이유 |
| --- | --- |
| winner latency와 winner 종류 | 특정 replica가 항상 먼저 실패하는 문제 탐지 |
| loser cancellation 완료 시간 | 취소를 지원하지 않는 I/O 탐지 |
| race당 in-flight 상한 | retry와 race가 곱해지는 폭증 방지 |
| all-failed 비율 | first-success가 장애를 가리는지 확인 |

## Source와 representative tests

- [`DeferredSupport.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/support/DeferredSupport.kt)
- [`DeferredValue.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DeferredValue.kt)
- [`DeferredSupportTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/support/DeferredSupportTest.kt)
- [`StructuredConcurrencyTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/StructuredConcurrencyTest.kt)

여러 값이 아니라 stream item의 순서와 병렬도를 조정하려면 [순서 보장과 병렬 Flow](./flow.md)로 이어집니다.
