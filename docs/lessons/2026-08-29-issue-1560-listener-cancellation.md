# #1560 SuspendJCache listener 취소 경합에서 permit 보존

## 배경

`SuspendJCacheEntryEventListener`는 callback마다 `Semaphore` permit을 확보한 뒤
`CoroutineStart.LAZY` child를 시작합니다. 기존 구현에는 child body의 `finally`,
`job.start() == false`, submit의 외부 `finally` 반환 경로가 있었지만,
`start()`가 `true`를 반환한 직후 parent scope가 취소되어 body에 진입하지 못하는
경우를 completion 경로가 보완하지 못했습니다. 이때 permit이 누수되고 다음
callback이 불필요하게 admission 거부를 받았습니다.

## 결정 또는 발견

- callback마다 `AtomicBoolean` release guard를 두고 body `finally`, completion
  handler, `start() == false`, submit 예외 경로가 같은 release-once 함수를
  사용합니다.
- permit을 확보한 callback은 `admitted` 분모에 기록하고, 정상 완료·취소·실패
  중 하나의 terminal outcome만 기록합니다. close 경합으로 실행되지 못한
  callback은 `cancelled`로 종결합니다.
- admission 전 조기 거부(`closed` 또는 target cache closed)는 기존처럼
  `ignored`로 분리해 이벤트 복사본과 metric 의미를 섞지 않습니다.

## 결과

body 진입 전 취소에서도 permit과 `inFlightCallbacks`가 정확히 복구됩니다.
관측 결과는 admission과 terminal outcome을 구분하며, cancellation/completion
경합에서 동일 callback을 두 번 세지 않습니다. 변경 범위는
`cache/cache-core` listener와 회귀 테스트로 한정했습니다.

## 놓친 점

초기 분석은 `job.start() == false`만 확인하면 취소된 lazy child를 모두 처리할 수
있다고 가정했습니다. 실제 결함은 `start() == true` 이후 dispatcher가 body를
실행하기 전 parent cancellation이 발생하는 경합이었고, child completion을
항상 관찰하는 별도 release 경로가 필요했습니다.

## 근거 원본

- 이슈: [#1560](https://github.com/bluetape4k/bluetape4k-projects/issues/1560)
- 구현: `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt`
- 회귀: `cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListenerTest.kt`
- 최신 listener JUnit 결과: `cache/cache-core/build/test-results/test/TEST-io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest.xml`
- 워크플로 실행: `20260829T054112Z-9fda19f7` (로컬 `.bluetape` receipt)

## 검증

- RED: `start true 후 body 진입 전 취소도 permit을 정확히 반환한다`가 수정 전
  두 번째 callback의 `admittedCallbacks == 1`로 실패했습니다.
- GREEN: 해당 회귀와 정상 terminal outcome 테스트 2개 통과.
- listener 테스트 25개를 `--rerun-tasks --no-build-cache`로 3회 반복해 모두
  통과했습니다.
- `:bluetape4k-cache-core:cleanTest :bluetape4k-cache-core:test --no-build-cache`
  에서 719개 통과했습니다(`skipped=0`, `failures=0`, `errors=0`). listener JUnit
  XML도 `tests=25`, `skipped=0`, `failures=0`, `errors=0`을 기록합니다.
- `:bluetape4k-cache-core:detekt`와 `git diff --check`가 통과했습니다. detekt
  출력의 기존 finding은 변경 파일 외부에 남아 있으며, 현재 변경 파일에는
  finding이 없습니다.

## 향후 지침

`CoroutineStart.LAZY` callback이 permit, semaphore, connection 같은 외부
자원을 소유하면 body 진입 여부와 무관하게 completion callback을 포함한
release-once 경로를 둡니다. 취소 회귀 테스트는 `start() == false`와
`start() == true` 후 body 진입 전 취소를 모두 포함하고, 후속 admission과
`inFlight == 0`을 함께 검증합니다.
