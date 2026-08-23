# #1447 NearJCache 조건부 mutation의 back-first 계약

## 맥락

`NearJCache`의 blocking `putIfAbsent`, `remove`, `remove(key, oldValue)`,
`replace`가 front cache 결과를 먼저 사용하고, 그 결과가 성공일 때만 back
cache를 조회하거나 변경하고 있었다. 정상적인 front eviction이나 두 번째
NearCache 인스턴스에서는 front가 miss이고 back만 값을 보유할 수 있으므로,
이 순서는 JCache 반환값과 논리적인 2-tier 상태를 불일치시켰다.

## 결정

- 네 조건부 연산은 JCache back provider의 원자 API를 먼저 호출한다.
- blocking 반환값은 front가 아니라 back 원자 결과로 결정한다. 결과를 반환해야
  하므로 `NearJCacheConfig.isSynchronous=false`여도 해당 back 호출은 완료될
  때까지 기다린다.
- back 성공 후 성공 결과는 front에 반영하고, 조건 불충족 결과는 front key를
  제거해 stale 값을 남기지 않는다.
- back 호출이 실패하면 front mutation을 수행하지 않는다. back commit 후 front
  보정이 실패하면 front key를 invalidate하고 원래 예외를 전달한다.
- `SuspendNearJCache`의 기존 back-first 일반 mutation 계약과 같은 기준을
  blocking API에도 적용하되, blocking provider 호출의 timeout/cancellation은
  `NearJCache`의 동기 write 경계를 따른다.

## 결과

front miss/back hit 상태에서 `putIfAbsent`, `remove`, 두 `replace` overload가
back 상태와 같은 성공·실패 결과를 반환하고, 성공 후 front/back 값이 같은
상태로 수렴한다. 조건부 mutation 경합에서는 back 원자 연산이 성공자를
결정하며, 실패한 호출의 front invalidate가 남은 stale 값을 노출하지 않는다.

## 검증

- RED: front `putIfAbsent`가 `true`여도 back `putIfAbsent`가 `false`이면
  blocking API가 `true`를 반환하던 회귀 테스트가 실패했다.
- GREEN: `NearJCacheConditionalMutationContractTest`에서 front miss/back hit
  4개 연산, back 실패·cancellation, front 보정 실패·invalidate, 동시
  `putIfAbsent`를 검증했다.
- 기존 `NearJCacheWriteThroughFailureTest`,
  `NearJCacheWriteThroughReentrancyTest`,
  `NearJCacheOperationStatisticsTest`를 새 back-first 호출 경계에 맞춰
  갱신하고 통과시켰다.
- `./gradlew :bluetape4k-cache-core:test --rerun-tasks`로 전체 모듈 회귀를
  확인했고 `711 passing`, `BUILD SUCCESSFUL`을 기록했다.
- `:bluetape4k-cache-hazelcast:test`의 `HazelcastNearJCacheTest` 6개,
  `:bluetape4k-cache-lettuce:test`의 NearJCache 회귀 74개,
  `:bluetape4k-cache-redisson:test`의 `RedissonNearJCacheTest` 73개가 모두
  통과했다.
- `./gradlew :bluetape4k-cache-core:detekt --rerun-tasks`와
  `./gradlew :bluetape4k-cache-core:build -x test --rerun-tasks`가 모두
  `BUILD SUCCESSFUL`로 완료됐다. detekt 출력의 기존 `magic number`,
  `LargeClass`, `ReturnCount` 항목은 이번 변경 파일이 아닌 기존 코드의
  비차단 경고다.

## 놓치기 쉬운 점과 후속 guard

NearCache의 front는 빠른 복사본이지 조건부 mutation의 기준 데이터 원본이
아니다. 새 blocking 조건부 연산을 추가할 때 front read/compare/write 조합을
사용하지 말고, back provider가 제공하는 원자 API를 먼저 호출한 뒤 front를
reconcile한다. back 결과가 false인 경우에도 front를 그대로 보존하지 말고
invalidate하여 다음 read가 back의 논리값을 다시 채우게 한다. back commit 뒤
front 반영 실패와 provider cancellation도 별도 계약 테스트로 유지한다.
