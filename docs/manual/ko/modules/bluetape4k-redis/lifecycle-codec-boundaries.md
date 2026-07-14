---
title: Lifecycle과 Codec 경계
description: Lettuce와 Redisson client의 종료 책임, coroutine 경계와 Redis wire format을 분리해 관리합니다.
manualId: bluetape4k-redis
chapterId: lifecycle-codec-boundaries
---

# Lifecycle과 Codec 경계

## 우산 좌표가 lifecycle을 합치지는 않는다

Lettuce의 `RedisClient`와 Redisson의 `RedissonClient`는 서로 다른 connection과 thread resource를 소유합니다. `bluetape4k-redis`를 추가해도 공통 lifecycle manager가 생기지 않습니다. application context에서 client별 owner를 하나씩 정하고 생성과 종료를 같은 component에 둡니다.

Lettuce의 `LettuceClients.commands(client)`는 client별 cached connection을 재사용합니다. `LettuceClients.shutdown(client)`는 해당 client의 cached connection을 닫습니다. process-wide 공유 resource 종료는 애플리케이션 종료 시점과 맞춰야 합니다.

Redisson factory는 새 `RedissonClient`를 반환하며 호출자가 `shutdown()`해야 합니다. Spring bean으로 만들었다면 destroy method나 lifecycle callback이 실제로 등록됐는지 확인합니다.

## coroutine은 소유권을 바꾸지 않는다

`RedisFuture.awaitSuspending()`과 Redisson `RFuture` adapter는 callback 기반 결과를 suspend 호출로 연결합니다. 이 adapter가 client를 닫거나 Redis 명령의 원자성을 추가하지는 않습니다. timeout과 cancellation이 발생했을 때 서버에서 명령이 이미 실행됐을 가능성도 애플리케이션의 idempotency 설계에 포함합니다.

동기 `get()`을 coroutine dispatcher에서 호출해 놓고 suspend API라고 부르지 않습니다. 선택한 client의 async·coroutine 진입점을 끝까지 유지하고 blocking 경계가 필요하면 전용 dispatcher와 제한된 범위를 둡니다.

## Codec은 Redis에 남는 배포 계약이다

client library를 교체해도 Redis 값은 이전 Codec bytes로 남아 있습니다. serializer class 이름이 비슷하거나 둘 다 Fory·JSON을 사용한다는 사실만으로 wire compatibility가 보장되지 않습니다. 압축 wrapper, type metadata, allow-list와 serializer 설정까지 같아야 합니다.

안전한 전환 방법은 세 가지입니다.

- cache data라면 배포 전 전체 무효화 또는 짧은 TTL로 자연 만료합니다.
- 영속성이 필요한 값은 새 key prefix로 저장하고 backfill한 뒤 read 경로를 전환합니다.
- 호환 decode가 필요하면 실제 이전 bytes를 fixture로 보관하고 새 client에서 contract test를 실행합니다.

## Keyspace와 보안 경계

두 client를 함께 쓸 때는 담당 key prefix를 문서화합니다. 공유 key에는 같은 TTL, serialization, compression과 schema evolution 규칙을 적용합니다. Redis가 외부 입력이나 여러 서비스가 쓰는 신뢰 경계라면 polymorphic decode와 binary fallback을 임의로 넓히지 않습니다.

## 운영 점검

- client별 active connection과 reconnect 횟수
- command timeout과 cancellation 이후 중복 실행 가능성
- Codec decode failure와 fallback 사용량
- shutdown 시 pending command와 worker drain
- 같은 key prefix를 쓰는 배포 버전과 Codec 목록

## Release sources

- [`LettuceClients.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/LettuceClients.kt)
- [`RedisFutureSupport.kt`](../../../../../infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupport.kt)
- [`RedissonClientSupport.kt`](../../../../../infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupport.kt)
- [Lettuce Codec과 직렬화](../bluetape4k-lettuce/codecs-and-serialization.md)
- [Redisson Codec과 보안](../bluetape4k-redisson/codecs-security-wire-format.md)
