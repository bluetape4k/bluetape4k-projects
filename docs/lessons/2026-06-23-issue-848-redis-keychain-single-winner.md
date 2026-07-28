# 이슈 #848 Redis key-chain single-winner rotation

issue #848은 `RedisKeyChainRepository.rotate()`가 process-local cached current keychain을
신뢰한다는 점을 찾았다. 두 application node가 같은 expired current keychain을 cache한
뒤 Redis에 서로 다른 replacement key를 모두 insert할 수 있었다.

## 결정

keychain queue name에서 파생한 Redisson lock으로 Redis-backed rotation을 serialize한다.
lock을 잡은 동안 Redis에서 current keychain을 다시 읽고 local cache를 갱신한 뒤 rotation
여부를 결정한다. race에서 진 node는 winner의 non-expired keychain을 보고 즉시 cache를
그 key로 수렴시킨다.

## 교훈

- distributed repository는 per-process cache만으로 rotation decision을 내릴 수 없다.
  lock-protected section은 shared state를 다시 읽어야 한다.
- stale-cache race는 thread timing 없이도 결정적으로 재현할 수 있다. repository instance
  두 개가 같은 expired key를 cache한 뒤 순차적으로 rotate하면 된다.
- forced rotation도 같은 Redis lock과 capacity trimming path를 사용해야 regular write와
  forced write가 deque maintenance 주변에서 interleave하지 않는다.

## 검증

- RED: `./gradlew :bluetape4k-jwt:test --tests "io.bluetape4k.jwt.keychain.redis.RedisKeyChainRepositoryTest.expired cached keychain rotates with single Redis winner"`가 `Expected <2> to equal to <1>`로 실패했다.
- GREEN targeted: 같은 Redis-backed test가 1 test로 통과했다.
- module: `./gradlew :bluetape4k-jwt:test`가 149 tests, 10 pending으로 통과했다.
- build: `./gradlew :bluetape4k-jwt:build`가 통과했다.
- hygiene: `git diff --check`가 통과했다.
- static analysis: `./gradlew detekt`가 `:detekt NO-SOURCE`와 함께 통과했다.
