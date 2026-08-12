# #1363 NearJCache 표준 read/clear 계약 교훈

## 결정

`NearJCache`가 `javax.cache.Cache`로 노출될 때 표준 `get`, `containsKey`,
`getAll`, `clear`도 front/back을 합친 하나의 논리적 캐시를 관찰해야 한다.
기존 `getDeeply`와 `clearAllCache`는 각각 표준 `get`과 `clear`의 호환 alias로
유지한다.

## 실패 원인과 복구

- front-only 표준 read는 back-only 값을 숨겨 JCache 호출자에게 provider마다 다른
  결과를 보였다. 표준 read는 front miss에서 back fallback과 front populate를
  수행하고, `getAll`은 miss set을 한 번의 back bulk 호출로 처리한다.
- read-through 결과를 전역 mutation epoch로 fencing하되, backend write도 같은
  epoch로 무효화하면 서로 다른 키의 정상적인 비동기 write가 사라진다. 따라서
  read populate에는 `mutationEpoch`, clear 이전 write를 무효화하는 backend
  barrier에는 `backWriteGeneration`을 별도로 사용한다.
- `clear`는 mutation gate와 공정한 backend write lock을 사용한다. timeout이 먼저
  completion을 실패시켜도 실제 backend 호출이 끝날 때까지 기다린 뒤 back을
  삭제하므로 late write가 clear 이후 값을 부활시키지 않는다.
- 기본 front 설정은 store-by-reference로 고정하고, 안전한 filtered copier 계약이
  없는 custom store-by-value 설정은 생성 단계에서 거부한다. populate 실패 로그는
  operation/provider/cache 메타데이터만 남긴다.

## 검증

- `NearJCacheContractTest`는 표준 `Cache` 참조의 read/clear, bulk 호출 수,
  compound front-only 왕복, read-vs-mutation/clear latch 경합, timeout-late
  barrier, populate 예외 경계, serialization 설정을 검증한다.
- `cache-core` fixture는 호출 wrapper의 front/back 삭제와 listener 없는 peer
  front의 전파 한계를 분리해 검증한다.
- provider 검증은 Cache2k, Hazelcast, Lettuce, Redisson 순서로 실행한다.

## 후속 주의

공유 back cache의 tenant/owner 권한 경계와 `getAll` front residency 상한은 이번
이슈에서 공개 설정으로 추가하지 않았다. destructive shared clear 또는 대량 bulk
populate 정책을 바꿀 때는 별도 설계와 보안 검토를 먼저 수행해야 한다.
