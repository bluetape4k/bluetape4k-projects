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
    implementation("io.github.bluetape4k:bluetape4k-redis:${bluetape4kVersion}")
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

#### 단일 리더 선출 — 동시에 1개만 실행

```kotlin
import io.bluetape4k.redis.redisson.leader.*
import io.bluetape4k.redis.redisson.leader.coroutines.*

val options = RedissonLeaderElectionOptions(
    waitTime = Duration.ofSeconds(5),
    leaseTime = Duration.ofSeconds(60),
)

// 동기 방식
val leaderElection = RedissonLeaderElection(redissonClient, options)
val result = leaderElection.runIfLeader("batch-job") {
    println("I'm the leader! Running batch job...")
    processBatch()
}

// 코루틴 방식
val suspendElection = RedissonSuspendLeaderElection(redissonClient, options)
val result = suspendElection.runIfLeader("batch-job") {
    println("I'm the leader in coroutines!")
    processBatchSuspend()
}

// 확장 함수 방식
val result = redissonClient.runIfLeader("batch-job", options) {
    processBatch()
}
```

#### 복수 리더 선출 (LeaderGroupElection) — 동시에 N개까지 실행

`maxLeaders`개의 슬롯을 제공하며, 슬롯이 가득 찬 경우 `waitTime` 내에 슬롯을 획득하지 못하면
`RedisException`을 던집니다. 분산 환경에서 배치 작업의 동시 처리 수를 제어할 때 유용합니다.

```kotlin
import io.bluetape4k.redis.redisson.leader.*
import io.bluetape4k.redis.redisson.leader.coroutines.*

val options = RedissonLeaderElectionOptions(
    waitTime = Duration.ofSeconds(10),
    leaseTime = Duration.ofSeconds(60),
)

// 동기 방식 — 최대 3개 스레드/프로세스 동시 실행
val groupElection = RedissonLeaderGroupElection(redissonClient, maxLeaders = 3, options)

val result = groupElection.runIfLeader("batch-job") {
    processChunk()  // 슬롯 획득 → 작업 실행 → 슬롯 자동 반납
}

// 상태 조회
val state = groupElection.state("batch-job")
println("활성 리더: ${state.activeCount} / ${state.maxLeaders}")
println("남은 슬롯: ${state.availableSlots}, 가득 참: ${state.isFull}")

// 코루틴 방식 — 최대 3개 코루틴/프로세스 동시 실행
val suspendGroupElection = RedissonSuspendLeaderGroupElection(redissonClient, maxLeaders = 3, options)

val result = suspendGroupElection.runIfLeader("batch-job") {
    processChunkSuspend()
}

// 확장 함수 방식
val result = redissonClient.runIfLeaderGroup("batch-job", maxLeaders = 3, options) {
    processChunk()
}

// suspend 확장 함수 방식
val result = redissonClient.runSuspendIfLeaderGroup("batch-job", maxLeaders = 3, options) {
    processChunkSuspend()
}
```

#### LeaderGroupState — 상태 정보

```kotlin
data class LeaderGroupState(
    val lockName: String,       // 락 이름
    val maxLeaders: Int,        // 최대 동시 리더 수
    val activeCount: Int,       // 현재 활성 리더 수
) {
    val availableSlots: Int     // 남은 슬롯 수
    val isFull: Boolean         // 슬롯이 가득 찬지 여부
    val isEmpty: Boolean        // 활성 리더가 없는지 여부
}
```

### 7. Redisson Memorizer (캐시 래퍼)

```kotlin
import io.bluetape4k.redis.redisson.memorizer.*
import org.redisson.client.codec.IntegerCodec

val map = redisson.getMap<Int, Int>("cache-map", IntegerCodec())

// 결과를 Redis에 캐싱하는 함수 래퍼
val memorizer = map.memorizer { key ->
    expensiveOperation(key)
}
val result = memorizer(10)

// 비동기
val asyncMemorizer = map.asyncMemorizer { key ->
    asyncOperation(key)
}
val future = asyncMemorizer(10)

// Coroutines
val suspendMemorizer = map.suspendMemorizer { key ->
    suspendOperation(key)
}
val suspendResult = suspendMemorizer(10)
```

`AsyncRedissonMemorizer`는 동일 키에 대한 동시 호출을 하나의 in-flight 계산으로 합쳐 중복 계산을 줄입니다.
또한 `clear()`는 Redis 엔트리와 로컬 in-flight 상태를 함께 비워 다음 호출이 새 계산으로 시작되도록 보장합니다.

### 8. RedisCacheConfig 사용 시 주의사항

```kotlin
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.redisson.api.map.WriteMode

val config = RedisCacheConfig(
    writeMode = WriteMode.WRITE_BEHIND,
    nearCacheEnabled = true,
)

val options = config.toLocalCachedMapOptions<String, String>("users")
```

- `codec`, write-through/write-behind, retry, near-cache TTL/max-idle/sync-strategy는 옵션 객체에 반영됩니다.
- `ttl`, `maxSize`, `deleteFromDBOnInvalidate`는 Redisson의 `MapOptions`/`LocalCachedMapOptions`가 직접 지원하지 않으므로 변환 시 fail-fast 예외를 발생시킵니다.
- 전역 TTL이 필요하면 per-entry expiration 또는 별도의 map cache API를 사용하세요.

### 9. Spring Data Redis Serializer

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

### 10. Redis Streams (Redisson)

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
