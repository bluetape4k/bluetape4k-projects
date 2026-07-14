---
title: 테스트, 운영과 생태계 경로
description: serializer·context·consumer runtime 검증을 애플리케이션 운영 점검으로 확장하고 Lettuce, Redisson, cache와 workshop 경로를 안내합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: testing-operations-ecosystem
---

# 테스트, 운영과 생태계 경로

## 모듈 테스트가 보장하는 것

모듈 unit test는 다음 계약을 Redis server 없이 검증합니다.

- String, data class와 list의 binary round trip
- Kryo·Fory·JDK와 네 compressor 조합
- null serialize·deserialize 계약
- compression-only round trip
- serialization context의 key·value pair
- JDK singleton의 deprecated message와 replacement

```bash
./gradlew :bluetape4k-spring-boot-redis:test
```

이 검증은 serializer 함수의 대칭성을 확인하지만 실제 Redis connection, TTL, transaction, cluster topology와 rolling deployment 호환성을 보장하지 않습니다.

## consumer runtime classpath

codec을 `runtimeOnly`로 게시하면 module unit test만으로 consumer가 실제 codec을 받을지 확인하기 어렵습니다. `consumerRuntimeTest` source set은 published consumer와 비슷한 runtime classpath에서 `LZ4Fory`와 `LZ4Kryo` round trip을 실행합니다.

```bash
./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest
```

이 task는 module `check`에 연결되어 있습니다. 새 표준 serializer 조합을 문서화한다면 runtime dependency와 consumer test도 함께 확인해야 합니다.

## 애플리케이션 통합 테스트

실제 서비스에서는 다음 경계를 추가합니다.

1. 동기 template이 쓴 값을 reactive template이 읽습니다.
2. value와 hash value를 모두 round trip합니다.
3. 이전·새 애플리케이션 버전 사이의 교차 읽기를 검증합니다.
4. corrupted, truncated, 다른 compressor payload의 실패 정책을 확인합니다.
5. null, key 부재와 빈 byte array를 구분합니다.
6. 실제 Redis command timeout이 serializer 실패와 구분되는지 확인합니다.

Redis Testcontainers를 사용한다면 다른 heavy integration suite와 순차 실행합니다.

## 운영 지표

serializer별 encode/decode failure, payload byte size, Redis command latency와 application CPU를 함께 봅니다. 압축률만 높고 CPU가 크게 늘 수 있고, 반대로 network latency가 큰 환경에서는 압축이 유리할 수 있습니다.

class 이름, field와 serializer 조합을 log label로 무제한 남기면 cardinality가 커집니다. schema version과 제한된 serializer ID를 metric tag로 사용하고 상세 예외는 sampled log에서 확인합니다.

## 장애 대응

새 배포 뒤 deserialize 실패가 증가하면 먼저 writer/reader version과 key prefix를 확인합니다. connection 장애와 섞지 않고 serializer exception을 분리합니다. 새 형식만 문제라면 writer를 멈추고 v2 key를 격리한 뒤 구 key fallback을 유지합니다.

자동으로 모든 Redis key를 삭제하는 복구는 피합니다. 해당 prefix와 TTL, 원본 저장소에서 값을 다시 만드는 비용을 확인한 뒤 범위를 제한합니다.

## 생태계 학습 경로

- Spring Data Redis serializer와 template: 이 매뉴얼
- Lettuce command·coroutine·codec: [`bluetape4k-lettuce`](../bluetape4k-lettuce.md)
- Redisson client·distributed object·codec: [`bluetape4k-redisson`](../bluetape4k-redisson.md)
- JCache·memoizer·Near Cache: [`bluetape4k-cache-lettuce`](../bluetape4k-cache-lettuce.md), [`bluetape4k-cache-redisson`](../bluetape4k-cache-redisson.md)
- 실행 가능한 Spring Boot Redis 예제: [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)

serializer는 cache 전략의 한 부분일 뿐입니다. cache-aside, read-through, write-through와 invalidation은 cache·repository 경계에서 별도로 설계합니다.

## Source와 tests

- [`build.gradle.kts`](../../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializersTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializersTest.kt)
- [`RedisSerializationContextSupportTest.kt`](../../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupportTest.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)
