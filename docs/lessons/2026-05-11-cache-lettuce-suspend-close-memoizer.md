# Cache Lettuce Suspend Close 및 Memoizer 복구 Lessons

## Context

`cache/cache-lettuce` 모듈의 6-Tier 코드 리뷰, 누락 테스트 보강, 공개 API KDoc/README 정비를 진행했다. 핵심 범위는 `LettuceSuspendJCache`, `LettuceSuspendCacheManager`, `LettuceSuspendNearCache`, `LettuceSuspendMemoizer`의 suspend lifecycle 계약이었다.

## Decision or Finding

P0는 없었다. P1은 두 가지였다.

- `runCatching` 기반 close 정리는 `CancellationException`을 일반 실패처럼 삼킬 수 있다. suspend API의 close 경로에서는 취소 신호를 명시적으로 재전파해야 한다.
- `LettuceSuspendJCache.close()` KDoc이 Redis 데이터를 지운다고 설명했지만 실제 JCache 계약은 resource close와 data clear를 분리한다. 공개 API 문서가 구현과 다르면 사용자에게 데이터 삭제/보존 계약을 잘못 전달한다.

추가로 확인한 P2는 다음과 같다.

- 매니저 close 중 일부 cache close가 실패하거나 취소 예외를 던져도 나머지 cache close를 먼저 시도해야 한다.
- memoizer의 in-flight `Deferred`는 영구 캐시 값이 아니라 동시성 조율 상태다. evaluator 실패나 취소가 in-flight에 남으면 같은 key의 다음 호출이 복구되지 못한다.
- 예제의 transient failure는 실제로 두 번째 호출에서 성공하는 형태여야 한다. 같은 key를 계속 실패시키는 예제는 복구 계약을 설명하지 못한다.

## Outcome

- `LettuceSuspendNearCache.close()`는 resource별 close 실패를 로그로 남기되 `CancellationException`은 삼키지 않도록 바꿨다.
- `LettuceSuspendCacheManager.close()`는 `NonCancellable` 정리 구간에서 등록 cache close를 끝까지 시도하고, 개별 cache close가 `CancellationException`을 명시적으로 던진 경우 잔여 정리 후 재전파하도록 정리했다.
- `LettuceSuspendJCache.close()` KDoc과 README는 `close()`가 데이터를 삭제하지 않는다는 JCache 계약을 기준으로 맞췄다.
- `LettuceSuspendMemoizer`는 evaluator 실패, 명시적 cancellation, 실제 `Job.cancel()` 경로 모두에서 in-flight 항목이 제거되고 같은 key가 새 계산으로 복구되는 테스트를 갖게 됐다.

## Verification

- `./gradlew :bluetape4k-cache-lettuce:compileKotlin :bluetape4k-cache-lettuce:compileTestKotlin :bluetape4k-cache-lettuce:test --tests '*LettuceSuspendMemoizerTest'` 통과.
- `./gradlew :bluetape4k-cache-lettuce:test` 통과: 353 passing, 4 pending.
- `git diff --check` 통과.
- 외부 6-Tier 최종 게이트 결과: P0 없음, P1 없음.

## Future Guidance

- suspend close 경로에서 `runCatching`을 사용할 때는 `CancellationException`이 삼켜지는지 먼저 확인한다. 필요하면 `try/catch`로 분해해 취소 신호를 명시적으로 재전파한다.
- close가 resource 해제인지 data 삭제인지 KDoc, README, 테스트에서 같은 용어로 고정한다. `close()`와 `clear()/destroy()`의 계약을 섞지 않는다.
- memoizer 복구 테스트는 실패 예외, 명시적 `CancellationException`, 실제 `Job.cancel()`을 분리해 둔다. 세 경로는 모두 in-flight cleanup을 검증하지만, 실패 원인이 다르다.
- transient failure 예제는 반드시 재시도에서 성공하도록 작성한다. 그래야 사용자가 실패 결과를 캐시하지 않는다는 계약을 바로 이해할 수 있다.
