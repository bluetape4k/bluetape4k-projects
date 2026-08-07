---
title: 모듈 경계와 의존성
description: Spring Boot Redis 모듈이 실제로 제공하는 serializer API와 자동 설정·client·cache 계층의 책임을 구분합니다.
manualId: bluetape4k-spring-boot-redis
chapterId: module-boundary-dependencies
---

# 모듈 경계와 의존성

## 이름보다 source를 먼저 보기

`bluetape4k-spring-boot-redis`의 production source는 네 파일뿐입니다. `RedisBinarySerializer`, `RedisCompressSerializer`, 미리 조합한 `RedisBinarySerializers`, `RedisSerializationContext` helper를 제공합니다.

반대로 다음 기능은 1.12.1에 없습니다.

- `@AutoConfiguration`이나 starter metadata
- 이 모듈이 등록하는 `RedisTemplate`·`ReactiveRedisTemplate` bean
- 전용 `@ConfigurationProperties`와 template customizer
- Redis client 생성, endpoint·pool·SSL·timeout 설정
- cache-aside, JCache, memoizer, Near Cache 또는 Pub/Sub invalidation

즉 Spring Boot의 connection factory 위에 직렬화 규칙을 올리는 얇은 integration library입니다.

## 의존성이 가져오는 범위

build script는 Spring Data Redis starter를 API로 노출하고 `bluetape4k-core`, `bluetape4k-io`를 연결합니다. 문서에서 제공하는 Fory·Kryo와 LZ4·Snappy·Zstd 조합이 consumer runtime에서도 동작하도록 codec과 compressor를 runtime dependency로 게시합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis")
}
```

애플리케이션은 세부 library version을 직접 적지 않습니다. 다만 artifact를 추가한다고 Redis server나 connection 설정이 생기는 것은 아닙니다.

## Spring Data Redis와의 역할 분담

Spring Boot는 `RedisConnectionFactory`와 `ReactiveRedisConnectionFactory`를 구성합니다. Spring Data Redis는 template, command execution과 exception translation을 제공합니다. 이 모듈은 template이 key와 value를 byte array로 바꾸고 되돌리는 방법만 제공합니다.

```text
application object
  -> bluetape4k RedisSerializer
  -> Spring Data Redis template
  -> connection factory / Lettuce driver
  -> Redis server
```

serializer가 Redis 연결을 소유하지 않으므로 닫을 resource도 없습니다. connection factory와 template bean의 수명은 Spring container가 관리합니다.

## 다른 Redis 모듈과 구분하기

`bluetape4k-lettuce`는 Lettuce command, coroutine과 codec helper를 제공합니다. `bluetape4k-redisson`은 Redisson client, distributed object, codec과 coroutine API를 다룹니다. `bluetape4k-cache-lettuce`와 `bluetape4k-cache-redisson`은 JCache, memoizer와 Near Cache를 구현합니다.

이 모듈을 선택하는 질문은 “어떤 Redis client를 쓸까?”가 아니라 “Spring Data Redis template의 value를 어떤 byte format으로 저장할까?”입니다.

## 도입 전 확인표

- Spring Boot가 구성할 Redis driver와 connection factory를 선택했는가?
- 동기, reactive 또는 둘 다 사용할지 정했는가?
- key와 hash key 형식을 고정했는가?
- value serializer와 compressor 조합을 schema로 기록했는가?
- 기존 Redis 값과 새 reader의 호환성을 검증했는가?
- JCache나 Near Cache가 필요하다면 별도 cache 모듈을 선택했는가?

## Source와 tests

- [`build.gradle.kts`](../../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializer.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisSerializationContextSupport.kt`](../../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)
