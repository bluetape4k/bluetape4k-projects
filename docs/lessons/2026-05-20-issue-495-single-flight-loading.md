# 이슈 495 Single-flight Loading

## 배경

Issue #495는 memoizer implementation에서 같은 key의 in-flight loading을 재사용하는 primitive를
도입했다. 첫 migration slice는 cache-core의 in-memory sync, async, suspend memoizer를 대상으로 했다.

## 결정

새 public API가 아니라 internal `SingleFlight` primitive를 추가한다. Blocking call, `CompletableFuture`,
suspend call은 별도 coordination path를 유지하면서 `clear()`를 위한 generation-token contract를 공유한다.

Migrated memoizer는 captured token이 여전히 current일 때만 computed value를 write한다. `clear()` 전에
evaluator를 시작한 caller는 계산 결과를 받지만, stale result는 cache를 다시 채우지 않는다.

## 결과

- `InMemoryMemoizer`는 cache miss에서 더 이상 `ConcurrentHashMap.getOrPut`을 쓰지 않아 active miss 중
  duplicate same-key evaluator execution을 피한다.
- `InMemoryAsyncMemoizer`는 null future failure behavior를 유지하면서 in-flight와 generation handling을
  `SingleFlight`에 위임한다.
- `InMemorySuspendMemoizer`는 same-key coordination을 `SingleFlight`에 위임하고 failed/cancelled work가
  retryable하도록 유지한다.
- Same-key coalescing, clear-during-flight, null Java future completion, suspend cancellation cleanup을
  다루는 focused test를 추가했다.

## 검증

통과:

```bash
./gradlew :bluetape4k-cache-core:compileKotlin :bluetape4k-cache-core:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --tests '*SingleFlightTest' --tests '*InMemory*MemoizerTest' --no-configuration-cache
./gradlew :bluetape4k-cache-core:test --no-configuration-cache
git diff --check
```

Full cache-core result: 464 passing.

## 향후 가이드

- Backend memoizer에 또 다른 local `inFlight` + generation implementation을 추가하기 전에 `SingleFlight`를 재사용한다.
- In-flight map이나 cache를 비우기 전에 generation을 증가시킨다.
- Concurrency test에서는 짧은 latch timeout을 피한다. Full-suite load는 올바른 test도 scheduling race로 만들 수 있다.
