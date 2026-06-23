# bluetape4k-spring-boot-redis

English | [한국어](./README.ko.md)

A module that replaces Spring Data Redis's serialization layer with high-performance binary serialization and compression combinations (Spring Boot 4.x).

Provides a convenient way to configure `Serializer` and `RedisSerializationContext` when setting up `RedisTemplate` /
`ReactiveRedisTemplate`.

> This is the versionless Spring Boot 4 implementation.

## Key Features

| Class / Function                   | Description                                                                                  |
|------------------------------------|----------------------------------------------------------------------------------------------|
| `RedisBinarySerializer`            | `RedisSerializer<Any>` implementation backed by `BinarySerializer`                           |
| `RedisCompressSerializer`          | Compression-only `RedisSerializer<ByteArray>` backed by `Compressor`                         |
| `RedisBinarySerializers`           | Singleton factory combining serializers (Jdk/Kryo/Fory) × compressors (GZip/LZ4/Snappy/Zstd) |
| `redisSerializationContext {}`     | DSL-based `RedisSerializationContext` builder                                                |
| `redisSerializationContextOf(...)` | Convenience function to specify key/value serializers directly                               |

## Architecture Diagrams

### Redis Serializer Class Structure

![Redis Serializer Class Structure diagram](../../docs/images/readme-diagrams/spring-boot-redis-diagram-01.png)

### ReactiveRedisTemplate Serialization Flow

![ReactiveRedisTemplate Serialization Flow diagram](../../docs/images/readme-diagrams/spring-boot-redis-diagram-02.png)

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis:${bluetape4kVersion}")
}
```

The module publishes runtime dependencies for the documented
`RedisBinarySerializers` Kryo/Fory and LZ4/Snappy/Zstd combinations. Consumers
do not need to add separate codec or compressor dependencies for the serializer
matrix shown below.

## Usage Examples

### ReactiveRedisTemplate Configuration (DSL approach)

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

### ReactiveRedisTemplate Configuration (convenience function approach)

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

### RedisTemplate Configuration

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

## Serializer Reference

### Object Serializers (Object → ByteArray)

JDK deserialization can expose Redis values to RCE gadget-chain risk. The JDK
serializer constants are deprecated and should be used only when the stored Redis
data is fully trusted. Prefer Kryo or Fory for general Redis object values.

| Constant                            | Serialization Engine | Compression | Status |
|-------------------------------------|----------------------|-------------|--------|
| `RedisBinarySerializers.Jdk`        | JDK                  | None        | Deprecated; trusted data only |
| `RedisBinarySerializers.Kryo`       | Kryo                 | None        | Recommended |
| `RedisBinarySerializers.Fory`       | Fory                 | None        | Recommended |
| `RedisBinarySerializers.GzipJdk`    | JDK                  | GZip        | Deprecated; trusted data only |
| `RedisBinarySerializers.LZ4Jdk`     | JDK                  | LZ4         | Deprecated; trusted data only |
| `RedisBinarySerializers.SnappyJdk`  | JDK                  | Snappy      | Deprecated; trusted data only |
| `RedisBinarySerializers.ZstdJdk`    | JDK                  | Zstd        | Deprecated; trusted data only |
| `RedisBinarySerializers.GzipKryo`   | Kryo                 | GZip        | Recommended |
| `RedisBinarySerializers.LZ4Kryo`    | Kryo                 | LZ4         | Recommended |
| `RedisBinarySerializers.SnappyKryo` | Kryo                 | Snappy      | Recommended |
| `RedisBinarySerializers.ZstdKryo`   | Kryo                 | Zstd        | Recommended |
| `RedisBinarySerializers.GzipFory`   | Fory                 | GZip        | Recommended |
| `RedisBinarySerializers.LZ4Fory`    | Fory                 | LZ4         | Recommended |
| `RedisBinarySerializers.SnappyFory` | Fory                 | Snappy      | Recommended |
| `RedisBinarySerializers.ZstdFory`   | Fory                 | Zstd        | Recommended |

### Compression-only (ByteArray → ByteArray)

| Constant                        | Compression Algorithm |
|---------------------------------|-----------------------|
| `RedisBinarySerializers.LZ4`    | LZ4                   |
| `RedisBinarySerializers.Zstd`   | Zstd                  |
| `RedisBinarySerializers.Snappy` | Snappy                |
| `RedisBinarySerializers.Gzip`   | GZip                  |

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot-redis:test
```
