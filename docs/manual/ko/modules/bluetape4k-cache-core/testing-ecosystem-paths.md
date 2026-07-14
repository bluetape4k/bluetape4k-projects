---
title: 테스트와 생태계 경로
description: Cache-core의 빠른 단위 테스트와 provider conformance fixture를 실행하고 Lettuce, Redisson, Exposed, workshop으로 확장합니다.
manualId: bluetape4k-cache-core
chapterId: testing-ecosystem-paths
---

# 테스트와 생태계 경로

## 외부 서버 없이 계약 고정하기

`cache-core` 테스트는 local provider, MockK delegate와 in-memory fixture로 공통 계약을 검증합니다.

```bash
./gradlew :bluetape4k-cache-core:test --no-build-cache --no-configuration-cache
```

도입 코드에는 다음 네 가지 테스트를 먼저 둡니다.

1. 같은 key의 반복 조회가 evaluator를 다시 실행하지 않습니다.
2. 같은 key의 동시 miss가 필요한 경우 한 계산으로 합쳐집니다.
3. 실패·취소 뒤 다음 호출이 새 계산으로 복구합니다.
4. 원본 변경 뒤 invalidate가 실패했을 때 허용한 stale window 안에서 회복합니다.

## 공개 test fixture

모듈의 `testFixtures`에는 `AbstractSuspendJCacheTest`, blocking·suspend Near Cache operation fixture, memoizer fixture가 있습니다. 새 provider 구현은 fixture를 상속해 CRUD만 맞추는 데서 끝나지 않고 `clearLocal`, `clearAll`, conditional replace, statistics와 close까지 같은 계약을 검증합니다.

```kotlin
class MyNearCacheTest : AbstractNearCacheOperationsTest<String>() {
    override fun createCache(): NearCacheOperations<String> = myNearCache()
    override fun sampleValue(): String = "hello"
    override fun anotherValue(): String = "world"
}
```

remote provider 테스트는 실제 server를 쓰므로 cache-core 단위 테스트와 분리하고 Testcontainers를 순차 실행합니다.

## Lettuce와 Redisson

[bluetape4k-cache-lettuce](../bluetape4k-cache-lettuce.md)는 Redis hash JCache, Lettuce Near Cache와 RESP3 CLIENT TRACKING을 다룹니다. invalidation 연결이 끊겼을 때 local entry가 얼마나 남는지 확인해야 합니다.

[bluetape4k-cache-redisson](../bluetape4k-cache-redisson.md)은 Redisson JCache와 `RLocalCachedMap` 기반 Near Cache로 이어집니다. Pub/Sub invalidation, reconnection과 local cache option을 provider test에서 검증합니다.

두 provider 모두 `cache-core`의 API를 구현하지만 wire protocol, event 전달과 failure recovery가 같지는 않습니다. 공통 interface만 보고 운영 특성까지 같다고 가정하지 않습니다.

## JDBC·Exposed로 확장하기

cache-core의 `getOrPut`과 Near Cache `put`은 persistence transaction을 소유하지 않습니다. database cache 전략은 [bluetape4k-jdbc](../bluetape4k-jdbc.md)에서 connection·transaction 경계를 익힌 뒤 [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)로 확장합니다.

[exposed-workshop](https://github.com/bluetape4k/exposed-workshop)의 cache 장은 다음 경계를 실제 repository 코드로 보여 줍니다.

- `JdbcCacheRepository`: database와 cache 호출의 소유자
- `EntityMapLoader`: miss 시 database에서 읽는 read-through
- `EntityMapWriter`: write-through 또는 write-behind persistence
- `RMap` / `RLocalCachedMap`: remote·local cache 계층

이 예제에서만 cache writer가 Exposed DB/table 경계까지 이어집니다.

## Workshop 학습 순서

1. cache-core test로 local cache와 memoizer 동시성을 확인합니다.
2. 선택한 Redis provider 매뉴얼과 테스트로 invalidation을 확인합니다.
3. exposed-workshop에서 cache-aside, read-through, write-through를 비교합니다.
4. [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)의 서비스 예제에 expiry·failure metric을 추가합니다.

benchmark가 없다면 “더 빠르다”고 단정하지 않습니다. hit ratio, throughput, latency를 실제 workload에서 측정하고, cache가 없을 때의 원본 저장소 부하도 함께 기록합니다.

## Source와 tests

- [`AbstractSuspendJCacheTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/jcache/AbstractSuspendJCacheTest.kt)
- [`AbstractNearCacheOperationsTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt)
- [`AbstractSuspendNearCacheOperationsTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractSuspendNearCacheOperationsTest.kt)
- [`AbstractMemoizerTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/memoizer/AbstractMemoizerTest.kt)
- [Near Cache backend capability matrix](../../../../../docs/cache/near-cache-capability-matrix.md)
