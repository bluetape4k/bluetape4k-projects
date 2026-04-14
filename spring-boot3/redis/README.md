# bluetape4k-spring-boot3-redis

English | [한국어](./README.ko.md)

A module that replaces the serialization layer of Spring Data Redis with high-performance binary serialization and compression combinations. It makes it easy to configure
`Serializer` and `RedisSerializationContext` when setting up `RedisTemplate` or `ReactiveRedisTemplate`.

## Key Features

| Class / Function                   | Description                                                                                    |
|------------------------------------|------------------------------------------------------------------------------------------------|
| `RedisBinarySerializer`            | `RedisSerializer<Any>` implementation backed by `BinarySerializer`                             |
| `RedisCompressSerializer`          | Compression-only `RedisSerializer<ByteArray>` backed by `Compressor`                           |
| `RedisBinarySerializers`           | Singleton factory combining serialization (Jdk/Kryo/Fory) × compression (GZip/LZ4/Snappy/Zstd) |
| `redisSerializationContext {}`     | DSL-based `RedisSerializationContext` builder                                                  |
| `redisSerializationContextOf(...)` | Convenience function for specifying key/value serializers directly                             |

## Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot3-redis:$bluetape4kVersion")
}
```

## Usage Examples

### ReactiveRedisTemplate Configuration (DSL Style)

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

### ReactiveRedisTemplate Configuration (Convenience Function Style)

```kotlin
@Bean
fun reactiveRedisTemplate(
    factory: ReactiveRedisConnectionFactory,
): ReactiveRedisTemplate<String, ByteArray> {
    // String key + LZ4 Kryo value serialization
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

### Compression-Only Serializer

```kotlin
// When the value is already a ByteArray, apply compression only
val context = redisSerializationContext<String, ByteArray> {
    key(RedisSerializer.string())
    value(RedisBinarySerializers.LZ4)   // ByteArray → LZ4 compression
}
```

## Serializer Reference

### Serialization (Object → ByteArray)

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

### Compression-Only (ByteArray → ByteArray)

| Constant                        | Algorithm |
|---------------------------------|-----------|
| `RedisBinarySerializers.LZ4`    | LZ4       |
| `RedisBinarySerializers.Zstd`   | Zstd      |
| `RedisBinarySerializers.Snappy` | Snappy    |
| `RedisBinarySerializers.Gzip`   | GZip      |

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

    style RedisSerializer fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style RedisBinarySerializer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style RedisCompressSerializer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style RedisBinarySerializers fill:#FFF3E0,stroke:#FFCC80,color:#E65100
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

    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef springStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class App serviceStyle
    class Template springStyle
    class Context springStyle
    class KeySer utilStyle
    class ValSer utilStyle
    class Fory extStyle
    class LZ4 extStyle
    class Redis dataStyle
```

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot3-redis:test
```
