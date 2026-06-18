# bluetape4k-redis

English | [한국어](./README.ko.md)

An **umbrella module** that bundles both the Lettuce and Redisson Redis clients. Existing code depending on
`bluetape4k-redis` continues to work without modification.

## Module Structure

```
infra/redis (umbrella)
├── infra/lettuce      — Lettuce client, high-performance codecs, RedisFuture → Coroutines adapter
└── infra/redisson     — Redisson client, codecs, cache helpers, NearCache
```

For Spring Data Redis serialization, use the `spring-boot/redis` module separately.

## Dependency

### Full Bundle (umbrella)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-redis:$bluetape4kVersion")
}
```

### Selective Client Dependencies

```kotlin
dependencies {
    // Lettuce only
    implementation("io.github.bluetape4k:bluetape4k-lettuce:$bluetape4kVersion")

    // Redisson only
    implementation("io.github.bluetape4k:bluetape4k-redisson:$bluetape4kVersion")

    // Spring Boot Redis serializers
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-redis:$bluetape4kVersion")
}
```

## Submodule Details

### [bluetape4k-lettuce](../lettuce/README.md)

High-performance Redis client extension based on Lettuce.

- `LettuceClients` — `RedisClient` / `StatefulRedisConnection` factory with connection caching
- `LettuceBinaryCodecs` — Codec combinations: serializers (Jdk/Kryo/Fory) × compression (GZip/LZ4/Snappy/Zstd)
- `LettuceProtobufCodecs` — Protobuf-based codecs
- `RedisFuture.awaitSuspending()` — Converts `RedisFuture` to a suspend function

```kotlin
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.redis.lettuce.awaitSuspending

val client = LettuceClients.clientOf("redis://localhost:6379")

// Coroutine commands
val commands = LettuceClients.coroutinesCommands(client)
val value = commands.get("key")

// Store objects with high-performance codec
val codec = LettuceBinaryCodecs.lz4Fory<MyData>()
val typedCommands = LettuceClients.commands(client, codec)
typedCommands.set("data:1", MyData(id = 1))

// RedisFuture → suspend
val asyncResult = LettuceClients.asyncCommands(client).get("key").awaitSuspending()

LettuceClients.shutdown(client)
```

### [bluetape4k-redisson](../redisson/README.md)

Distributed Redis extension based on Redisson.

- `redissonClient {}` DSL — Creates a `RedissonClient`
- `RedissonCodecs` — Codec combinations: serializers (Kryo5/Fory/Jdk/Protobuf) × compression (GZip/LZ4/Snappy/Zstd)
- `Collection<RFuture>.awaitAll()` / `Iterable<RFuture>.sequence()` — Coroutine-friendly Redisson future adapters
- `RedissonNearCache` — 2-tier Near Cache based on `RLocalCachedMap`

```kotlin
import io.bluetape4k.redis.redisson.redissonClient
import io.bluetape4k.redis.redisson.codec.RedissonCodecs

// Create client
val client = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
    codec = RedissonCodecs.LZ4Fory
}

// Redisson distributed map
val map = client.getMap<String, String>("settings")
map.fastPut("theme", "dark")
```

## Module Dependency Structure

![Module Dependency Structure diagram](../../docs/images/readme-diagrams/infra-redis-diagram-01.png)

## Exported API Surface

![Exported API Surface diagram](../../docs/images/readme-diagrams/infra-redis-diagram-02.png)

## Spring Data Redis

The following separate module provides high-performance serializers for configuring
`RedisTemplate` / `ReactiveRedisTemplate`.

- [bluetape4k-spring-boot-redis](../../spring-boot/redis/README.md)

```kotlin
import io.bluetape4k.spring.redis.serializer.RedisBinarySerializers
import io.bluetape4k.spring.redis.serializer.redisSerializationContext

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
```

## Testing

```bash
# Run all redis module tests
./gradlew :bluetape4k-redis:test

# Run submodule tests individually
./gradlew :bluetape4k-lettuce:test
./gradlew :bluetape4k-redisson:test
```

Tests require a Redis server, which is automatically provisioned via [Testcontainers](../../testing/testcontainers).

## References

- [Lettuce Official Documentation](https://lettuce.io/core/release/reference/)
- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
