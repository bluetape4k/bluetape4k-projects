# #1269 Near Cache write-behind mutation 실패 전파

## 배경

`ResilientNearJCache`와 `ResilientSuspendNearJCache`는 front cache를 먼저
변경하고 back cache mutation을 queue 또는 channel에서 비동기로 처리합니다.
기존 구현은 retry exhaustion이나 close drain timeout을 로그로만 남겼습니다.
특히 remove 실패 뒤 tombstone을 해제하고 clear 실패 뒤 `clearPending`을 해제해,
back cache의 stale value가 삭제 또는 clear 성공처럼 다시 보일 수 있었습니다.

## 결정 또는 발견

- write-behind mutation 메서드(`put`, `putAll`, `remove`, `removeAll`, `clearAll`)는
  back cache 반영 완료를 나타내는 `CompletableFuture<Unit>`을 반환합니다. 정상 완료는
  back cache 반영 완료이며, retry exhaustion과 close drain timeout은 future를
  exceptionally complete합니다.
- queue 또는 channel이 가득 차면 front cache를 변경하기 전에 기존처럼
  `IllegalStateException`을 동기적으로 던집니다. 수락되지 않은 명령에는 completion
  future를 만들지 않습니다.
- remove 명령이 실패하면 tombstone을 유지하고, clear 명령이 실패하면
  `clearPending`과 clear token을 유지합니다. 따라서 caller가 실패를 확인하고
  재시도하기 전까지 stale back read가 성공처럼 노출되지 않습니다.
- close timeout 시 아직 완료되지 않은 명령을 idempotent하게 실패 완료합니다.
  consumer가 이후 늦게 반환해도 이미 실패한 completion과 보호 상태를 덮어쓰지
  않습니다.
- blocking 구현과 suspend 구현은 동일한 completion, retry, tombstone/clear guard,
  close timeout 정책을 사용합니다. drain timeout은
  `ResilientNearJCacheConfig.closeDrainTimeout`으로 조정할 수 있습니다.

## 결과

호출자는 mutation의 terminal 상태를 future로 관찰할 수 있고, 실패한 삭제 또는
clear가 stale back state를 성공처럼 되살리지 않습니다. 수락된 명령은 close 시
설정된 시간만큼 drain을 시도하고, 시간 안에 끝나지 않으면 caller-visible failure로
남습니다. 성공 명령은 기존 write-behind 순서와 front cache 즉시 반영 동작을
유지합니다.

## 검증

- RED: blocking/suspend remove, removeAll, clear retry exhaustion에서 back value가
  다시 읽히는 6개 회귀 실패를 재현했습니다.
- GREEN: blocking/suspend near-cache targeted test에서 completion success/failure,
  tombstone/clear guard, queue/channel backpressure, close timeout을 검증했습니다.
  73개 테스트가 6.6초에 통과했습니다.
- `:bluetape4k-cache-core:compileKotlin` 통과.
- `:bluetape4k-cache-core:test --rerun-tasks` 507개 통과, 48.9초,
  `BUILD SUCCESSFUL`.
- `git diff --check` 통과.
- `:bluetape4k-cache-core:detekt`는 기존 cache-core의 ReturnCount, TooManyFunctions,
  TooGenericExceptionCaught, MagicNumber finding으로 실패했습니다. 새 동작과 무관한
  기존 finding이며, 새로 추가한 긴 표현식은 정리했습니다.
- `:bluetape4k-cache-core:dokkaGenerate`는 기존 `README.md`의
  `Package / Import Stability` package-name 오류로 실패했습니다. 변경 KDoc 자체의
  unresolved link 경고(`CacheEntryListenerException`, `Cache`)도 남아 있습니다.

## 향후 지침

write-behind API를 변경할 때는 front 반영 성공과 back 반영 완료를 같은 상태로
취급하지 말고, caller가 terminal failure를 관찰할 수 있는 계약을 먼저 정의합니다.
retry가 끝난 뒤에는 실패 증거(tombstone 또는 clear guard)를 보존해야 하며,
close timeout은 pending 명령을 예외 완료해 무기한 대기를 방지해야 합니다.
blocking과 suspend 경로는 동일한 상태 전이와 테스트 시나리오를 공유하고,
queue overflow는 front mutation보다 먼저 검증합니다.
