# 코루틴 취소 프리미티브 — 오래된 대기자와 취소되지 않는 Future

**날짜:** 2026-05-16
**이슈:** #483
**브랜치:** `fix/coroutine-cancellation-primitives`
**PR:** (pending)

---

## 근본 원인

### 버그 1 — `Resumable`의 오래된 대기자

`Resumable.await()`는 `suspendCancellableCoroutine` 내부에서 CAS로 `Continuation`을
`continuationRef`에 설치하지만, 기존 구현은 `invokeOnCancellation` 핸들러를 등록하지 않았다.

대기 중인 코루틴이 `resume()` 호출 전에 취소되면 continuation이 슬롯에 그대로 남았다.
이후 `await()` 호출은 원래 대기자가 이미 사라졌는데도 슬롯이 점유된 것으로 판단해
`IllegalStateException("Only one thread can await a Resumable")`을 던졌다.

### 버그 2 — `FutureToCompletableFutureWrapper`가 하위 Future를 취소하지 않음

`FutureToCompletableFutureWrapper`(`bluetape4k-core`)는 `future.get()`에서 blocking하는
가상 스레드로 일반 `Future<T>`를 감싼다. 이 클래스가 `cancel()`을 override하지 않아
코루틴 취소(`CompletableFuture.cancel()` 호출)는 wrapper 상태만 취소로 표시했다.
가상 스레드는 계속 `future.get()`에서 무기한 대기했고, 하위 작업이 누수되었다.

---

## 결정

**버그 1 수정** — CAS 성공 직후 `invokeOnCancellation`을 등록한다:

```kotlin
if (continuationRef.compareAndSet(current, cont)) {
    // 이 cont만 CAS로 정리한다. resume()이 이미 READY로 교체했다면 READY를 보존한다.
    cont.invokeOnCancellation {
        continuationRef.compareAndSet(cont, null)
    }
    break
}
```

핵심 불변식: `set(null)`이 아니라 `compareAndSet(cont, null)`을 사용해야 concurrent
`resume()`이 배치한 READY sentinel을 보존한다. `resume()`이 먼저 이기면 CAS 비교 대상은
READY이며 `cont`가 아니므로, 취소 핸들러는 no-op이 되고 READY는 영향을 받지 않는다.

**버그 2 수정** — `FutureToCompletableFutureWrapper`에서 `cancel()`을 override한다:

```kotlin
override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    wrapped.cancel(mayInterruptIfRunning)
    return super.cancel(mayInterruptIfRunning)
}
```

순서가 중요하다. 먼저 `wrapped`를 취소해 가상 스레드의 `future.get()`을 unblock한 뒤
`super.cancel()`을 호출한다. 가상 스레드는 `CancellationException` catch 블록에서
`cancel(true)`에 재진입할 수 있지만, 이미 취소된 상태에서 `CompletableFuture.cancel()`은
멱등적이므로 안전하다.

---

## 결과

- `Resumable`: 취소된 `await()`가 슬롯을 정리하므로 다음 `await()` + `resume()` 쌍이
  `IllegalStateException` 없이 성공한다.
- `FutureToCompletableFutureWrapper`: 코루틴 취소가 하위 `Future`까지 전파되어
  가상 스레드의 blocking 작업을 중단한다.
- 테스트 7개 통과(`Resumable` 기존 5개 + 신규 2개, `FutureSupport` 기존 4개 + 신규 1개).

---

## 검증 증거

```
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ correct state
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ cancelled await clears slot so subsequent await succeeds
io.bluetape4k.coroutines.flow.extensions.ResumableTest ✔ READY fast path still works after invokeOnCancellation change
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ Massive future as CompletableFuture in multi-threads
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ Massive Future as CompletableFuture in Coroutines
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ 취소된 Future는 await 시 CancellationException을 던진다
io.bluetape4k.coroutines.support.FutureSupportTest     ✔ awaitSuspending 취소 시 하위 Future도 취소된다
7 passing (2.1s)
```

---

## 리뷰 지적 해결

| 심각도 | 지적 | 해결 |
|----------|---------|------------|
| LOW | `Resumable.await()` KDoc 언어 혼용 | 전체 block을 영어로 통일 |
| LOW | `awaitSuspending()` KDoc 언어 혼용 | 전체 block을 영어로 통일 |
| LOW | test에서 `catch (CancellationException)` 후 rethrow 없음 | try/catch 전체 제거 |
| LOW | test의 magic `50.milliseconds` literal | `CANCEL_PROPAGATION_DELAY_MS` const로 추출 |

---

## 향후 가이드

- **`suspendCancellableCoroutine` + CAS install 패턴**: producer가 동시에 설정한 sentinel을
  덮어쓰지 않도록 `set(null)`이 아니라 `compareAndSet(cont, null)`로
  `invokeOnCancellation`을 항상 등록한다.
- **`invokeOnCancellation`은 등록 시점에 이미 코루틴이 취소된 경우 동기적으로 실행된다.**
  따라서 "너무 늦게 등록된" window가 없다.
- **READY fast path는 suspend하지 않는다.** CancellableCoroutine lambda 내부에서
  `cont.resumeWith()`가 실행되면 취소 window가 없으므로 `invokeOnCancellation` 등록이 필요 없다.
- **`CompletableFuture` wrapper가 background thread나 virtual thread를 실행해 감싼 resource에서
  blocking한다면 반드시 `cancel()`을 override해야 한다.** override를 생략하면 thread가 누수된다.
- **wrapper의 취소 순서**: 감싼 resource를 먼저 취소한 뒤 `super.cancel()`을 호출한다.
  wrapper의 가상 스레드가 catch 블록에서 `cancel()`에 재진입할 수 있지만,
  `CompletableFuture.cancel()`은 멱등적이므로 안전하다.
