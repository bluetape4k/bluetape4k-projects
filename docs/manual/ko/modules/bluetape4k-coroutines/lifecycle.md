---
title: 수명주기와 취소
description: Scope 소유권, 취소 전파, dispatcher 종료를 코드와 종료 순서로 설계합니다.
manualId: bluetape4k-coroutines
chapterId: lifecycle
---

# 수명주기와 취소

Coroutine을 시작하는 위치보다 **누가 끝내는지**가 더 중요합니다. 요청과 함께 끝나야 하는 작업을 component scope로 탈출시키거나, component가 만든 dispatcher를 닫지 않으면 취소 누락과 thread leak이 발생합니다.

![CloseableCoroutineScope의 생성, 자식 취소, dispatcher 종료 순서](../../../assets/coroutines/scope-lifecycle.svg)

## 두 가지 소유권

| 소유자 | 기본 선택 | 종료 시점 |
| --- | --- | --- |
| HTTP 요청, message 처리, CLI command | 호출자가 제공한 `CoroutineScope`, `coroutineScope` | 호출 반환 또는 caller 취소 |
| 여러 호출 사이에 유지되는 component | `CloseableCoroutineScope` 구현 | component `close()` 또는 framework shutdown hook |

요청 안에서 두 API를 병렬 호출할 뿐이라면 새 scope가 필요하지 않습니다.

```kotlin
suspend fun loadDashboard(id: String): Dashboard = coroutineScope {
    val profile = async { profileClient.load(id) }
    val notices = async { noticeClient.load(id) }
    Dashboard(profile.await(), notices.await())
}
```

부모가 취소되면 두 child가 함께 취소되고, 함수가 반환될 때 child가 남지 않습니다.

## Closeable scope 계약

`CloseableCoroutineScope`는 `_closed`와 `_cancelled`를 atomic CAS로 보호합니다.

1. 첫 `close()`가 `scopeClosed=true`로 바꿉니다.
2. `clearJobs()`가 먼저 `cancelChildren(cause)`를 호출합니다.
3. 이어서 scope context 자체를 `cancel(cause)`합니다.
4. 이후 `close()`와 `clearJobs()`는 상태를 되돌리거나 취소를 반복하지 않습니다.

`DefaultCoroutineScope`는 `Dispatchers.Default + SupervisorJob()`을 소유합니다. `SupervisorJob`은 형제 하나의 실패가 다른 형제를 즉시 취소하지 않게 하지만, owner가 `close()`하면 전체 child가 정리됩니다.

```kotlin
class ThumbnailWorker : AutoCloseable {
    private val scope = DefaultCoroutineScope()

    fun submit(imageId: String): Job = scope.launch {
        thumbnailService.generate(imageId)
    }

    override fun close() = scope.close()
}
```

## 전용 dispatcher를 소유할 때

`ThreadPoolCoroutineScope(poolSize, name)`은 양수 `poolSize`를 검증하고 fixed thread pool과 `SupervisorJob`을 결합합니다. 종료 시 coroutine 취소만으로는 thread pool이 해제되지 않으므로 dispatcher도 닫습니다.

```kotlin
class BlockingAdapter : AutoCloseable {
    private val scope = ThreadPoolCoroutineScope(poolSize = 4, name = "legacy-io")

    suspend fun <T> call(block: () -> T): T =
        withContext(scope.coroutineContext) { block() }

    override fun close() = scope.close()
}
```

`ThreadPoolCoroutineScope.close()`는 상위 `close()` 뒤 별도 CAS guard로 `dispatcher.close()`를 한 번만 수행합니다. `poolSize <= 0`은 생성 시 `IllegalArgumentException`입니다.

## CancellationException 처리

취소는 일반적인 실패가 아니라 구조화된 종료 신호입니다. broad catch가 필요하면 취소를 먼저 재전파합니다.

```kotlin
try {
    remoteClient.load()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    fallback(e)
}
```

`clearJobs(CancellationException("shutdown"))`에 전달한 cause는 child의 suspending point에서 관찰됩니다. 테스트는 child가 같은 message를 받은 뒤 parent `Job`이 inactive가 되는지 확인합니다.

## 테스트 패턴

전용 dispatcher를 만드는 테스트는 반드시 종료를 fixture에 포함합니다.

```kotlin
private val scopeLazy = lazy { ThreadPoolCoroutineScope(poolSize = 2) }

@AfterEach
fun closeScope() {
    if (scopeLazy.isInitialized()) scopeLazy.value.close()
}
```

`use {}`도 적합합니다. 핵심은 성공, 실패, assertion failure 모두에서 owner가 close를 실행하는 것입니다.

## 문제 진단

| 증상 | 확인할 경계 | 조치 |
| --- | --- | --- |
| 요청이 끝난 뒤 작업이 계속됨 | request child가 application/component scope로 탈출했는가 | caller scope 안에서 시작합니다. |
| shutdown 뒤 thread가 남음 | fixed/virtual dispatcher의 owner가 누구인가 | owner `close()`를 shutdown hook에 연결합니다. |
| child 하나의 실패가 전체 작업을 취소함 | `Job`과 `SupervisorJob` 중 무엇인가 | 결과가 독립적일 때만 supervision을 선택합니다. |
| timeout 뒤 remote I/O가 계속됨 | client가 coroutine cancellation을 socket/request 취소로 연결하는가 | client deadline과 idempotency도 함께 설계합니다. |

## Source와 검증 근거

- [`CloseableCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/CloseableCoroutineScope.kt)
- [`DefaultCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/DefaultCoroutineScope.kt)
- [`ThreadPoolCoroutineScope.kt`](../../../../../bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/ThreadPoolCoroutineScope.kt)
- [`AbstractCoroutineScopeTest.kt`](../../../../../bluetape4k/coroutines/src/test/kotlin/io/bluetape4k/coroutines/AbstractCoroutineScopeTest.kt)

다음 장에서는 owner가 시작한 여러 `Deferred` 가운데 winner와 loser cleanup을 어떻게 정의하는지 설명합니다: [Deferred 조정](./deferred.md).
