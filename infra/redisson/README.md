# bluetape4k-redisson

Redisson Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. 고성능 Codec, Coroutines 어댑터, Memorizer, NearCache, Leader Election 기능을 제공합니다.

## 주요 기능

| 기능                              | 설명                                                               |
|---------------------------------|------------------------------------------------------------------|
| `RedissonClientSupport`         | DSL 기반 `RedissonClient` / `RedissonReactiveClient` 팩토리           |
| `RedissonCodecs`                | 직렬화(Kryo5/Fory/Jdk/Protobuf) × 압축(GZip/LZ4/Snappy/Zstd) Codec 목록 (`bluetape4k-protobuf` 필요) |
| `RFutureSupport`                | `RFuture` → `awaitSuspending()` Coroutines 어댑터                   |
| `RedissonMemorizer`             | Redis `RMap` 기반 함수 결과 메모이제이션 (sync)                              |
| `AsyncRedissonMemorizer`        | `RFuture` 기반 비동기 메모이제이션                                          |
| `RedissonSuspendMemorizer`      | suspend 함수 기반 메모이제이션                                             |
| `RedissonNearCache`             | `RLocalCachedMap` 기반 2-tier Near Cache                           |
| `RedissonLeaderElection`        | Redisson `RLock` 기반 단일 리더 선출                                     |
| `RedissonLeaderGroupElection`   | 그룹 리더 선출                                                         |
| `RedissonSuspendLeaderElection` | Coroutines 기반 리더 선출                                              |

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.bluetape4k:bluetape4k-redisson:$bluetape4kVersion")
}
```

## 사용 예시

### RedissonClient 생성

```kotlin
import io.bluetape4k.redis.redisson.redissonClient

val client = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
}

// YAML 설정 파일로 생성
val config = configFromYamlOf(File("redisson.yaml"))
val client2 = redissonClientOf(config)
```

### 고성능 Codec 사용

```kotlin
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.redissonClient

val client = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
    // LZ4 + Fory 조합 (기본값, 가장 빠름)
    codec = RedissonCodecs.LZ4Fory
}

data class User(val id: Long, val name: String)

val map = client.getMap<String, User>("users")
map["user:1"] = User(1L, "Alice")
val user = map["user:1"]
```

### Coroutines 사용 (`awaitSuspending`)

```kotlin
import io.bluetape4k.redis.redisson.coroutines.awaitSuspending

val bucket = client.getBucket<String>("key")

// RFuture → suspend 변환
val value = bucket.getAsync().awaitSuspending()
bucket.setAsync("new-value").awaitSuspending()
```

### Memorizer — 함수 결과 Redis 캐싱

```kotlin
import io.bluetape4k.redis.redisson.memorizer.memorizer

val map = client.getMap<Int, Int>("squares")

// RMap 확장 함수로 생성
val memorizer = map.memorizer { key -> key * key }

val result1 = memorizer(5)  // 25 — 계산 후 저장
val result2 = memorizer(5)  // 25 — Redis에서 조회

// suspend 버전
val suspendMemorizer = map.suspendMemorizer { key -> computeExpensive(key) }
val result = suspendMemorizer(key)
```

### NearCache — 2-tier 로컬+분산 캐시

```kotlin
import io.bluetape4k.redis.redisson.nearcache.RedissonNearCache
import org.redisson.api.options.LocalCachedMapOptions

val options = LocalCachedMapOptions.name<String, User>("users-cache")
    .cacheSize(10_000)
    .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)

val nearCache = RedissonNearCache<String, User>(client, options)

nearCache["user:1"] = User(1L, "Alice")
val user = nearCache["user:1"]  // 로컬 캐시 우선, 없으면 Redis 조회
```

### Leader Election — 분산 리더 선출

```kotlin
import io.bluetape4k.redis.redisson.leader.RedissonLeaderElection

val election = RedissonLeaderElection(client, "my-leader-lock")

election.runIfLeader {
    // 리더일 때만 실행되는 로직
    println("나는 리더입니다!")
}

// Coroutines 버전
val suspendElection = RedissonSuspendLeaderElection(client, "my-leader-lock")
suspendElection.runIfLeader {
    // suspend 함수 호출 가능
}
```

## Codec 목록

| 상수                                | 직렬화                | 압축     |
|-----------------------------------|--------------------|--------|
| `RedissonCodecs.Fory` *(기본값)*     | Fory               | 없음     |
| `RedissonCodecs.Kryo5`            | Kryo5              | 없음     |
| `RedissonCodecs.Protobuf`         | Protobuf           | 없음     |
| `RedissonCodecs.LZ4Fory`          | Fory               | LZ4    |
| `RedissonCodecs.LZ4Kryo5`         | Kryo5              | LZ4    |
| `RedissonCodecs.ZstdFory`         | Fory               | Zstd   |
| `RedissonCodecs.SnappyFory`       | Fory               | Snappy |
| `RedissonCodecs.GzipFory`         | Fory               | GZip   |
| `RedissonCodecs.LZ4ForyComposite` | Fory (String 키 분리) | LZ4    |

## 빌드 및 테스트

테스트 실행 시 Redis 서버가 필요합니다. [Testcontainers](../testing/testcontainers)를 통해 자동 구성됩니다.

```bash
./gradlew :bluetape4k-redisson:test
```
