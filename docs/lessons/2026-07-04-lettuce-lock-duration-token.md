# Lettuce lock duration과 token 보존

## 배경

이슈 #949는 Lettuce lock API가 잘못된 duration을 Redis PX value로 변환하고,
Redis release success를 알기 전에 local owner token을 지운다는 점을 확인했다.

## 결정

Lock duration은 API boundary에서 검증하고, Redis Lua release script가 성공을 확인한
뒤에만 local token을 지운다.

## 결과

잘못된 lease/wait duration은 Redis command가 발행되기 전에 실패한다. Redis key가
만료되었거나 더 이상 일치하지 않아 release가 실패하면 local token은 남아 있고,
호출자는 release evidence가 확보될 때 retry할 수 있다.

## 검증

- `./gradlew :bluetape4k-lettuce:test --tests 'io.bluetape4k.redis.lettuce.lock.LettuceLockTest' --tests 'io.bluetape4k.redis.lettuce.lock.LettuceSuspendLockTest'`

## 향후 지침

Distributed lock 구현은 command construction 전에 Redis TTL input을 검증해야 하며,
remote release success가 확인되기 전에는 local ownership evidence를 버리면 안 된다.
