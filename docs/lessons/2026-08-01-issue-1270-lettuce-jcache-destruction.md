# #1270 Lettuce JCache destroy 실패 가시성과 재시도 상태

## 배경

`LettuceCacheManager.destroyCache`는 기존에 registry에서 캐시를 먼저
제거한 뒤 `clear()`와 `close()` 예외를 `runCatching`으로 삼켰습니다. 따라서
Redis 데이터 삭제가 실패해도 호출자는 성공으로 오인하고, 같은 이름으로
캐시를 다시 만들 때 stale 데이터가 남을 수 있었습니다. suspend manager도
clear 뒤 wrapper를 닫지 않았고, blocking JCache wrapper의 resource close
실패 역시 숨겨졌습니다.

## 결정 또는 발견

- sync/suspend `destroyCache`는 `clear()`를 먼저 완료한 뒤 `close()`를 수행하고,
  두 단계의 실패를 `CacheException`으로 호출자에게 전파합니다.
- clear 실패 시 registry 항목을 유지해 같은 인스턴스로 재시도할 수 있게 합니다.
  close 실패 시 Redis 데이터 삭제는 완료된 상태이므로 해당 registry 항목은
  제거하고 실패를 전파합니다.
- `LettuceJCache.close()`는 wrapper를 closed 상태로 전환하고 manager registry에서
  제거한 뒤 resource close 실패를 `CacheException`으로 보존합니다. 기존의
  log-only/`runCatching` 처리는 사용하지 않습니다.
- suspend 경로는 `CancellationException`을 lifecycle 오류로 변환하지 않고
  그대로 재전파하며, 정상 종료 시에는 현재 wrapper와 일치하는 registry entry만
  제거합니다.

## 결과

호출자는 clear 또는 close의 terminal 상태와 원래 원인을 관찰할 수 있습니다.
clear 실패 뒤에는 캐시가 registry에 남아 재시도할 수 있고, 성공적인 clear 뒤
동일한 이름으로 캐시를 재생성해도 이전 Redis hash 데이터가 보이지 않습니다.
blocking과 suspend 경로의 삭제 순서, registry 정책, 예외 가시성이 일치합니다.

## 검증

- RED: sync/suspend manager targeted suite에서 clear/close 예외 전파와 직접
  resource close 가시성에 대한 5개 회귀 실패를 먼저 재현했습니다.
- GREEN: `LettuceJCacheManagerTest`와 `LettuceSuspendJCacheManagerTest` 23개가
  모두 통과했습니다.
- `:bluetape4k-cache-lettuce:test` 전체 443개 통과, `BUILD SUCCESSFUL`.
- `:bluetape4k-cache-lettuce:compileKotlin` 및 `compileTestKotlin` 통과.
- `git diff --check` 통과.

## 향후 지침

JCache lifecycle을 변경할 때는 데이터 삭제, resource close, registry 제거를
서로 다른 terminal 단계로 취급합니다. cleanup 실패를 로그로만 남기지 말고
호출자가 원인과 재시도 가능 상태를 판단할 수 있는 예외 계약을 유지해야 합니다.
blocking과 suspend 구현은 동일한 순서와 실패 정책을 함께 검증하고, concurrent
재생성 가능성이 있는 registry 제거에는 이름뿐 아니라 현재 wrapper 일치 여부를
확인합니다.
