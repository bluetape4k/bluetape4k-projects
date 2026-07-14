---
title: Cache 전략과 생태계 경로
description: Cache-aside와 read/write-through·behind를 구분하고 persistence와 workshop 경로를 연결합니다.
manualId: bluetape4k-cache-redisson
chapterId: ecosystem-paths
---

# Cache 전략과 생태계 경로

## 이름보다 데이터 경로를 본다

| 전략 | miss/쓰기 owner | 원본 저장소 호출 |
| --- | --- | --- |
| cache-aside | caller | caller가 직접 조회·저장 |
| read-through | `CacheLoader`/`MapLoader` | cache miss가 loader를 호출 |
| write-through | `CacheWriter`/`MapWriter` | cache write 완료 전에 writer 호출 |
| write-behind | writer queue | cache write 뒤 비동기로 원본 반영 |

`RedissonNearCache.put()`과 memoizer의 `RMap.putIfAbsent()`는 Redis cache를 갱신할 뿐입니다. database table을 호출하는 writer가 없으므로 persistence write-through가 아닙니다.

## Cache-aside 기본형

```kotlin
suspend fun findProduct(id: String): Product {
    products.get(id)?.let { return it }
    return repository.findById(id).also { loaded ->
        products.put(id, loaded)
    }
}
```

첫 요청은 DB에서 읽고 cache를 채웁니다. update에서는 DB commit과 cache invalidate 순서, 실패 뒤 stale window를 별도로 정합니다.

## Redisson loader와 writer로 확장

`examples/redisson-demo`는 `MapLoader`, `MapWriter`와 async 대응 API로 read-through, write-through, write-behind를 보여 줍니다. 이 예제에서는 loader/writer가 실제 repository를 호출하므로 cache-aside와 구분할 수 있습니다.

write-behind는 응답 latency를 줄일 수 있지만 process crash, queue overflow, ordering과 retry에 따라 데이터 손실·재정렬 위험이 생깁니다. 원본 데이터의 유일한 쓰기 경로로 채택하기 전에 durability 요구를 검증합니다.

## 모듈 선택 경로

- [cache-core](../bluetape4k-cache-core.md): local provider, JCache, memoizer와 공통 Near Cache 계약부터 배웁니다.
- [redisson](../bluetape4k-redisson.md): Redisson codec, distributed map, lock, topic과 coroutine bridge를 확장합니다.
- [Hibernate](../bluetape4k-hibernate.md): entity lifecycle과 second-level cache 경계를 다룹니다.
- [Spring Boot Hibernate Lettuce](../bluetape4k-spring-boot-hibernate-lettuce.md): Spring/Hibernate cache wiring을 확인합니다.
- [redisson-demo](../bluetape4k-examples-redisson-demo.md): loader/writer가 있는 실행 예제로 전략 차이를 검증합니다.

## Exposed와 workshop

[exposed-workshop](https://github.com/bluetape4k/exposed-workshop)의 cache 장은 `JdbcCacheRepository`, `EntityMapLoader`, `EntityMapWriter`를 연결해 repository와 cache 경계를 보여 줍니다. Hibernate와 Exposed 중 어느 persistence 모델을 선택하든 transaction commit 전후 invalidation 규칙은 별도로 필요합니다.

[bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)은 서비스 수준 예제로 이어지는 경로입니다. workshop 코드가 이 매뉴얼의 전략 이름과 다르면 실제 loader/writer 호출을 기준으로 판단합니다.

## Source와 tests

- [`CacheReadThroughExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheReadThroughExample.kt)
- [`CacheWriteThroughExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteThroughExample.kt)
- [`CacheWriteBehindExample.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/cachestrategy/CacheWriteBehindExample.kt)
- [`MapReadWriteThroughTest.kt`](../../../../../examples/redisson-demo/src/test/kotlin/io/bluetape4k/examples/redisson/coroutines/readwritethrough/MapReadWriteThroughTest.kt)
