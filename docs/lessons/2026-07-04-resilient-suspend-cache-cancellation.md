# Resilient suspend cache cancellation

## 배경

이슈 #942는 `ResilientSuspendNearJCache`의 suspend back-cache read 주변에
`runCatching`이 있다는 점을 확인했다. 이 패턴은 coroutine cancellation을 configured
fallback behavior로 바꿨다.

## 결정

Suspend `runCatching` fallback path를 명시적인 `try/catch` block으로 바꾸고,
non-cancellation fallback behavior를 적용하기 전에 `CancellationException`을 다시
던진다.

## 결과

`get`, `getAll`, `replace`, `containsKey`는 이제 structured concurrency
cancellation을 보존한다. 기존 계약이 graceful degradation을 기대하는 일반 back cache
failure에서는 계속 null/false를 반환한다.

## 검증

- `./gradlew :bluetape4k-cache-core:test --tests 'io.bluetape4k.cache.nearcache.jcache.ResilientSuspendNearJCacheTest'`

## 향후 지침

Fallback behavior를 적용하기 전에 cancellation을 처리하고 다시 던질 수 없다면 suspend
cache call 주변에 `runCatching`을 사용하지 않는다.
