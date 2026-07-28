# Future Wrapper Executor

**날짜:** 2026-05-20
**이슈:** #541
**브랜치:** `perf/issue-541-future-wrapper`

---

## 배경

`FutureToCompletableFutureWrapper`는 일반 `Future<T>` instance를 변환할 때 wrapper instance마다 새
virtual-thread builder를 만들고 시작했다. 이는 blocking `Future.get()` semantics를 보존했지만
불필요한 allocation을 만들었고, diagnostics에서 모든 watcher thread가 같은 `future-wrapper` 이름으로
보이게 했다.

## 결정

Named factory를 가진 shared virtual-thread-per-task executor 하나를 사용한다:

- Watcher task는 `Executors.newThreadPerTaskExecutor(...)`를 통해 실행한다.
- Thread name은 per-thread numbering을 가진 `future-wrapper-` prefix를 사용한다.
- Executor는 기존 core lifecycle pattern에 맞춰 `ShutdownQueue`에 등록한다.
- `cancel()`은 여전히 wrapped `Future`를 먼저 취소한 뒤 watcher task와 wrapper `CompletableFuture`를
  취소한다.

Plain `Future.get()` adapter라서 active blocking wait마다 virtual thread 하나를 쓰는 것은 의도적이다.
Callback API가 없다면 wait thread를 피하려면 polling scheduler나 bounded platform-thread pool이
필요한데, 여기서는 둘 다 더 나쁜 tradeoff다.

## 결과

Wrapper는 더 이상 conversion마다 virtual thread를 직접 build/start하지 않으며, 기존 success, exception,
cancellation behavior는 유지된다. 새 test는 named virtual watcher와 wrapped-future cancellation
contract를 고정한다.

## 검증 증거

- `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.concurrent.FutureSupportTest' --no-configuration-cache`
- `./gradlew :bluetape4k-core:test --no-configuration-cache`

## 향후 가이드

Blocking `Future` API를 `CompletableFuture`로 adapter할 때 watcher execution은 shared
lifecycle-managed executor 뒤에 두고 wrapped resource로의 cancellation propagation을 보존한다.
Wall-clock performance를 assert하는 test는 피하고, thread naming, virtual-thread usage, completion
behavior, cancellation behavior 같은 deterministic contract를 선호한다.
