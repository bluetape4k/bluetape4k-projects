# 이슈 494 Cancellation Contract

날짜: 2026-05-20
이슈: #494

## 배경

여러 module의 coroutine wrapper는 `CancellationException` rethrow, cancellation 후 waiter slot cleanup,
Retrofit call 같은 하위 resource 취소를 재사용 가능한 방식으로 증명할 필요가 있었다.

## 결정

각 module에 launch/cancel scaffolding을 복사하지 않고 `bluetape4k-junit5`에 cancellation contract helper를
추가한다. Helper signature는 일반 coroutine test에서 쓸 수 있도록 core coroutine type과 `Duration`에
머무르게 한다.

## 결과

- Structured cancellation을 보존해야 하는 `Result` style wrapper를 위해 `resultOfNonCancellation`과
  `runCatchingNonCancellation` 추가.
- Propagation, cancelled waiter cleanup, resource cancellation을 위한 helper assertion 추가.
- `bluetape4k-coroutines`와 `bluetape4k-micrometer`의 local cancellation scaffolding 교체.
- `bluetape4k-retrofit2`에 실제 MockWebServer delayed-response cancellation test 추가.
- English/Korean JUnit5 README에 usage와 checklist 문서화.

## 검증

- `./gradlew :bluetape4k-junit5:compileKotlin :bluetape4k-junit5:test --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-coroutines:test --tests '*ResumableTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-micrometer:test --tests '*ObservationCoroutinesSupportTest' --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-retrofit2:test --tests '*SuspendRetrofitCallSupportTest' --console=plain --no-configuration-cache`
- `git diff --check`

## 향후 가이드

`Result`를 반환하는 suspend wrapper를 추가할 때 plain `runCatching`으로 suspend call을 감싸지 않는다.
`runCatchingNonCancellation`을 사용하거나, non-cancellation failure로 변환하기 전에
`CancellationException`을 명시적으로 rethrow한다.
