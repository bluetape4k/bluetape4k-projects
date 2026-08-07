---
title: 설정과 Hibernate property 매핑
description: Spring Boot property의 기본값과 duration, region별 TTL이 Hibernate 설정으로 변환되는 방식을 설명합니다.
manualId: bluetape4k-spring-boot-hibernate-lettuce
chapterId: properties-and-hibernate-mapping
---

# 설정과 Hibernate property 매핑

## 기본 설정

`LettuceNearCacheSpringProperties`의 prefix는 `bluetape4k.cache.lettuce-near`입니다.

| Spring property | 기본값 | 의미 |
| --- | --- | --- |
| `enabled` | `true` | 전체 auto-configuration 활성화 |
| `redis-uri` | `redis://localhost:6379` | Redis 연결 URI |
| `codec` | `lz4fory` | cache value codec |
| `use-resp3` | `true` | RESP3 client tracking 사용 |
| `local.max-size` | `10000` | Caffeine L1의 최대 항목 수 |
| `local.expire-after-write` | `30m` | L1 write 이후 만료 |
| `redis-ttl.default` | `120s` | Redis L2 기본 TTL |
| `metrics.enabled` | `true` | Hibernate statistics와 binder 활성화 |
| `metrics.enable-caffeine-stats` | `true` | Caffeine local statistics 기록 |

기본 Redis URI는 개발 편의를 위한 값입니다. 운영에서는 secret·환경 설정에서 명시하고 TLS, 인증, timeout 정책을 하위 Lettuce 구성과 함께 확인합니다.

## Hibernate property 변환

Customizer는 항상 RegionFactory와 2차 캐시 활성화를 설정합니다.

| Spring property | Hibernate property |
| --- | --- |
| `redis-uri` | `hibernate.cache.lettuce.redis_uri` |
| `codec` | `hibernate.cache.lettuce.codec` |
| `use-resp3` | `hibernate.cache.lettuce.use_resp3` |
| `local.max-size` | `hibernate.cache.lettuce.local.max_size` |
| `local.expire-after-write` | `hibernate.cache.lettuce.local.expire_after_write` |
| `redis-ttl.default` | `hibernate.cache.lettuce.redis_ttl.default` |
| `redis-ttl.regions[name]` | `hibernate.cache.lettuce.redis_ttl.{name}` |
| `metrics.enabled=true` | `hibernate.generate_statistics=true` |
| `metrics.enable-caffeine-stats=true` | `hibernate.cache.lettuce.local.record_stats=true` |

metrics를 끄면 관련 key를 `false`로 넣는 것이 아니라 아예 추가하지 않습니다. 애플리케이션이 같은 Hibernate property를 별도로 정의했다면 최종 병합 순서를 확인해야 합니다.

## Duration 보존

duration이 초 단위로 정확히 나뉘면 `60s`, 그렇지 않으면 `500ms`처럼 전달됩니다.

```yaml
local:
  expire-after-write: 500ms
redis-ttl:
  default: 1500ms
```

1.12.1 테스트는 위 두 값이 각각 `500ms`, `1500ms`로 보존되는지 확인합니다. 즉, sub-second 값을 초로 반올림하지 않습니다.

## Region별 TTL

`regions` map은 default TTL을 region별로 덮어씁니다.

```yaml
redis-ttl:
  default: 120s
  regions:
    product: 300s
    "[com.example.Order]": 600s
```

점이 포함된 region 이름은 Spring binding이 쪼개지지 않도록 대괄호로 감쌉니다. 변환된 Hibernate key에는 원래 점이 유지됩니다. entity annotation의 region 이름과 여기의 map key가 정확히 같아야 override가 적용됩니다.

## 설정 검토 기준

- L1 크기는 entry 수 기준이며 byte 제한이 아닙니다.
- L1 만료와 Redis TTL은 서로 다른 계층에 적용됩니다.
- `use-resp3=true`는 Redis 6+와 client tracking을 전제로 합니다.
- codec을 바꾸면 기존 Redis value와 호환되지 않을 수 있으므로 배포 전환·cache flush 계획이 필요합니다.
- statistics는 비용이 있으므로 운영 관측에 필요한 수준인지 부하 테스트에서 확인합니다.

## 실행 근거

- [`LettuceNearCacheSpringProperties.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`LettuceNearCacheHibernateAutoConfiguration.kt`](../../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCachePropertiesCustomizerTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`LettuceNearCacheAutoConfigurationTest.kt`](../../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
