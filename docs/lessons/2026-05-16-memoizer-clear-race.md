# Memoizer Write-after-clear 경쟁 조건 수정

**날짜**: 2026-05-16
**이슈**: #487
**브랜치**: `fix/memoizer-clear-race`

## 근본 원인

Async 및 suspend memoizer는 공통 버그를 공유했다. `clear()` 호출 이후 완료된 in-flight
evaluator가 결과를 cache에 다시 써서 stale data로 cache를 다시 채웠다. `clear()`를 호출해
cache를 무효화한 caller는 바로 다음 호출에서 fresh result를 얻었지만, `clear()` 전에 평가를
시작한 다른 caller가 조용히 cache를 다시 채울 수 있었다.

부차적인 버그도 `CaffeineMemoizer.clear()`에 있었다. 실제 eviction인 `cache.invalidateAll()`이
아니라 maintenance pass인 `cache.cleanUp()`을 호출해 `clear()`가 사실상 no-op이었다.

## 수정

### Generation counter 패턴

영향받은 memoizer 5개에 `AtomicLong generation` counter를 추가했다:

1. evaluator가 시작되기 전(또는 in-flight future가 공유되기 전) `generation.get()`을
   `capturedGen`으로 capture한다.
2. `clear()`는 먼저 `generation.incrementAndGet()`을 호출한 뒤 cache와 in-flight map을 비운다.
3. 평가 후 cache에 쓰기 전에 `generation.get() == capturedGen`인지 확인한다. 다르면 결과가
   `clear()` 이전에 계산된 것이므로 write를 건너뛴다. 단 caller의 future/deferred는 계산된
   값으로 계속 완료한다.

### Value-aware `ConcurrentHashMap.remove`

In-flight map cleanup은 이전 generation의 completion과 경쟁하는 freshly-installed promise를
제거하지 않도록 2-argument `remove(key, value)` form을 사용한다.

### Null future completion

`InMemoryAsyncMemoizer`와 Caffeine/EhCache/JCache variant는 evaluator의 `CompletableFuture`가
`null` value로 완료될 때 `completeExceptionally(NullPointerException(...))`를 호출한다.
이렇게 해서 영원히 완료되지 않는 promise를 방지한다.

### `CaffeineMemoizer.clear()` 버그

`cache.cleanUp()`을 `cache.invalidateAll()`로 바꿨다. maintenance call만 보호하던 불필요한
`ReentrantLock`도 제거했다.

### 영향받은 파일

| 파일 | 변경 |
|------|--------|
| `CaffeineAsyncMemoizer.kt` | generation counter, value-aware remove, null check, `invalidateAll()` |
| `EhCacheAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `JCacheAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `InMemoryAsyncMemoizer.kt` | generation counter, value-aware remove, null check |
| `Cache2kSuspendMemoizer.kt` | generation counter, value-aware remove |
| `CaffeineMemoizer.kt` | `clear()`가 `invalidateAll()` 사용, lock 제거 |

## 테스트 범위

`CaffeineAsyncMemoizerTest`와 `Cache2KSuspendMemoizerTest`에 새 테스트를 추가했다:

- `clear 후 캐시가 무효화된다` — basic clear가 cache를 무효화하고 fresh evaluation을 유발함.
- `clear 중 진행 중인 비동기 결과는 캐시에 저장되지 않는다` — in-flight result가
  `clear()` 후 다시 쓰이지 않음(`getIfPresent`가 null을 반환하는 것으로 검증).
- `clear 중 진행 중인 suspend 결과는 캐시에 저장되지 않는다` — suspend memoizer의 동일 보장
  검증(두 번째 evaluation이 trigger되는 것으로 확인).

## 검증

```
447 passing (40.8s) — BUILD SUCCESSFUL
```

## 핵심 교훈

**generation은 clear 이후가 아니라 이전에 증가시킨다.** `clear()`가 cache를 비운 다음
generation을 증가시키면 concurrent evaluator가 아직 old generation을 보고 equality를 통과한 뒤
increment 완료 이후 write할 수 있다. 올바른 순서는 increment -> clear이다.

**`cache.cleanUp()`은 `cache.invalidateAll()`이 아니다.** Caffeine의 `cleanUp()`은 pending
maintenance task(expiry sweep 등)를 trigger할 뿐 모든 entry를 evict하지 않는다. cache를 비우려는
의도라면 항상 `invalidateAll()`을 사용한다.
