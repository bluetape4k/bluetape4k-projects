---
title: 수명주기, 장애와 운영
description: RegionFactory 시작·종료, Redis 장애 fallback, eviction과 관측 항목을 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: lifecycle-failures-operations
---

# 수명주기, 장애와 운영

## SessionFactory가 자원을 소유한다

Hibernate가 `prepareForUse`를 호출하면 설정과 codec을 검증하고 `RedisClient`를 만듭니다. RESP3가 켜져 있으면 client protocol을 지정한 뒤 필드에 할당하고 `ShutdownQueue`에도 등록합니다.

종료는 다음 순서를 지킵니다.

1. 각 Region의 Near Cache와 tracking connection을 닫습니다.
2. Region map을 비웁니다.
3. 공유 `RedisClient`를 종료하고 `null`로 바꿉니다.

client를 먼저 닫으면 cache가 이미 종료된 client로 L2 정리를 시도할 수 있습니다. `StorageAccess.release()`는 공유 cache를 닫지 않는 no-op이고 최종 소유자는 RegionFactory입니다. SessionFactory를 명시적으로 닫아야 이 경로가 실행됩니다.

## 장애는 DB fallback으로 바뀐다

StorageAccess의 계약은 다음과 같습니다.

| 연산 | Redis·cache 오류 시 결과 |
| --- | --- |
| `getFromCache` | 경고 후 `null`; Hibernate가 DB에서 읽음 |
| `putIntoCache` | 경고 후 무시; transaction은 계속됨 |
| `contains` | 경고 후 `false` |
| key·Region eviction | 경고 후 무시 |

이 정책은 Redis 장애가 곧바로 주문·결제 transaction 실패로 번지는 것을 막습니다. 반대로 miss storm이 DB를 압박하고, eviction 실패로 local stale entry가 남을 수 있습니다. cache error rate와 DB pool을 따로 보지 말아야 합니다.

## Eviction 범위

`evictData(key)`는 L1에서 제거하고 Redis `UNLINK`를 호출합니다. Region 전체 `evictData()`는 L1을 비운 뒤 `${regionName}:*`를 cursor `SCAN`하며 묶음 `UNLINK`합니다.

`clearAll()`은 즉시 완료되는 상수 시간 연산이 아닙니다. Region이 크면 여러 scan round trip이 필요합니다. 배포나 운영 명령으로 전체 Region을 자주 비우기보다 schema·serialization 변경 때 namespace/version 전략과 함께 계획합니다.

## 관측할 지표

- Hibernate `secondLevelCacheHitCount`, miss, put
- Region별 `CacheRegionStatistics`
- query cache hit·put과 update timestamps put
- Caffeine local size와 hit rate (`local.record_stats=true`)
- Redis latency, errors, reconnect와 connection 수
- DB query latency, connection pool active·pending
- Region eviction 시간과 삭제 key 수

hit rate가 높아도 stale read나 DB fallback 폭증이 있으면 성공한 캐시가 아닙니다. 캐시를 끈 baseline과 함께 p95/p99, DB load와 결과 정합성을 비교합니다.

## 장애 훈련

운영 전 Redis를 잠시 중단해 읽기가 DB로 돌아가는지, DB pool이 감당하는지, Redis 복구 뒤 L1·L2가 다시 채워지는지 확인합니다. 별도 프로세스에서 Redis 값을 변경해 RESP3 invalidation도 검증합니다. cache serialization을 바꿀 때는 기존 entry를 읽을 수 있는지 또는 안전하게 Region을 비울 절차가 있는지 준비합니다.

## Source와 tests

- [`LettuceNearCacheRegionFactory.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`HibernateCacheStatisticsTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheStatisticsTest.kt)
- [`HibernateCacheContainmentTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateCacheContainmentTest.kt)
- [`LettuceNearCacheRegionFactoryTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactoryTest.kt)
