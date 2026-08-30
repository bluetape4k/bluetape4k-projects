---
title: Actuator와 Micrometer 관측
description: nearcache endpoint와 aggregate gauge가 제공하는 값, null 의미와 운영 대시보드 구성을 설명합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: observability-actuator-metrics
---

# Actuator와 Micrometer 관측

> 계약 범위: `develop`의 **2.0.0 current contract**입니다. 안정 릴리스 rollback
> 기준은 [1.12.1](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/1.12.1)입니다.

## Endpoint 활성화

`bluetape4k.cache.lettuce-near.enabled`와
`bluetape4k.cache.lettuce-near.metrics.enabled`가 모두 활성화되고, classpath에
Actuator가 있으며 `EntityManagerFactory` Bean이 있으면 `nearcache` endpoint
Bean이 등록됩니다. endpoint 자체는 `MeterRegistry` Bean을 요구하지 않습니다.
HTTP로 조회하려면 `management.endpoints.web.exposure.include`에 추가합니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,nearcache
```

```text
GET /actuator/nearcache
GET /actuator/nearcache/{regionName}
```

전체 조회는 `Map<String, RegionStats>`를, 개별 조회는 region이 없을 때 `null`을 반환합니다.

## RegionStats 읽기

| 필드 | 출처 | null 가능성 |
| --- | --- | --- |
| `regionName` | RegionFactory | 없음 |
| `localSize` | Caffeine local cache | cache가 없으면 `0` |
| `localHitRate`, hit/miss/eviction | Caffeine stats | local stats가 꺼지면 `null` |
| `l2HitCount`, miss/put | Hibernate statistics | statistics가 꺼지거나 조회가 실패하면 `null` |

`0`과 `null`을 구분해야 합니다. `0`은 관측 대상이 있지만 현재 값이 0일 수 있고, `null`은 해당 통계가 수집되지 않았다는 뜻입니다.

Endpoint는 factory unwrap과 통계 조회를 `runCatching`으로 감쌉니다. 예상한 RegionFactory가 아니거나 unwrap이 실패하면 전체 조회는 빈 map, 개별 조회는 `null`이 됩니다. endpoint 응답만으로 cache backend가 정상이라고 판단하지 않습니다.

## Micrometer gauge

Binder는 같은 root/metrics property 조건에 더해 실제 `EntityManagerFactory`와
`MeterRegistry` Bean을 요구합니다. registry가 없으면 endpoint는 등록될 수 있지만
gauge는 등록되지 않습니다. singleton 생성이 끝나면 다음 gauge를 등록합니다.

- `lettuce.nearcache.active.regions`: 현재 RegionFactory cache map의 크기
- `lettuce.nearcache.total.local.size`: 모든 region의 local entry 수 합계

두 gauge는 조회할 때 현재 RegionFactory 상태를 읽으므로 새 region이 생기면 값에 반영됩니다. region별 hit rate나 Redis latency는 이 binder가 제공하지 않습니다.

```text
GET /actuator/metrics/lettuce.nearcache.active.regions
GET /actuator/metrics/lettuce.nearcache.total.local.size
```

## 대시보드 구성

aggregate gauge만 보면 특정 hot region의 eviction 폭증을 놓칠 수 있습니다. 다음 신호를 함께 둡니다.

- application instance별 active region과 local size
- Hibernate region별 L2 hit/miss/put
- Redis command latency, error와 reconnect
- database query count와 latency
- cache 설정 변경·배포 시각

local size는 eviction이 비동기 정리되는 순간 정확히 0이 아닐 수 있습니다. 현재
통합 테스트도 local size를 정확한 값이 아니라 0 이상으로 검증합니다. 순간값보다
추세와 database 부하의 상관관계를 봅니다.

## 등록 실패

Binder는 RegionFactory가 다른 구현이면 debug 로그를 남기고 아무 것도 등록하지 않습니다. unwrap이나 gauge 등록이 실패하면 warning을 기록하지만 startup을 실패시키지 않습니다. 따라서 “metric이 없다”는 alert는 cache backend alert와 별도로 둬야 원인을 구분할 수 있습니다.

## 실행 근거

- [`LettuceNearCacheActuatorEndpoint.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt)
- [`LettuceNearCacheActuatorAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheMetricsBinder.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt)
- [`LettuceNearCacheMetricsAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheIntegrationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
