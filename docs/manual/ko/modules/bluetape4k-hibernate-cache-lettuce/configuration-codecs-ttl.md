---
title: 설정, codec과 TTL
description: Hibernate property, duration 파싱, Region별 TTL과 직렬화 codec의 신뢰 경계를 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: configuration-codecs-ttl
---

# 설정, codec과 TTL

## 설정은 시작할 때 검증한다

모든 provider 설정은 `hibernate.cache.lettuce.` prefix를 사용합니다.

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `redis_uri` | `redis://localhost:6379` | Redis endpoint |
| `codec` | `lz4fory` | value 직렬화·압축 조합 |
| `use_resp3` | `true` | CLIENT TRACKING용 RESP3 |
| `local.max_size` | `10000` | Region별 L1 최대 entry 수 |
| `local.expire_after_write` | `30m` | L1 쓰기 후 만료 |
| `redis_ttl.default` | `120s` | 일반 Region L2 TTL |
| `redis_ttl.<region>` | 없음 | 해당 Region TTL override |
| `local.record_stats` | `false` | Caffeine 통계 기록 |

boolean은 `true`와 `false`만 허용합니다. duration은 `500ms`, `90s`, `15m`, `2h`를 지원하며 접미사가 없으면 초입니다. 파싱할 수 없는 값, 0 이하 크기와 duration은 factory가 cache를 만들기 전에 실패합니다.

## Region별 TTL

```properties
hibernate.cache.lettuce.redis_ttl.default=120s
hibernate.cache.lettuce.redis_ttl.io.example.Product=300s
hibernate.cache.lettuce.redis_ttl.io.example.Order=600s
```

Region override가 기본값보다 우선합니다. L1 만료와 Redis TTL은 서로 다른 계층의 정책입니다. L1이 먼저 만료되면 Redis에서 다시 채우고, Redis가 먼저 만료되면 다음 조회가 DB까지 내려갑니다.

`default-update-timestamps-region`의 Redis TTL은 항상 `null`입니다. query result가 남아 있는데 변경 시각 정보만 만료되면 stale query 결과를 안전하게 판별할 수 없기 때문입니다.

## 15개 codec

지원 이름은 `jdk`, `kryo`, `fory`와 각 serializer의 `gzip`, `lz4`, `snappy`, `zstd` 조합입니다. 이름은 대소문자를 구분하지 않지만 오타를 기본값으로 바꾸지 않습니다.

압축은 payload와 CPU의 교환입니다. 작은 entity에 압축을 켜면 Redis bytes보다 CPU와 allocation이 더 늘 수 있습니다. 실제 entity 분포로 크기와 latency를 측정해 고릅니다.

## 직렬화 신뢰 경계

Kryo와 Fory 기반 codec은 Redis의 byte stream으로 객체를 재구성합니다. Redis에 신뢰할 수 없는 사용자가 값을 쓸 수 있으면 역직렬화 공격 면이 생깁니다. Redis network를 격리하고 인증·ACL을 적용하며, 다른 애플리케이션과 임의 key 쓰기 권한을 공유하지 않습니다.

JDK serializer도 안전한 allowlist serializer라는 뜻은 아닙니다. 1.12.1 KDoc은 `jdk`를 Jackson과 연결하지만 실제 구현은 `LettuceBinaryCodecs.jdk()`를 호출하므로 이 매뉴얼은 코드 동작을 기준으로 설명합니다.

## Source와 tests

- [`LettuceNearCacheProperties.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt)
- [`LettuceNearCachePropertiesTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCachePropertiesTest.kt)
- [`LettuceBinaryCodecs.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt)
