# 이슈 #1275 Redis key-chain rotation lock lease

이슈 #1275는 `RedisKeyChainRepository`가 rotation lock에 30초 고정 lease를
사용하고, lock 해제 과정의 오류를 `runCatching`으로 삼키던 문제를 다룬다. Redis
저장과 오래된 KeyChain 정리가 lease보다 오래 걸리면 lock이 먼저 만료되어 두 노드가
동시에 rotation을 commit할 수 있고, 소유권 상실이나 해제 실패가 관찰되지 않았다.

## 결정

rotation lock은 lease 시간을 지정하지 않는 Redisson `tryLock(waitTime, unit)`
overload를 사용한다. 이 방식은 Redisson watchdog가 commit이 끝날 때까지 lock
소유권을 갱신하도록 한다. 해제 직전에 현재 스레드의 소유권을 확인하고, 소유권 상실이나
unlock 실패를 호출자에게 전달한다. rotation 본문이 먼저 실패했다면 unlock 오류는
suppressed exception으로 붙여 원래 오류를 primary failure로 보존한다.

## 교훈

- 분산 lock의 고정 lease는 여러 Redis 명령으로 구성된 commit 구간의 상한으로 사용할 수
  없다. client watchdog 또는 fencing 같은 명시적인 소유권 전략이 필요하다.
- lock 해제 실패를 무시하면 commit 결과와 lock 상태가 달라져 장애 원인을 추적할 수 없다.
  release 오류는 관찰 가능해야 하며, primary failure를 덮어쓰면 안 된다.
- concurrent two-node 회전 테스트와 짧은 lease를 시뮬레이션한 watchdog 테스트를 함께 두면
  실제 Redis 수렴과 lease 만료 회귀를 긴 wall-clock 대기 없이 검증할 수 있다.

## 검증

- RED: 기존 고정 lease 호출은 watchdog-overload를 검증하는 테스트에서
  `Failed to acquire Redis keychain rotation lock.`으로 실패했다.
- GREEN targeted: `./gradlew :bluetape4k-jwt:test --tests
  "io.bluetape4k.jwt.keychain.redis.RedisKeyChainRepositoryTest" --no-build-cache`가
  15 tests로 통과했다.
- module: `./gradlew :bluetape4k-jwt:test --no-build-cache`가 155 tests, 10 pending으로
  통과했다.
- build: `./gradlew :bluetape4k-jwt:build --no-build-cache`가 통과했다.
- static analysis: `./gradlew :bluetape4k-jwt:detekt --no-build-cache
  --no-configuration-cache`가 통과했고 변경한 `RedisKeyChainRepository.kt`에는
  새 detekt finding이 없었다. 기존 JWT 파일의 finding은 별도 이슈로 남아 있다.
- hygiene: `git diff --check`가 통과했다.
