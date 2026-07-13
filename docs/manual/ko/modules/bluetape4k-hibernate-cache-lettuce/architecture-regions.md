---
title: Near Cache 구조와 Region
description: Hibernate Region을 Caffeine L1과 Redis L2에 연결하는 읽기, 쓰기, 격리 구조를 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: architecture-regions
---

# Near Cache 구조와 Region

## 두 계층이 맡는 일

각 `LettuceNearCache`는 프로세스 안의 Caffeine L1과 여러 프로세스가 공유하는 Redis L2를 묶습니다. 읽을 때 L1을 먼저 보고, miss이면 Redis에서 읽어 L1을 채웁니다. 쓸 때는 Redis에 먼저 저장한 뒤 L1을 갱신합니다.

```text
Hibernate SessionFactory
  └─ LettuceNearCacheRegionFactory
       ├─ entity Region ─ Caffeine L1 + Redis L2
       ├─ collection Region ─ Caffeine L1 + Redis L2
       └─ query Region ─ Caffeine L1 + Redis L2
```

Redis 쓰기가 실패하면 `LettuceNearCache.put`은 L1을 갱신하기 전에 예외를 던집니다. StorageAccess는 그 예외를 경고로 바꾸므로 database transaction은 계속되지만 cache put은 완료되지 않습니다.

## Region당 인스턴스 하나

`LettuceNearCacheRegionFactory`는 `ConcurrentHashMap.computeIfAbsent`로 같은 Region의 cache를 재사용합니다. entity와 collection access가 같은 Region을 여러 번 요청해도 Redis connection과 Caffeine cache를 중복으로 만들지 않습니다.

```kotlin
val nearCache = caches.computeIfAbsent(regionName) {
    LettuceNearCache(client, codec, properties.buildNearCacheConfig(regionName))
}
```

`getCaches()`는 Metrics와 Actuator가 읽을 수 있도록 현재 Region map을 반환하지만 수정 불가능한 view입니다. 애플리케이션이 여기에 entry를 추가하거나 제거해 수명주기를 우회할 수 없습니다.

## Redis key 공간

Near Cache는 Redis key 앞에 `{regionName}:`을 붙입니다. Hibernate key 자체는 StorageAccess가 `hck2:<SHA-256 digest>`로 바꾼 뒤 전달하므로 실제 key는 다음 형태입니다.

```text
io.example.Product:hck2:K3...digest
```

Region 전체 제거는 `FLUSHDB`가 아니라 `${regionName}:*`를 `SCAN`하고 `UNLINK`합니다. 다른 Region과 같은 Redis database를 쓰더라도 해당 prefix 밖의 key는 지우지 않습니다.

## 1차 캐시와 혼동하지 않기

Hibernate Session의 persistence context가 1차 캐시입니다. 같은 Session에서 entity를 두 번 찾았을 때 SQL이 한 번만 실행되는 결과만으로 Lettuce 2차 캐시가 동작한다고 판단할 수 없습니다.

```kotlin
repeat(2) {
    sessionFactory.openSession().use { session ->
        session.beginTransaction()
        checkNotNull(session.find(Product::class.java, id))
        session.transaction.commit()
    }
}
```

새 Session에서 반복하고 `secondLevelCacheHitCount`를 확인해야 2차 캐시를 검증할 수 있습니다.

## Source와 tests

- [`LettuceNearCacheRegionFactory.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`HibernateFirstLevelCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateFirstLevelCacheTest.kt)
- [`LettuceNearCacheRegionFactoryTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactoryTest.kt)
