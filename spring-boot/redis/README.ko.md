# bluetape4k-spring-boot-redis

[English](./README.md) | 한국어

Spring Data Redis의 직렬화 계층을 고성능 바이너리 직렬화/압축 조합으로 대체할 수 있는 모듈입니다 (Spring Boot 4.x).

`RedisTemplate` / `ReactiveRedisTemplate` 설정 시 Serializer와 `RedisSerializationContext`를 간편하게 구성할 수 있습니다.

> Spring Boot 4 기반 versionless 표준 구현입니다.

## 주요 기능

| 클래스 / 함수                           | 설명                                                       |
|------------------------------------|----------------------------------------------------------|
| `RedisBinarySerializer`            | `BinarySerializer` 기반 `RedisSerializer<Any>` 구현          |
| `RedisCompressSerializer`          | `Compressor` 기반 압축 전용 `RedisSerializer<ByteArray>`       |
| `RedisBinarySerializers`           | 직렬화(Jdk/Kryo/Fory) × 압축(GZip/LZ4/Snappy/Zstd) 조합 싱글턴 팩토리 |
| `redisSerializationContext {}`     | DSL 기반 `RedisSerializationContext` 빌더                    |
| `redisSerializationContextOf(...)` | 키/값 Serializer를 직접 지정하는 편의 함수                            |

## 아키텍처 다이어그램

### Redis Serializer 클래스 구조

![Spring Boot Redis Serializer 클래스 구조 다이어그램](../../docs/images/readme-diagrams/spring-boot-redis-diagram-01.png)

### ReactiveRedisTemplate 직렬화 흐름

![ReactiveRedisTemplate 직렬화 흐름 다이어그램](../../docs/images/readme-diagrams/spring-boot-redis-diagram-02.png)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis:${bluetape4kVersion}")
}
```

이 모듈은 아래 `RedisBinarySerializers` 목록에 나온 Kryo/Fory 및
LZ4/Snappy/Zstd 조합의 런타임 의존성을 함께 게시합니다. 표시된 serializer 조합을
사용하기 위해 별도 codec 또는 compressor 의존성을 추가할 필요가 없습니다.

## 사용 예시

### ReactiveRedisTemplate 설정 (DSL 방식)

```kotlin
@Configuration
class RedisConfig {

    @Bean
    fun reactiveRedisTemplate(
        factory: ReactiveRedisConnectionFactory,
    ): ReactiveRedisTemplate<String, Any> {
        val context = redisSerializationContext<String, Any> {
            key(RedisSerializer.string())
            value(RedisBinarySerializers.LZ4Fory)
            hashKey(RedisSerializer.string())
            hashValue(RedisBinarySerializers.LZ4Fory)
        }
        return ReactiveRedisTemplate(factory, context)
    }
}
```

### ReactiveRedisTemplate 설정 (편의 함수 방식)

```kotlin
@Bean
fun reactiveRedisTemplate(
    factory: ReactiveRedisConnectionFactory,
): ReactiveRedisTemplate<String, ByteArray> {
    val context = redisSerializationContextOf<ByteArray>(
        valueSerializer = RedisBinarySerializers.LZ4Kryo,
    )
    return ReactiveRedisTemplate(factory, context)
}
```

### RedisTemplate 설정

```kotlin
@Bean
fun redisTemplate(factory: RedisConnectionFactory): RedisTemplate<String, Any> {
    return RedisTemplate<String, Any>().apply {
        connectionFactory = factory
        keySerializer = RedisSerializer.string()
        valueSerializer = RedisBinarySerializers.LZ4Fory
        hashKeySerializer = RedisSerializer.string()
        hashValueSerializer = RedisBinarySerializers.LZ4Fory
    }
}
```

## Serializer 목록

### 직렬화 (객체 → ByteArray)

JDK 역직렬화는 Redis에 저장된 값이 gadget chain 기반 RCE 위험에 노출될 수 있습니다. JDK
serializer 상수는 deprecated 상태이며, 저장된 Redis 데이터가 완전히 신뢰 가능한 경우에만
사용하세요. 일반 Redis 객체 값에는 Kryo 또는 Fory를 권장합니다.

| 상수                                  | 직렬화 엔진 | 압축     | 상태                 |
|-------------------------------------|--------|--------|--------------------|
| `RedisBinarySerializers.Jdk`        | JDK    | 없음     | Deprecated; 신뢰 데이터 전용 |
| `RedisBinarySerializers.Kryo`       | Kryo   | 없음     | 권장                 |
| `RedisBinarySerializers.Fory`       | Fory   | 없음     | 권장                 |
| `RedisBinarySerializers.GzipJdk`    | JDK    | GZip   | Deprecated; 신뢰 데이터 전용 |
| `RedisBinarySerializers.LZ4Jdk`     | JDK    | LZ4    | Deprecated; 신뢰 데이터 전용 |
| `RedisBinarySerializers.SnappyJdk`  | JDK    | Snappy | Deprecated; 신뢰 데이터 전용 |
| `RedisBinarySerializers.ZstdJdk`    | JDK    | Zstd   | Deprecated; 신뢰 데이터 전용 |
| `RedisBinarySerializers.GzipKryo`   | Kryo   | GZip   | 권장                 |
| `RedisBinarySerializers.LZ4Kryo`    | Kryo   | LZ4    | 권장                 |
| `RedisBinarySerializers.SnappyKryo` | Kryo   | Snappy | 권장                 |
| `RedisBinarySerializers.ZstdKryo`   | Kryo   | Zstd   | 권장                 |
| `RedisBinarySerializers.GzipFory`   | Fory   | GZip   | 권장                 |
| `RedisBinarySerializers.LZ4Fory`    | Fory   | LZ4    | 권장                 |
| `RedisBinarySerializers.SnappyFory` | Fory   | Snappy | 권장                 |
| `RedisBinarySerializers.ZstdFory`   | Fory   | Zstd   | 권장                 |

### 압축 전용 (ByteArray → ByteArray)

| 상수                              | 압축 알고리즘 |
|---------------------------------|---------|
| `RedisBinarySerializers.LZ4`    | LZ4     |
| `RedisBinarySerializers.Zstd`   | Zstd    |
| `RedisBinarySerializers.Snappy` | Snappy  |
| `RedisBinarySerializers.Gzip`   | GZip    |

## 빌드 및 테스트

```bash
./gradlew :bluetape4k-spring-boot-redis:test
```
