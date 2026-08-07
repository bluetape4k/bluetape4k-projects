---
title: Lifecycle·테스트·생태계 경로
description: Redisson resource lifecycle, 운영 지표, Testcontainers 검증과 cache-redisson·Exposed·workshop 연결을 설명합니다.
manualId: bluetape4k-redisson
chapterId: lifecycle-testing-ecosystem
---

# Lifecycle·테스트·생태계 경로

## 어떤 계층을 선택할까

| 필요한 수준 | 선택 | 맡는 일 |
| --- | --- | --- |
| Redisson object 직접 사용 | `bluetape4k-redisson` | client, Codec, batch/transaction, Stream, coroutine, local cached map |
| Spring Cache annotation | [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md) | `CacheManager`, cache region과 Spring Cache 연동 |
| DB repository cache | [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed) | loader/writer와 repository를 연결한 entity cache |
| 단계별 실습 | [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop) | cache-aside, read/write-through, Near Cache와 측정 |

상위 계층으로 올라가도 Redisson client, Codec, invalidation과 shutdown 경계는 남습니다. 문제를 진단하려면 이 모듈의 기본 계약을 이해해야 합니다.

## 시작과 종료 순서

1. 설정과 Codec을 확정합니다.
2. client를 한 번 만들고 health check를 통과시킵니다.
3. map, listener와 near-cache reference를 application component에 연결합니다.
4. 종료 시 새 request를 막고 write-behind·consumer work를 drain합니다.
5. near-cache instance를 `destroy()`하고 client를 `shutdown()`합니다.

Spring이 client를 소유하면 bean lifecycle에 맡기고 수동으로 중복 종료하지 않습니다. 직접 만든 client는 test에서도 반드시 닫아 thread와 connection 누수를 막습니다.

## 관찰할 지표

- connected node와 connection pool 사용량
- command p50/p95/p99, timeout과 retry
- batch command 수와 transaction rollback
- Stream pending count, oldest idle time, claim과 duplicate 처리
- local cache hit ratio, invalidation rate와 reconnect
- write-behind queue 크기, oldest age, writer failure와 drain 시간
- encode/decode failure와 payload size

cache hit ratio만 높아도 stale incident가 늘면 좋은 상태가 아닙니다. hit ratio와 source-of-truth freshness를 함께 확인합니다.

## 1.12.1 테스트 읽는 순서

`AbstractRedissonTest`는 Redis Testcontainer와 공용 client fixture를 제공합니다. 그 위에서 다음 test를 순서대로 읽으면 기능과 경계가 이어집니다.

1. `RedissonClientSupportTest` — client와 YAML configuration
2. `RedissonClientExtensionsTest` — batch와 transaction
3. `RFutureSupportTest`, `RedissonClientCoroutineTest` — future와 cancellation
4. `LocalCacheMapSupportTest`, `RedissonNearCacheTest` — local cache와 destroy
5. `RedissonCacheConfigTest` — validation과 unsupported options
6. Codec tests — round trip, allow-list, fallback와 압축 상한

```bash
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

Docker를 사용하는 Testcontainers task이므로 다른 DB·Redis suite와 차례로 실행합니다. 실제 장애 검증에는 Redis restart, Pub/Sub disconnect, latency injection과 process shutdown을 추가합니다.

## Workshop에서 확인할 것

workshop 예제에서는 API 이름보다 data flow를 먼저 그립니다. 첫 요청은 DB에서 읽고 cache에 넣는지, miss가 loader로 넘어가는지, write가 writer를 거치는지 확인합니다. benchmark가 있다면 throughput은 높을수록, latency는 낮을수록 좋다는 방향과 환경·payload·stale 허용 범위를 함께 기록합니다.

## 관련 경로

- [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md)
- [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md)
- [bluetape4k-exposed](https://github.com/bluetape4k/bluetape4k-exposed)
- [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)
- [bluetape4k Workshop](https://github.com/bluetape4k/bluetape4k-workshop)
- [bluetape4k 생태계 지도](https://bluetape4k.github.io/ko/ecosystem/atlas/)
