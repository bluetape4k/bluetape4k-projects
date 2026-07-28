# Micrometer suspend try* wrapper의 코루틴 취소 삼킴

**날짜:** 2026-05-16
**이슈:** #488
**브랜치:** `fix/micrometer-cancellation-swallow`
**PR:** (pending)

---

## 근본 원인

`tryObserveSuspending`과 `tryWithObservationSuspending`은 전체 body를 `runCatching {}`으로
감쌌다. `runCatching`은 넓은 `try { } catch (e: Throwable) { Result.failure(e) }`로 구현되어
`CancellationException`을 catch한 뒤 rethrow하지 않고 `Result.failure(CancellationException)`으로
반환한다.

Inner helper(`withObservationContextSuspending`)는 이미 `CancellationException`을 올바르게
rethrow했다(lines 196-198, 235-237). 그러나 outer `runCatching`이 이 rethrow를 가로채 삼켰고,
structured concurrency를 위반했다.

---

## 결정

취소와 실패를 구분하는 명시적인 try/catch로 `runCatching`을 교체한다:

```kotlin
return try {
    Result.success(innerCall() ?: throw NoSuchElementException())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
```

`CancellationException`은 `IllegalStateException`의 subtype이고 이는 다시 `Throwable`의 subtype이므로
`catch (CancellationException)` block은 반드시 `catch (Throwable)`보다 앞에 와야 한다. 순서가 중요하다.

---

## 결과

- `tryObserveSuspending`과 `tryWithObservationSuspending` 모두 `CancellationException`을
  parent job으로 전파한다.
- `Result.failure`는 cancellation이 아닌 `Throwable`에 대해서만 반환된다.
- 테스트 9개 통과(기존 7개 + 신규 cancellation regression test 2개).

---

## 검증 증거

```
io.bluetape4k.micrometer.observation.coroutines.ObservationCoroutinesSupportTest ✔ tryObserveSuspending - cancellation propagates to parent job
io.bluetape4k.micrometer.observation.coroutines.ObservationCoroutinesSupportTest ✔ tryWithObservationSuspending - cancellation propagates to parent job
9 passing (2.3s)
```

---

## 향후 가이드

- **`runCatching {}`은 `suspend` 호출을 감싸면 안 된다.** `CancellationException`을 조용히
  `Result.failure`로 바꿔 structured concurrency를 깨뜨린다. 명시적인 `try/catch`를 사용한다.
- **Catch 순서:** `CancellationException`이 `Throwable`보다 항상 먼저 와야 한다.
- **Cancellation propagation test 패턴:**
  ```kotlin
  var cancelled = false
  var result: Result<T>? = null
  val job = launch {
      try { result = suspendWrapper() }
      catch (e: CancellationException) { cancelled = true; throw e }
  }
  yield(); job.cancel(); job.join()
  cancelled shouldBeEqualTo true
  result shouldBeEqualTo null  // Result.failure를 반환하면 안 됨
  ```
  `cancelled == true`와 `result == null`을 모두 assertion해야 전체 contract가 고정된다.
