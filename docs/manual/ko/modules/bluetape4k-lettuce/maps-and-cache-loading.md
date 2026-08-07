---
title: Map과 cache loader
description: Redis map의 read-through, write-through, write-behind와 invalidation 경계를 설명합니다.
manualId: bluetape4k-lettuce
chapterId: maps-and-cache-loading
---

# Map과 cache loader

## 단순 map과 loaded map

`LettuceMap`은 Redis Hash를 sync/async API로 감싸고 `LettuceSuspendMap`은 suspend API를 제공합니다. `LettuceLoadedMap`은 key별 Redis value에 TTL을 적용하고 miss 때 `MapLoader`를 호출합니다.

```kotlin
LettuceLoadedMap(
    client = client,
    loader = object : MapLoader<Long, Account> {
        override fun load(key: Long): Account? = repository.find(key)
        override fun loadAllKeys(): Iterable<Long> = repository.findAllIds()
    },
    writer = accountWriter,
    config = LettuceCacheConfig.READ_WRITE_THROUGH,
).use { cache ->
    val account = cache[42L]
}
```

`WRITE_THROUGH`은 writer가 성공한 뒤 Redis를 갱신합니다. writer가 실패하면 Redis에는 새 값이 들어가지 않습니다. `WRITE_BEHIND`는 bounded queue에 넣고 Redis를 즉시 갱신하므로 DB 반영 실패는 나중에 드러납니다. queue 포화는 즉시 실패합니다.

## invalidation과 종료

`delete`는 writer와 Redis를 함께 변경하고 `evict`는 Redis만 지웁니다. pattern invalidation은 `SCAN`과 `UNLINK`를 사용합니다. write-behind map은 `close()`에서 정해진 timeout 동안 queue를 drain하고 남은 항목을 warning으로 기록합니다. suspend variant는 caller scope 전체가 아니라 내부 job만 취소합니다.

## 1.12.1 Near Cache 주의

`LettuceCacheConfig`의 `nearCache*` 값과 preset은 검증만 됩니다. loaded map은 이 값을 읽지 않으며 RESP3 tracking invalidation도 없습니다. 이 버전에서 local Near Cache가 필요하면 이를 별도 구현하거나 `bluetape4k-cache-lettuce`의 실제 계약을 확인합니다.

## Source와 tests

- [`LettuceLoadedMap.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMap.kt)
- [`LettuceSuspendedLoadedMap.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/map/LettuceSuspendedLoadedMap.kt)
- [`LettuceLoadedMapTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/map/LettuceLoadedMapTest.kt)

다음은 [Filter, script와 분산 primitive](./filters-scripts-and-primitives.md)입니다.
