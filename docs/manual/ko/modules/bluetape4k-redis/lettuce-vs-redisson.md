---
title: Lettuce와 Redisson 선택
description: Redis command 중심 요구사항과 분산 객체 중심 요구사항을 구분해 client를 선택합니다.
manualId: bluetape4k-redis
chapterId: lettuce-vs-redisson
---

# Lettuce와 Redisson 선택

## library 이름보다 필요한 추상화를 본다

Lettuce와 Redisson은 같은 Redis에 연결하지만 제공하는 주 추상화가 다릅니다. 먼저 애플리케이션이 Redis command를 직접 조합하려는지, 분산 객체와 lifecycle이 포함된 높은 수준의 abstraction을 원하는지 정합니다.

| 요구사항 | Lettuce | Redisson |
| --- | --- | --- |
| sync·async Redis command와 pipeline | 주 경로 | 지원하지만 이 매뉴얼의 helper 중심은 아님 |
| `RedisFuture`를 coroutine에서 대기 | `awaitSuspending`, coroutine commands | `RFuture` adapter와 suspend batch·transaction |
| 객체 Codec 조합 | `LettuceBinaryCodecs`, JSON·Protobuf | `RedissonCodecs`, JSON·Fory·압축 Codec |
| 분산 lock·map·queue 같은 객체 | raw command로 직접 설계 | Redisson 분산 객체 사용 |
| Stream consumer-group helper | command API에서 구성 | 검증을 포함한 `RStreamSupport` helper |
| local cached map | 1.12.1 loaded-map 범위 제한 확인 | `RLocalCachedMap` 기반 Near Cache |

## Lettuce가 맞는 경우

Redis command, pipeline, 낮은 수준의 key 설계가 서비스 코드의 중심이면 Lettuce가 단순합니다. `LettuceClients`가 client와 command 진입점을 제공하고 `RedisFutureSupport`가 future를 suspend 경계로 연결합니다.

다만 coroutine adapter를 쓴다고 Redis 작업 자체가 취소 가능한 transaction으로 바뀌지는 않습니다. connection과 cached command의 소유권, timeout과 재연결은 [Client와 connection](../bluetape4k-lettuce/clients-and-connections.md)에서 먼저 정합니다.

## Redisson이 맞는 경우

분산 lock, map, Stream, batch·transaction이나 local cached map이 필요하면 Redisson의 객체 모델을 직접 사용하는 편이 낫습니다. `redissonClient {}` DSL은 client 생성을 줄여주지만 생성한 client의 종료 책임은 애플리케이션에 남습니다.

Near Cache를 선택할 때는 단순히 빠른 map으로 보지 않습니다. Pub/Sub 무효화, reconnect 이후 local entry 처리, Codec과 cache name 일치를 운영 계약에 포함해야 합니다. [Local Cached Map과 무효화](../bluetape4k-redisson/local-cache-pubsub-invalidation.md)에서 이 경계를 확인합니다.

## 둘 다 쓰는 경우

한 서비스에서 Lettuce command와 Redisson lock을 함께 쓸 수 있습니다. 이 경우 client pool, retry, timeout과 shutdown을 각각 관리합니다. 동일 keyspace를 공유한다면 두 Codec이 같은 bytes를 읽고 쓸 수 있는지 테스트하지 않고 가정하면 안 됩니다.

두 client를 장애 fallback 관계로 두는 설계는 특히 조심해야 합니다. 첫 명령이 서버에서 성공했지만 client가 timeout으로 판단한 뒤 다른 client가 같은 명령을 다시 실행할 수 있습니다. idempotency와 결과 확인 방법이 없다면 자동 fallback을 두지 않습니다.

## 다음 선택

- Lettuce를 골랐다면 [Command와 coroutine](../bluetape4k-lettuce/commands-and-coroutines.md)으로 이동합니다.
- Redisson을 골랐다면 [Client와 분산 객체·Stream](../bluetape4k-redisson/client-distributed-objects-streams.md)부터 읽습니다.
- 두 구현을 함께 쓰던 코드에서 하나를 제거하려면 [선택 의존성으로 전환](./selective-dependency-migration.md)을 따릅니다.

## Release sources

- [`LettuceClients.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`RedisFutureSupport.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`RedissonClientSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [`RedissonClientCoroutine.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt)
