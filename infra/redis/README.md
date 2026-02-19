# Module bluetape4k-redis

Redis를 사용할 때 도움이 되는 기능들을 모아놓은 모듈입니다.

[Lettuce](https://lettuce.io/)와 [Redisson](https://redisson.org/) 두 가지 Redis 클라이언트를 지원하며,
Kotlin Coroutines 환경에서의 사용을 위한 확장 함수와 유틸리티를 제공합니다.

## 특징

- **Lettuce 지원**: 비동기 및 리액티브 Redis 클라이언트
- **Redisson 지원**: 분산 락, 캐시, 리더 선출 등 고급 기능
- **Coroutines 통합**: `suspend` 함수 및 Flow 지원
- **코덱 지원**: LZ4, GZip, Zstd, Fory, Protobuf 등 다양한 압축/직렬화 코덱
- **Spring 통합**: Spring Data Redis Serializer 지원

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-redis:${bluetape4kVersion}")
}
```

## 주요 기능

### 1. Lettuce 클라이언트

[Lettuce](https://lettuce.io/)는 Netty 기반의 비동기 Redis 클라이언트입니다.

```kotlin
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.RedisClient

// RedisClient 생성
val client: RedisClient = LettuceClients.clientOf("redis://localhost:6379")

// 또는 호스트/포트 지정
val client2 = LettuceClients.clientOf("localhost", 6379, timeoutInMillis = 5000)

// Sync 명령
val commands = LettuceClients.commands(client)
commands.set("key", "value")
val value = commands.get("key")

// Async 명령
val asyncCommands = LettuceClients.asyncCommands(client)
val future = asyncCommands.get("key")

// Coroutines 명령
val coroutinesCommands = LettuceClients.coroutinesCommands(client)
val result = coroutinesCommands.get("key")
```

### 2. Redisson 클라이언트

[Redisson](https://redisson.org/)은 Redis 기반의 In-Memory Data Grid입니다.

```kotlin
import io.bluetape4k.redis.redisson.*
import org.redisson.api.RedissonClient

// Config 생성
val config = redissonClient {
    useSingleServer().apply {
        address = "redis://localhost:6379"
        connectionMinimumIdleSize = 5
        connectionPoolSize = 10
    }
}

// RedissonClient 생성
val redisson: RedissonClient = redissonClientOf(config)

// Reactive 클라이언트
val reactiveClient = redissonReactiveClientOf(config)

// YAML 설정 로드
val configFromYaml = configFromYamlOf(File("redisson.yaml"))
val redisson2 = redissonClientOf(configFromYaml)
```

### 3. Redisson Coroutines 지원

```kotlin
import io.bluetape4k.redis.redisson.coroutines.*
import kotlinx.coroutines.delay

// Batch 작업
val result = redisson.withSuspendedBatch {
    val map = getMap<String, String>("my-map")
    map.putAsync("key1", "value1")
    map.putAsync("key2", "value2")
}

// Transaction
redisson.withSuspendedTransaction {
    val map = getMap<String, String>("my-map")
    map.put("key", "value")
    // commit 시 자동으로 적용
}

// Coroutines 환경에서 Lock 사용
val lockId = redisson.getLockId("my-lock")
val lock = redisson.getLock("my-lock:$lockId")
lock.lock()
try {
    // 임계 영역
    delay(1000)
} finally {
    lock.unlock(lockId)
}
```

### 4. Redisson Codec (압축/직렬화)

```kotlin
import io.bluetape4k.redis.redisson.*
import io.bluetape4k.redis.redisson.codec.*

// LZ4 압축 코덱
val lz4Codec = lz4Codec()

// GZip 압축 코덱
val gzipCodec = gzipCodec()

// Zstd 압축 코덱
val zstdCodec = zstdCodec()

// Apache Fory 직렬화 코덱
val foryCodec = foryCodec()

// Protobuf 코덱
val protobufCodec = protobufCodec()

// Config에 코덱 적용
val config = configFromYamlOf(inputStream, codec = lz4Codec)
val redisson = redissonClientOf(config)
```

### 5. Redisson 캐시 지원

```kotlin
import io.bluetape4k.redis.redisson.cache.*

// Local Cache Map
val localCacheMap = redisson.getLocalCachedMap<String, String>(
    "local-cache",
    localCachedMapOptions {
        evictionPolicy(EvictionPolicy.LRU)
        cacheSize(1000)
        timeToLive(10, TimeUnit.MINUTES)
    }
)

// Map Cache
val mapCache = redisson.getMapCache<String, String>("map-cache")
mapCache.put("key", "value", 10, TimeUnit.MINUTES)

// Near Cache
val nearCache = RedissonNearCache(redisson, "near-cache") {
    maxSize(1000)
    timeToLive(5, TimeUnit.MINUTES)
}
```

### 6. Redisson 리더 선출 (Leader Election)

```kotlin
import io.bluetape4k.redis.redisson.leader.*
import io.bluetape4k.redis.redisson.leader.coroutines.*

// 리더 선출
val leaderElection = RedissonLeaderElection(redisson, "leader-lock") {
    watchDogTimeout(30, TimeUnit.SECONDS)
}

// 리더가 되어 작업 수행
leaderElection.runIfLeader {
    println("I'm the leader!")
    // 리더만 수행하는 작업
}

// Coroutines 환경
val suspendLeaderElection = RedissonSuspendLeaderElection(redisson, "leader-lock")
suspendLeaderElection.runIfLeader {
    println("I'm the leader in coroutines!")
}
```

### 7. Redisson Memorizer (캐시 래퍼)

```kotlin
import io.bluetape4k.redis.redisson.memorizer.*

// 결과를 Redis에 캐싱하는 함수 래퍼
val memorizer = RedissonMemorizer(redisson, "cache-map", ttlMinutes = 10)

val result = memorizer.memoize("cache-key") {
    // 비용이 큰 작업
    expensiveOperation()
}

// 비동기
val asyncMemorizer = AsyncRedissonMemorizer(redisson, "async-cache")
val future = asyncMemorizer.memoize("key") {
    asyncOperation()
}

// Coroutines
val suspendMemorizer = RedissonSuspendMemorizer(redisson, "suspend-cache")
val result = suspendMemorizer.memoize("key") {
    suspendOperation()
}
```

### 8. Spring Data Redis Serializer

```kotlin
import io.bluetape4k.redis.spring.serializer.*

// 바이너리 Serializer
val binarySerializer = redisBinarySerializer<String>()

// 압축 Serializer (LZ4)
val compressSerializer = redisCompressSerializer<String>(CompressionAlgorithm.LZ4)

// RedisSerializationContext
val context = redisSerializationContext<String, String> {
    key(StringRedisSerializer())
    value(binarySerializer)
    hashKey(StringRedisSerializer())
    hashValue(compressSerializer)
}
```

### 9. Redis Streams (Redisson)

```kotlin
import io.bluetape4k.redis.redisson.*

// Stream 생성
val stream = redisson.getStream<String>("my-stream")

// 메시지 추가
val id = stream.add("field1", "value1")

// 메시지 읽기
val messages = stream.read(10)

// Consumer Group
val group = stream.createGroup("my-group")
val pending = stream.readGroup("my-group", "consumer1", 10)
```

## 테스트 지원

```kotlin
import io.bluetape4k.redis.AbstractRedisTest
import io.bluetape4k.redis.redisson.AbstractRedissonTest

class MyRedisTest: AbstractRedisTest() {
    @Test
    fun `Redis 연결 테스트`() {
        val client = LettuceClients.clientOf(redisHost, redisPort)
        val commands = LettuceClients.commands(client)
        commands.set("test", "value")
        commands.get("test") shouldBeEqualTo "value"
    }
}

class MyRedissonTest: AbstractRedissonTest() {
    @Test
    fun `Redisson Map 테스트`() {
        val map = redisson.getMap<String, String>("test-map")
        map.put("key", "value")
        map["key"] shouldBeEqualTo "value"
    }
}
```

## 예제

더 많은 예제는 `src/test/kotlin/io/bluetape4k/redis` 패키지에서 확인할 수 있습니다:

- `lettuce/`: Lettuce 클라이언트 예제
- `redisson/`: Redisson 클라이언트 예제
- `redisson/cache/`: 캐시 관련 예제
- `redisson/leader/`: 리더 선출 예제
- `redisson/memorizer/`: Memorizer 예제

## 참고 자료

- [Redis 공식 문서](https://redis.io/documentation)
- [Lettuce 문서](https://lettuce.io/core/release/reference/)
- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Redisson Configuration](https://github.com/redisson/redisson/wiki/2.-Configuration)

## 라이선스

Apache License 2.0
