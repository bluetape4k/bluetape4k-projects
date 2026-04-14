# bluetape4k-spring-boot4-redis

English | [한국어](./README.ko.md)

A module that replaces Spring Data Redis's serialization layer with high-performance binary serialization and compression combinations (Spring Boot 4.x).

Provides a convenient way to configure `Serializer` and `RedisSerializationContext` when setting up `RedisTemplate` /
`ReactiveRedisTemplate`.

> Provides the same functionality as the Spring Boot 3 module (
`bluetape4k-spring-boot3-redis`), adapted to the Spring Boot 4.x API.

## Key Features

| Class / Function                   | Description                                                                                  |
|------------------------------------|----------------------------------------------------------------------------------------------|
| `RedisBinarySerializer`            | `RedisSerializer<Any>` implementation backed by `BinarySerializer`                           |
| `RedisCompressSerializer`          | Compression-only `RedisSerializer<ByteArray>` backed by `Compressor`                         |
| `RedisBinarySerializers`           | Singleton factory combining serializers (Jdk/Kryo/Fory) × compressors (GZip/LZ4/Snappy/Zstd) |
| `redisSerializationContext {}`     | DSL-based `RedisSerializationContext` builder                                                |
| `redisSerializationContextOf(...)` | Convenience function to specify key/value serializers directly                               |

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-redis:${bluetape4kVersion}")
}
```

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

| Constant                            | Serialization Engine | Compression |
|-------------------------------------|----------------------|-------------|
| `RedisBinarySerializers.Jdk`        | JDK                  | None        |
| `RedisBinarySerializers.Kryo`       | Kryo                 | None        |
| `RedisBinarySerializers.Fory`       | Fory                 | None        |
| `RedisBinarySerializers.LZ4Fory`    | Fory                 | LZ4         |
| `RedisBinarySerializers.LZ4Kryo`    | Kryo                 | LZ4         |
| `RedisBinarySerializers.ZstdFory`   | Fory                 | Zstd        |
| `RedisBinarySerializers.SnappyFory` | Fory                 | Snappy      |
| `RedisBinarySerializers.GzipFory`   | Fory                 | GZip        |

### Compression-only (ByteArray → ByteArray)

| Constant                        | Compression Algorithm |
|---------------------------------|-----------------------|
| `RedisBinarySerializers.LZ4`    | LZ4                   |
| `RedisBinarySerializers.Zstd`   | Zstd                  |
| `RedisBinarySerializers.Snappy` | Snappy                |
| `RedisBinarySerializers.Gzip`   | GZip                  |

## Architecture Diagrams

### Redis Serializer Class Hierarchy

```mermaid
classDiagram
    class RedisSerializer {
        <<interface>>
        +serialize(T): ByteArray
        +deserialize(ByteArray): T
    }
    class RedisBinarySerializer {
        -serializer: BinarySerializer
        +serialize(Any): ByteArray
        +deserialize(ByteArray): Any
    }
    class RedisCompressSerializer {
        -compressor: Compressor
        +serialize(ByteArray): ByteArray
        +deserialize(ByteArray): ByteArray
    }
    class RedisBinarySerializers {
        <<object>>
        +Jdk: RedisBinarySerializer
        +Kryo: RedisBinarySerializer
        +Fory: RedisBinarySerializer
        +LZ4Fory: RedisBinarySerializer
        +LZ4Kryo: RedisBinarySerializer
        +ZstdFory: RedisBinarySerializer
        +LZ4: RedisCompressSerializer
        +Zstd: RedisCompressSerializer
    }

    RedisSerializer <|.. RedisBinarySerializer
    RedisSerializer <|.. RedisCompressSerializer
    RedisBinarySerializers --> RedisBinarySerializer : creates
    RedisBinarySerializers --> RedisCompressSerializer : creates

    style RedisSerializer fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style RedisBinarySerializer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style RedisCompressSerializer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style RedisBinarySerializers fill:#E65100,stroke:#BF360C,color:#FFFFFF
```

### ReactiveRedisTemplate Serialization Flow

```mermaid
flowchart LR
    App["Application"] --> Template["ReactiveRedisTemplate<br/>(String, Any)"]
    Template --> Context["RedisSerializationContext<br/>redisSerializationContext { }"]
    Context --> KeySer["Key Serializer<br/>RedisSerializer.string()"]
    Context --> ValSer["Value Serializer<br/>RedisBinarySerializers.LZ4Fory"]
    ValSer --> Fory["Fory Serialization"]
    ValSer --> LZ4["LZ4 Compression"]
    LZ4 --> Redis[("Redis")]
    KeySer --> Redis

    classDef appStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef templateStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef serdeStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef compressStyle fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    classDef dbStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class App appStyle
    class Template,Context templateStyle
    class KeySer,ValSer,Fory serdeStyle
    class LZ4 compressStyle
    class Redis dbStyle
```

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot4-redis:test
```
