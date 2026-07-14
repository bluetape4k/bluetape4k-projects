---
title: 테스트·운영·생태계 경로
description: 우산 모듈의 빈 테스트 task를 과장하지 않고 하위 client의 실제 테스트와 운영·학습 경로를 연결합니다.
manualId: bluetape4k-redis
chapterId: testing-operations-ecosystem
---

# 테스트·운영·생태계 경로

## 테스트 책임은 실제 client에 있다

`infra/redis`에는 production source와 test source가 없습니다. 그래서 `:bluetape4k-redis:test`가 성공해도 두 client의 연결, coroutine, Codec이나 분산 객체 동작을 검증했다는 뜻은 아닙니다. 사용한 하위 모듈의 테스트와 애플리케이션 통합 테스트를 실행해야 합니다.

```bash
./gradlew :bluetape4k-lettuce:test --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-redisson:test --no-build-cache --no-configuration-cache
```

두 suite는 실제 Redis를 Testcontainers로 띄웁니다. 다른 DB·broker Testcontainers suite와 병렬 실행하지 않습니다. 이 매뉴얼처럼 문서만 바꿀 때는 heavy suite 대신 배포 source link, locale 구조와 Markdown 검증을 실행합니다.

## 작은 학습 단위

| 배우려는 계약 | 대표 테스트 |
| --- | --- |
| Lettuce client·cached connection·shutdown | [`LettuceClientsTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt) |
| `RedisFuture` 결과·실패 전달 | [`RedisFutureSupportTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupportTest.kt) |
| Redisson client 구성·batch·transaction | [`RedissonClientSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupportTest.kt) |
| Stream group·consumer helper | [`RStreamSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt) |

우산 모듈 자체 예제처럼 묶어 설명하지 않고, 실제 소유 module의 테스트에서 API와 실패 조건을 읽습니다.

## 운영 baseline

두 client를 함께 운용한다면 metrics도 합산하지 말고 client별로 구분합니다.

- connection·pool 사용량과 reconnect
- command latency, timeout과 retry
- pending async 작업과 coroutine cancellation
- Codec decode 실패와 key prefix별 schema version
- Redisson Stream pending entry와 Near Cache stale incident
- shutdown 시간과 처리하지 못한 queue

우산 모듈을 선택 의존성으로 줄인 뒤에는 제거한 client의 thread, connection과 metric이 실제로 사라졌는지 확인합니다.

## 생태계 학습 경로

1. client와 command가 필요하면 [Lettuce 매뉴얼](../bluetape4k-lettuce.md) 또는 [Redisson 매뉴얼](../bluetape4k-redisson.md)을 끝까지 읽습니다.
2. 공통 cache 정책이 필요하면 [Cache Core](../bluetape4k-cache-core.md)에서 cache-aside와 failure 계약을 먼저 익힙니다.
3. provider 구현은 [Lettuce Cache](../bluetape4k-cache-lettuce.md)나 [Redisson Cache](../bluetape4k-cache-redisson.md)로 이어갑니다.
4. Hibernate 2차 cache는 [Hibernate Cache Lettuce](../bluetape4k-hibernate-cache-lettuce.md)에서 region과 transaction 경계를 확인합니다.
5. database repository와 cache를 함께 다루는 실습은 [Exposed Workshop](https://github.com/bluetape4k/exposed-workshop)과 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)에서 진행합니다.

우산 모듈은 이 경로의 기능 계층이 아니라 두 client를 함께 가져오는 build 편의 계층입니다. 학습과 운영은 실제로 선택한 client와 cache provider를 기준으로 나눕니다.

## Release sources

- [`infra/redis/build.gradle.kts`](../../../../../infra/redis/build.gradle.kts)
- [`LettuceClientsTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/LettuceClientsTest.kt)
- [`RedisFutureSupportTest.kt`](../../../../../infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/RedisFutureSupportTest.kt)
- [`RedissonClientSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RedissonClientSupportTest.kt)
- [`RStreamSupportTest.kt`](../../../../../infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/RStreamSupportTest.kt)
