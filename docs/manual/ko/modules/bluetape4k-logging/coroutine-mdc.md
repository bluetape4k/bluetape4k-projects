---
title: Coroutine MDC 전파
description: MDCContext로 suspension과 dispatcher 전환을 건너 correlation context를 유지합니다.
manualId: bluetape4k-logging
chapterId: coroutine-mdc
---

# Coroutine MDC 전파

Plain MDC scope 안에서 coroutine이 다른 thread로 이동하면 thread-local 값만으로는 context가 따라가지 않습니다. `withCoroutineLoggingContext`는 scoped MDC와 `kotlinx-coroutines-slf4j`의 `MDCContext`를 결합합니다.

## 완전한 경계

```kotlin
suspend fun handle(requestId: String) =
    withCoroutineLoggingContext(
        "requestId" to requestId,
        "component" to "checkout",
    ) {
        coroutineScope {
            val stock = async(Dispatchers.IO) { inventory.load() }
            val price = async(Dispatchers.Default) { pricing.load() }
            log.info { "Composing checkout" }
            stock.await() to price.await()
        }
    }
```

helper는 `withContext(MDCContext())` 안에서 block을 실행합니다. child coroutine은 parent context를 상속하므로 dispatcher가 달라도 snapshot이 전달됩니다.

## 중요한 snapshot 규칙

`MDCContext`는 context 생성 시점의 MDC를 capture합니다. block 안에서 plain `MDC.put`만 호출한 뒤 다음 suspension에도 자동 반영되리라 기대하면 안 됩니다. 새로운 값을 지속해서 전달하려면 새 helper scope 또는 새 `MDCContext` boundary를 만듭니다.

## 복원 정책

`restorePrevious=true`는 동기 helper와 같은 nested restore 계약을 사용합니다. `false`이면 적용한 non-null key 목록을 block 바깥 `finally`에서도 제거해 caller thread의 잔여 값을 정리합니다.

## 선택 기준

| 코드 | 선택 |
| --- | --- |
| suspension 없는 동기 block | `withLoggingContext` |
| suspend/async/dispatcher 전환 | `withCoroutineLoggingContext` |
| framework가 이미 trace MDC를 관리 | 중복 key ownership을 피하고 framework lifecycle 확인 |

## Source와 tests

- [`MdcSupportCoroutines.kt`](../../../../../bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/coroutines/MdcSupportCoroutines.kt)
- [`MdcSupportCoroutinesTest.kt`](../../../../../bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/coroutines/MdcSupportCoroutinesTest.kt)

비동기 emission 자체가 필요하면 [Async channel](./async-channel.md)의 추가 수명주기를 먼저 확인합니다.
