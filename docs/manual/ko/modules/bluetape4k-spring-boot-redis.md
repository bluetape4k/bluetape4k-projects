---
manualId: bluetape4k-spring-boot-redis
title: "Spring Data Redis 코루틴 지원"
description: "Spring Data Redis의 직렬화 형식을 Kryo·Fory와 압축 조합으로 설계하고 RedisTemplate·ReactiveRedisTemplate에 안전하게 적용하는 방법을 설명합니다."
kind: library
group: spring
learningOrder: 940
---

# Spring Data Redis 코루틴 지원

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-redis`는 Spring Data Redis의 `RedisSerializer`와 `RedisSerializationContext`를 bluetape4k binary serializer·compressor에 연결합니다. 객체를 Kryo 또는 Fory로 직렬화하고 GZip, LZ4, Snappy, Zstd 중 하나로 압축하는 조합을 제공하며, `RedisTemplate`과 `ReactiveRedisTemplate`의 key·value·hash 직렬화 규칙을 같은 코드에서 구성할 수 있습니다.

이 모듈은 Redis client나 cache 구현이 아닙니다. 1.12.1에는 `@AutoConfiguration`, `@ConfigurationProperties`, `RedisTemplateCustomizer`도 없습니다. Spring Boot가 만든 connection factory를 사용해 애플리케이션이 template bean과 serializer를 명시적으로 조립해야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- Redis에 객체를 저장해야 하는지, JSON처럼 사람이 읽을 수 있는 형식이 필요한지 먼저 정합니다.
- 새 저장 형식에는 Kryo 또는 Fory를 사용합니다. JDK serializer 조합은 역직렬화 보안 위험 때문에 deprecated입니다.
- serializer와 compressor 조합은 Redis에 남는 wire format입니다. 배포마다 임의로 바꾸지 않습니다.
- key는 `StringRedisSerializer.UTF_8` 같은 안정적인 형식을 쓰고 객체 serializer는 value와 hash value에 한정하는 편이 안전합니다.
- 동기 `RedisTemplate`과 reactive template이 같은 key 공간을 공유한다면 네 serialization slot을 모두 동일하게 맞춥니다.
- 이 모듈이 값을 byte array로 바꾸는 책임과 Redis connection·timeout·retry·cache 일관성 책임을 구분합니다.

## 의존성 추가 {#coordinates}

사용자는 Spring Data Redis와 codec의 세부 버전을 따로 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis")
}
```

모듈은 문서화된 Fory·Kryo와 LZ4·Snappy·Zstd runtime 의존성을 함께 게시합니다. Gradle project path는 `:bluetape4k-spring-boot-redis`, source directory는 `spring-boot/redis`입니다.

## 첫 ReactiveRedisTemplate {#quick-start}

Spring Boot가 제공하는 `ReactiveRedisConnectionFactory`를 받아 serialization context와 template을 직접 등록합니다.

```kotlin
@Configuration
class RedisTemplateConfig {

    @Bean
    fun objectRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, Any> {
        val context = redisSerializationContextOf<Any>(
            valueSerializer = RedisBinarySerializers.LZ4Fory,
        )
        return ReactiveRedisTemplate(factory, context)
    }
}
```

`redisSerializationContextOf<Any>` overload는 key와 hash key에 UTF-8 String serializer를, value와 hash value에 전달한 serializer를 설정합니다. 이 bean의 connection은 Spring이 관리하지만 Redis에 저장된 byte format은 애플리케이션이 관리합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| 객체 직렬화 | `RedisBinarySerializer` | 내부 `BinarySerializer`의 형식과 type 호환성을 그대로 따릅니다. |
| 표준 조합 선택 | `RedisBinarySerializers` | singleton은 lazy 생성되며 조합 이름이 wire format 계약이 됩니다. |
| 이미 byte array인 값 압축 | `RedisCompressSerializer` | 객체 직렬화는 하지 않고 압축·복원만 수행합니다. |
| reactive context 전체 구성 | `redisSerializationContext {}` | key/value/hash/string slot을 직접 채웁니다. |
| key·value serializer 지정 | `redisSerializationContextOf(K, V)` | 같은 serializer를 hash key/value에도 기본 적용합니다. |
| String key template | `redisSerializationContextOf<V>(valueSerializer)` | key와 hash key는 UTF-8 String으로 고정됩니다. |
| 동기 template | `RedisTemplate`의 serializer property | 이 모듈은 template bean을 자동 등록하지 않습니다. |

## 학습 경로 {#concepts}

아래 장은 1.12.1 배포 소스와 테스트를 따라 모듈 경계부터 저장 형식 변경과 운영 검증까지 설명합니다. serializer 이름만 나열하지 않고 실제 template 구성, null 계약, runtime classpath, 보안과 마이그레이션 절차를 함께 다룹니다.

1. [모듈 경계와 의존성](./bluetape4k-spring-boot-redis/module-boundary-dependencies.md) — 이 모듈이 제공하는 것과 자동 설정·connection·cache 계층에서 제공하지 않는 것을 구분합니다.
2. [Binary serializer 선택](./bluetape4k-spring-boot-redis/binary-serializers.md) — Kryo·Fory·JDK 조합, lazy singleton과 null 계약을 확인합니다.
3. [압축과 Redis wire format](./bluetape4k-spring-boot-redis/compression-wire-format.md) — 압축 전용 serializer와 객체 직렬화+압축 조합, 크기·CPU·호환성 판단 기준을 설명합니다.
4. [SerializationContext와 template 구성](./bluetape4k-spring-boot-redis/serialization-context-templates.md) — DSL과 두 convenience overload로 동기·reactive template을 구성합니다.
5. [보안, 호환성과 마이그레이션](./bluetape4k-spring-boot-redis/migration-security-compatibility.md) — JDK 역직렬화 위험, rolling deployment와 cache key/version 전략을 다룹니다.
6. [테스트, 운영과 생태계 경로](./bluetape4k-spring-boot-redis/testing-operations-ecosystem.md) — unit·consumer runtime 검증에서 Lettuce, Redisson, cache와 workshop으로 이어집니다.

처음 도입한다면 1→2→4 순서로 template 하나를 완성한 뒤 5장에서 배포 호환성을 확인합니다. 이미 운영 중인 Redis 형식을 바꾸려는 경우에는 5장을 먼저 읽고 새 key 공간이나 이중 읽기 전략을 정합니다.

## 권장 패턴 {#patterns}

key prefix와 serializer 조합을 하나의 schema version으로 관리합니다. 예를 들어 `user:v2:*`에 `LZ4Fory`를 쓰기로 했다면 모든 writer와 reader가 함께 바뀔 때까지 기존 `user:v1:*`를 유지합니다. serializer 교체는 코드 refactoring이 아니라 저장 데이터 migration입니다.

`RedisBinarySerializers` 상수는 조립 비용을 줄여주지만 형식 선택을 대신해 주지는 않습니다. 작은 값에는 압축 비용이 더 클 수 있으므로 대표 payload로 크기와 latency를 측정한 뒤 압축을 선택합니다. 사용자 입력이나 외부 producer가 Redis 값을 쓸 수 있다면 역직렬화 가능한 type과 key space를 더 엄격하게 제한합니다.

## 연동 {#integrations}

Spring Data Redis connection factory, repository와 template 실행 모델은 Spring Boot가 담당합니다. low-level Lettuce helper는 [`bluetape4k-lettuce`](./bluetape4k-lettuce.md), Redisson distributed object와 codec은 [`bluetape4k-redisson`](./bluetape4k-redisson.md)에서 설명합니다.

JCache, memoizer와 Near Cache가 필요하다면 [`bluetape4k-cache-lettuce`](./bluetape4k-cache-lettuce.md) 또는 [`bluetape4k-cache-redisson`](./bluetape4k-cache-redisson.md)을 선택합니다. Spring의 Redis cache abstraction 예제는 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)에서 이어갈 수 있습니다. serializer 모듈을 추가했다고 cache-aside나 write-through가 자동으로 생기지는 않습니다.

## 설정 {#configuration}

1.12.1에는 이 모듈 전용 property가 없습니다. Redis endpoint, pool, SSL, timeout은 Spring Boot의 Spring Data Redis 설정을 사용합니다. 이 모듈에서 애플리케이션이 정할 값은 template별 serializer와 serialization context입니다.

```kotlin
val context = redisSerializationContext<String, Any> {
    key(RedisSerializer.string())
    value(RedisBinarySerializers.ZstdFory)
    hashKey(RedisSerializer.string())
    hashValue(RedisBinarySerializers.ZstdFory)
}
```

`defaultSerializer`는 context builder의 시작값일 뿐입니다. key·value·hash slot을 명시적으로 덮어쓰면 해당 설정이 우선합니다.

## 실패 동작 {#failures}

`RedisBinarySerializer.serialize(null)`과 `RedisCompressSerializer.serialize(null)`은 빈 byte array를 반환합니다. binary serializer의 `deserialize(null)`과 compress serializer의 `deserialize(null)`은 `null`을 반환합니다. 빈 byte array와 업무상 null을 같은 의미로 저장하면 구분할 수 없으므로 null 저장 정책을 애플리케이션에서 정해야 합니다.

codec이 읽을 수 없는 기존 값, 누락된 runtime codec, 압축 형식 불일치와 type schema 변화는 deserialize 예외로 드러납니다. 이 모듈은 실패를 다른 serializer로 자동 재시도하거나 오래된 형식을 추측하지 않습니다. Redis 연결 오류와 timeout도 Spring Data Redis 계층에서 처리되며 serializer가 fallback을 제공하지 않습니다.

## 운영 {#operations}

직렬화 실패와 역직렬화 실패를 Redis command 실패와 분리해 집계합니다. payload 원문을 log에 남기기보다 key prefix, serializer schema version, payload byte size, 예외 type을 기록합니다. 압축은 전후 크기와 CPU 시간, command latency를 함께 측정해야 이득을 판단할 수 있습니다.

rolling deployment에서는 새 writer가 만든 값을 이전 reader가 읽을 수 있는지 먼저 검증합니다. 호환되지 않는 변경이라면 새 key prefix를 사용하고 TTL이나 명시적인 migration으로 기존 값을 정리합니다.

## 테스트 {#testing}

serializer와 context unit test는 Redis 서버 없이 실행됩니다. 모듈의 `check`는 배포 runtime classpath에 Fory·Kryo와 compressor가 실제로 포함되는지 `consumerRuntimeTest`로 추가 검증합니다.

```bash
./gradlew :bluetape4k-spring-boot-redis:test \
    :bluetape4k-spring-boot-redis:consumerRuntimeTest
```

애플리케이션에서는 writer와 reader가 다른 version일 때의 교차 호환성, 실제 Redis round trip, key·value·hash slot, corrupted payload와 null 정책을 별도로 테스트합니다.

## 워크숍 {#workshops}

모듈 내부에서는 `RedisSerializationContextSupportTest`를 가장 작은 DSL 실습으로, `RedisConsumerRuntimeClasspathTest`를 실제 consumer dependency 검증으로 사용할 수 있습니다. 그다음 [bluetape4k-workshop](https://github.com/bluetape4k/bluetape4k-workshop)의 Spring Boot Redis 예제에서 connection 설정, cache abstraction과 애플리케이션 경계를 함께 확인합니다.

Near Cache나 분산 memoizer를 실습하려면 cache-lettuce 또는 cache-redisson 매뉴얼로 이동합니다. 이 모듈의 serializer를 그 cache provider의 codec과 무조건 혼용하지 말고 각 저장 형식 경계를 따로 확인합니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. 제공되는 production API는 네 Kotlin source file의 serializer와 context helper입니다.

자동 설정, customizer, properties, health indicator, metrics, Redis client wrapper와 cache 구현은 이 모듈의 1.12.1 범위가 아닙니다. README의 Spring Boot 4 표현은 의존성 line을 뜻하며 자동 설정 모듈이라는 뜻이 아닙니다.

## Source와 tests {#sources}

- [`build.gradle.kts`](../../../../spring-boot/redis/build.gradle.kts)
- [`RedisBinarySerializer.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializer.kt)
- [`RedisBinarySerializers.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializers.kt)
- [`RedisCompressSerializer.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisCompressSerializer.kt)
- [`RedisSerializationContextSupport.kt`](../../../../spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer/RedisSerializationContextSupport.kt)
- [`RedisBinarySerializerTest.kt`](../../../../spring-boot/redis/src/test/kotlin/io/bluetape4k/spring/redis/serializer/RedisBinarySerializerTest.kt)
- [`RedisConsumerRuntimeClasspathTest.kt`](../../../../spring-boot/redis/src/consumerRuntimeTest/kotlin/io/bluetape4k/spring/redis/serializer/RedisConsumerRuntimeClasspathTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot Redis Serializer 클래스 구조 다이어그램

[![Spring Boot Redis Serializer 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-01.svg)

_배포본 README: [`spring-boot/redis/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/redis/README.ko.md)_

### ReactiveRedisTemplate 직렬화 흐름 다이어그램

[![ReactiveRedisTemplate 직렬화 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-redis-diagram-02.svg)

_배포본 README: [`spring-boot/redis/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/redis/README.ko.md)_

<!-- release-readme-diagrams:end -->
