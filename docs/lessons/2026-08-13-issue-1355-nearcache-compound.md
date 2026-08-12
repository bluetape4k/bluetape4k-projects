# NearJCache compound operation의 back 원자성 보장

## 배경

`NearJCache`와 `SuspendNearJCache`의 `getAndPut`, `getAndRemove`,
`getAndReplace`가 front 조회·변경 조합에 의존해 front miss에서 표준 JCache
반환값을 잃고, front와 back 사이의 compound 계약을 보장하지 못했다.

## 결정

- 세 연산은 front를 먼저 읽지 않고 back provider의 `getAnd*` 원자 연산을
  호출한다.
- back 연산이 성공한 뒤에만 front를 새 값으로 반영하거나 제거한다.
- 동기 구현은 기존 write-through timeout·generation barrier를 재사용하되,
  compound 반환값을 보존하기 위해 완료까지 기다린다.
- suspend 구현은 back 결과를 받은 뒤 front를 반영하며, 별도 provider 의존성을
  추가하지 않는다.

## 검증

- RED: `NearJCacheCompoundOperationContractTest` 초기 6개가 기존 구현에서 실패했다.
  front miss 시 back `getAnd*`가 호출되지 않거나 front 반영이 없었다.
- GREEN: back 장애 전파 회귀를 포함한 현재 7개가 성공했다.
- 기존 `NearJCacheContractTest` 24개와 cache-core 전체 125개도 성공했다.
- `git diff --check`를 통과했다.

## 경계와 후속 작업

provider의 back 원자성은 provider 계약에 위임하며, 서로 다른 NearJCache
wrapper 사이의 다중 키 transaction은 보장하지 않는다. cleanup 관측성은
[#1371](https://github.com/bluetape4k/bluetape4k-projects/issues/1371),
`getAll` residency 예산은
[#1369](https://github.com/bluetape4k/bluetape4k-projects/issues/1369),
shared back clear 권한은
[#1368](https://github.com/bluetape4k/bluetape4k-projects/issues/1368)에서
별도로 다룬다.

## 참고

- [JCache `Cache` API](https://www.javadoc.io/static/javax.cache/cache-api/1.1.0/javax/cache/Cache.html)
- [Epic #1408](https://github.com/bluetape4k/bluetape4k-projects/issues/1408)
