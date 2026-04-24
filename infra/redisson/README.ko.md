# bluetape4k-redisson

[English](./README.md) | 한국어

Redisson Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. DSL 방식의 클라이언트 생성, 고성능 Codec, Kotlin Coroutines 지원, 분산 리더 선출, NearCache 기능을 제공합니다.

## 주요 기능

| 기능                              | 설명                                                                              |
|---------------------------------|---------------------------------------------------------------------------------|
| `RedissonClientSupport`         | DSL 기반 `RedissonClient` / `RedissonReactiveClient` 팩토리, YAML 설정 로드              |
| `RedissonClientExtensions`      | `withBatch {}`, `withTransaction {}` DSL 확장 함수                                  |
| `RedissonClientCoroutine`       | `withSuspendedBatch {}`, `withSuspendedTransaction {}` suspend 확장 함수            |
| `RFutureSupport`                | `Collection<RFuture>.awaitAll()`, `Iterable<RFuture>.sequence()` Coroutines 어댑터 |
| `RedissonCodecs`                | 직렬화(Fory/Kryo5/Jackson3/Fastjson2) × 압축(LZ4/Zstd/Snappy/GZip) 조합 Codec 목록         |
| `RedissonLeaderElection`        | `RLock` 기반 단일 리더 선출 (동기 / 비동기)                                                  |
| `RedissonSuspendLeaderElection` | `RLock` 기반 단일 리더 선출 (Coroutines)                                                |
| `RedissonLeaderGroupElection`   | `RSemaphore` 기반 복수(N개) 동시 리더 선출                                                 |
| `RedissonNearCache`             | `RLocalCachedMap` 기반 2-tier Near Cache                                          |

`RedissonCacheConfig`/`RedissonNearCacheConfig` 사용 시:

- `maxSize`, `nearCacheMaxSize`, `writeBehindBatchSize`는 음수일 수 없고, 배치 크기는 0보다 커야 합니다.
- `timeToLive`, `maxIdle`, `nearCacheTtl`, `nearCacheMaxIdleTime`은 지정 시 음수일 수 없으며, near cache TTL/idle은 0보다 커야 합니다.

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-redisson:$bluetape4kVersion")

    // Codec 선택적 의존성 (사용하는 항목만 추가)
    runtimeOnly("org.apache.fury:fury-kotlin")        // Fory 직렬화
    runtimeOnly("com.esotericsoftware:kryo")           // Kryo5 직렬화
    runtimeOnly("org.lz4:lz4-java")                   // LZ4 압축
    runtimeOnly("com.github.luben:zstd-jni")          // Zstd 압축
    runtimeOnly("org.xerial.snappy:snappy-java")       // Snappy 압축
    runtimeOnly("org.apache.commons:commons-compress") // GZip 압축
}
```

## 사용 예시

### 1. RedissonClient 생성

#### DSL 방식

```kotlin
import io.bluetape4k.redis.redisson.redissonClient
import io.bluetape4k.redis.redisson.redissonReactiveClient

// 단일 서버
val client = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
}

// Reactive 클라이언트
val reactive = redissonReactiveClient {
    useSingleServer().address = "redis://localhost:6379"
}

client.shutdown()
```

#### YAML 설정 파일 방식

```kotlin
import io.bluetape4k.redis.redisson.configFromYamlOf
import io.bluetape4k.redis.redisson.redissonClientOf
import io.bluetape4k.redis.redisson.codec.RedissonCodecs

// InputStream, String, File, URL 모두 지원
val config = configFromYamlOf(
    input = File("redisson.yaml").inputStream(),
    codec = RedissonCodecs.Default,  // 선택적 Codec 지정 (기본: RedissonCodecs.Default)
)
val client = redissonClientOf(config)
```

`redisson.yaml` 예시:

```yaml
singleServerConfig:
  address: "redis://localhost:6379"
  connectionPoolSize: 64
  connectionMinimumIdleSize: 24
```

---

### 2. Codec

`io.bluetape4k.redis.redisson.codec` 패키지에서 고성능 Codec을 제공합니다.

| 상수                            | 직렬화                    | 압축   | 설명                          |
|---------------------------------|------------------------|------|-------------------------------|
| `RedissonCodecs.Default`        | Fory (fallback: Kryo5) | LZ4  | 기본값. 빠른 속도와 압축 균형             |
| `RedissonCodecs.Fory`           | Fory                   | 없음   | Fory 직렬화만 사용                   |
| `RedissonCodecs.Kryo5`          | Kryo5                  | 없음   | Kryo5 직렬화만 사용                  |
| `RedissonCodecs.LZ4`            | Default                | LZ4  | LZ4 압축 래핑                     |
| `RedissonCodecs.Zstd`           | Default                | Zstd | 높은 압축률                        |
| `RedissonCodecs.Jackson3`       | Jackson3 (JSON)        | 없음   | Jackson 3.x JSON Codec         |
| `RedissonCodecs.Fastjson2`      | Fastjson2 (JSONB)      | 없음   | Fastjson2 JSONB Codec          |

```kotlin
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.redis.redisson.codec.ForyCodec
import io.bluetape4k.redis.redisson.codec.Jackson3Codec
import io.bluetape4k.redis.redisson.codec.Fastjson2Codec
import io.bluetape4k.redis.redisson.codec.Lz4Codec

val client = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
    codec = RedissonCodecs.Default   // Fory + LZ4 조합
}

// JSON 기반 Codec — 사람이 읽을 수 있는 저장 형식
val jsonClient = redissonClient {
    useSingleServer().address = "redis://localhost:6379"
    codec = RedissonCodecs.Jackson3   // Jackson 3.x JSON
}

// Fastjson2 JSONB — 패키지 기반 보안 제한
val secureCodec = Fastjson2Codec(
    allowedPackagePrefixes = setOf("com.example.", "io.bluetape4k.")
)

// 직접 조합도 가능
val customCodec = Lz4Codec(innerCodec = ForyCodec())
```

Codec 클래스:

- `ForyCodec` — Apache Fory 직렬화. 직렬화 실패 시 fallback Codec(Kryo5)으로 자동 전환
- `Jackson3Codec` — Jackson 3.x JSON 직렬화. 사람이 읽을 수 있는 JSON 텍스트로 저장
- `Fastjson2Codec` — Fastjson2 JSONB 바이너리 포맷. 클래스 이름 헤더 + JSONB 바이트로 저장. `allowedPackagePrefixes`로 pre-instantiation 보안 검증 지원
- `Lz4Codec` — LZ4 압축 래퍼. `innerCodec`으로 감쌈
- `ZstdCodec` — Zstd 압축 래퍼
- `GzipCodec` — GZip 압축 래퍼

---

### 3. Batch / Transaction

#### Batch — 네트워크 왕복 최소화

```kotlin
import io.bluetape4k.redis.redisson.withBatch

val result = client.withBatch {
    getBucket<String>("key1").setAsync("value1")
    getBucket<String>("key2").setAsync("value2")
    getAtomicLong("counter").incrementAndGetAsync()
}
```

#### Transaction — 원자적 실행

```kotlin
import io.bluetape4k.redis.redisson.withTransaction

client.withTransaction {
    getBucket<String>("account:balance").set("1000")
    getMap<String, Int>("ledger").put("tx-001", 500)
    // 블록 정상 종료 시 자동 commit, 예외 발생 시 자동 rollback
}
```

> **주의**: Coroutine 환경에서는 스레드 전환으로 트랜잭션이 깨질 수 있습니다. 아래 Coroutine 버전을 사용하세요.

---

### 4. Coroutine 지원

#### withSuspendedBatch / withSuspendedTransaction

```kotlin
import io.bluetape4k.redis.redisson.coroutines.withSuspendedBatch
import io.bluetape4k.redis.redisson.coroutines.withSuspendedTransaction

// suspend Batch
val result = client.withSuspendedBatch {
    getBucket<String>("key1").setAsync("value1")
    getAtomicLong("counter").incrementAndGetAsync()
}

// suspend Transaction
client.withSuspendedTransaction {
    getBucket<String>("key").set("value")
    // 정상 종료 시 commitAsync().await(), 예외 시 rollbackAsync().await()
}
```

#### RFuture Coroutine 변환

```kotlin
import io.bluetape4k.redis.redisson.coroutines.awaitAll
import io.bluetape4k.redis.redisson.coroutines.sequence

// 여러 RFuture를 suspend로 일괄 대기
val rfutures: List<RFuture<String>> = ids.map { rmap.getAsync(it) }
val results: List<String> = rfutures.awaitAll()   // suspend

// CompletableFuture로 변환 후 일괄 처리 (blocking)
val future: CompletableFuture<List<String>> = rfutures.sequence()
val values: List<String> = future.get()
```

---

### 5. Leader Election — 분산 리더 선출

#### 동기 버전

`RLock`을 기반으로 분산 환경에서 단 하나의 프로세스/스레드만 작업을 수행하도록 리더를 선출합니다.

```kotlin
import io.bluetape4k.redis.redisson.leader.RedissonLeaderElection
import io.bluetape4k.leader.LeaderElectionOptions
import java.time.Duration

val options = LeaderElectionOptions(
    waitTime = Duration.ofSeconds(5),
    leaseTime = Duration.ofSeconds(30),
)
val election = RedissonLeaderElection(client, options)

val result = election.runIfLeader("batch-job") {
    // 리더로 선출된 프로세스만 실행
    processBatch()
}

// RedissonClient 확장 함수로도 사용 가능
val result2 = client.runIfLeader("batch-job") {
    processBatch()
}

// 비동기 (CompletableFuture)
val future = client.runAsyncIfLeader("batch-job") {
    CompletableFuture.supplyAsync { processBatch() }
}
```

#### Coroutine 버전

```kotlin
import io.bluetape4k.redis.redisson.leader.RedissonSuspendLeaderElection

val election = RedissonSuspendLeaderElection(client, options)

val result = election.runIfLeader("batch-job") {
    delay(100)
    processData()
}

// RedissonClient 확장 함수로도 사용 가능
val result2 = client.suspendRunIfLeader("batch-job") {
    processData()
}
```

> **코루틴 Lock ID**: Redisson Lock은 스레드 ID 기반입니다. 코루틴 환경에서는 스레드가 전환되면 락이 깨질 수 있으므로, `RedissonSuspendLeaderElection`은
`RAtomicLong`으로 코루틴 세션마다 고유 ID를 발급하여 이 문제를 해결합니다.

#### 그룹 리더 선출 — 최대 N개 동시 실행

`RSemaphore` 기반으로 최대 N개 프로세스가 동시에 작업을 수행합니다.

```kotlin
import io.bluetape4k.redis.redisson.leader.RedissonLeaderGroupElection
import io.bluetape4k.leader.LeaderGroupElectionOptions

val options = LeaderGroupElectionOptions(
    maxLeaders = 3,                       // 최대 3개 동시 실행
    waitTime = Duration.ofSeconds(5),
)
val groupElection = RedissonLeaderGroupElection(client, options)

// 최대 3개 프로세스/스레드가 동시에 실행
val result = groupElection.runIfLeader("parallel-job") {
    processChunk()
}

// 상태 조회
val state = groupElection.state("parallel-job")
println("active=${state.activeCount}, available=${state.availableSlots}")

// 비동기 실행
val future = groupElection.runAsyncIfLeader("parallel-job") {
    CompletableFuture.supplyAsync { processChunk() }
}
```

---

### 6. NearCache

Redisson `RLocalCachedMap` 기반 2-tier Near Cache입니다. 로컬 캐시 우선 조회 후 없으면 Redis에서 조회합니다.

```kotlin
import io.bluetape4k.redis.redisson.nearcache.RedissonNearCache
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig

val config = RedisCacheConfig()
val nearCache = RedissonNearCache<String, Any>("my-cache", client, config)

nearCache.put("key", "value")
val value = nearCache.get("key")   // 로컬 캐시에서 우선 조회
```

> JCache 기반의 고급 NearCache (RESP3 하이브리드, Resilient write-behind 등)는 `bluetape4k-cache-redisson` 모듈을 사용하세요.

---

## 아키텍처 다이어그램

### Codec 계층 구조

```mermaid
classDiagram
    class Codec {
        <<interface>>
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class ForyCodec {
        -fallbackCodec: Codec
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class Kryo5Codec {
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class Lz4Codec {
        -innerCodec: Codec
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class ZstdCodec {
        -innerCodec: Codec
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class GzipCodec {
        -innerCodec: Codec
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class Jackson3Codec {
        -objectMapper: ObjectMapper
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class Fastjson2Codec {
        -fallbackCodec: Codec
        -allowedPackagePrefixes: Set~String~
        +getValueEncoder() Encoder
        +getValueDecoder() Decoder
    }

    class RedissonCodecs {
        <<object>>
        +Default: Lz4Codec
        +Fory: ForyCodec
        +Kryo5: Kryo5Codec
        +LZ4: Lz4Codec
        +Zstd: ZstdCodec
        +Jackson3: Jackson3Codec
        +Fastjson2: Fastjson2Codec
    }

    Codec <|.. ForyCodec
    Codec <|.. Kryo5Codec
    Codec <|.. Lz4Codec
    Codec <|.. ZstdCodec
    Codec <|.. GzipCodec
    Codec <|.. Jackson3Codec
    Codec <|.. Fastjson2Codec
    Lz4Codec --> ForyCodec : innerCodec
    ZstdCodec --> ForyCodec : innerCodec
    ForyCodec --> Kryo5Codec : fallback
    Fastjson2Codec --> ForyCodec : fallback
    RedissonCodecs --> Lz4Codec
    RedissonCodecs --> ForyCodec
    RedissonCodecs --> Kryo5Codec
    RedissonCodecs --> Jackson3Codec
    RedissonCodecs --> Fastjson2Codec

    style Codec fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style ForyCodec fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Kryo5Codec fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Lz4Codec fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style ZstdCodec fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style GzipCodec fill:#FCE4EC,stroke:#F48FB1,color:#AD1457
    style Jackson3Codec fill:#EDE7F6,stroke:#B39DDB,color:#4527A0
    style Fastjson2Codec fill:#EDE7F6,stroke:#B39DDB,color:#4527A0
    style RedissonCodecs fill:#FFF3E0,stroke:#FFCC80,color:#E65100

```

### 분산 리더 선출 시퀀스

```mermaid
sequenceDiagram
    participant P1 as 프로세스 1
    participant P2 as 프로세스 2
    participant Redis as Redis (RLock)
    participant Job as 배치 작업

    P1->>+Redis: tryLock("batch-job", waitTime=5s, leaseTime=30s)
    P2->>Redis: tryLock("batch-job", waitTime=5s, leaseTime=30s)
    Redis-->>P1: Lock 획득 성공
    Redis-->>P2: Lock 획득 실패 (대기 or 포기)

    P1->>+Job: runIfLeader { processBatch() }
    Note over P1,Job: 리더로 선출된 P1만 실행
    Job-->>-P1: 작업 완료
    P1->>Redis: unlock()
    Redis-->>-P1: Lock 해제

    P2->>Redis: 다음 라운드에서 재시도
```

### NearCache 2-Tier 캐시 흐름

```mermaid
sequenceDiagram
    participant App as 애플리케이션
    participant Local as 로컬 캐시<br/>(RLocalCachedMap)
    participant Redis as Redis<br/>(원격 저장소)
    participant Other as 다른 노드

    App->>+Local: get("key")
    alt 로컬 캐시 히트
        Local-->>App: 값 즉시 반환
    else 로컬 캐시 미스
        Local->>+Redis: GET "key"
        Redis-->>-Local: 값 반환
        Local->>Local: 로컬 캐시에 저장
        Local-->>-App: 값 반환
    end

    App->>Redis: put("key", newValue)
    Redis->>Local: Invalidation 전파
    Redis->>Other: Invalidation 전파 (Pub/Sub)
    Other->>Other: 로컬 캐시 무효화
```

### Batch / Transaction 처리 흐름

```mermaid
flowchart TD
    App[애플리케이션] -->|withBatch| Batch[RBatch]
    Batch -->|setAsync| Op1[bucket.setAsync]
    Batch -->|incrementAndGetAsync| Op2[atomicLong.incrementAsync]
    Batch -->|putAsync| Op3[map.putAsync]
    Op1 & Op2 & Op3 -->|execute| Redis[Redis<br/>파이프라인 실행]
    Redis -->|BatchResult| App

    App2[코루틴 환경] -->|withSuspendedTransaction| Tx[RTransaction]
    Tx -->|set/put 작업| TxOps[트랜잭션 연산]
    TxOps -->|성공 시 commitAsync| Redis
    TxOps -->|예외 시 rollbackAsync| Redis

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef asyncStyle fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef extStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000
    classDef cacheStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32

    style App fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style App2 fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    style Redis fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style Batch fill:#F57F17,stroke:#E65100,color:#000000
    style Tx fill:#F57F17,stroke:#E65100,color:#000000
    style Op1 fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style Op2 fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style Op3 fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style TxOps fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
```

## 고성능 Batch 패턴 — 메가배치

코루틴 환경에서 대량 Redis 쓰기/읽기 처리 시, **코루틴당 1개의 RBatch**를 생성하면 Redis 왕복(RTT)을 100배 이상 줄일 수 있습니다.

### 패턴 비교

| 방식 | RTT 수 (50 코루틴 × 100 ops) | 상대 처리량 |
|------|------------------------------|-----------|
| 개별 RMap op | 10,000 | 1× |
| op당 RBatch | 5,000 | ~2× |
| **코루틴당 1 RBatch (메가배치)** | **50** | **~8×** |

### 메가배치 구현 예시

```kotlin
import org.redisson.client.codec.StringCodec

// 키 풀 사전 생성 — per-op 문자열 보간 제거
private val KEY_POOL: Array<Array<String>> = Array(CONCURRENCY + 1) { cid ->
    Array(OPS_PER_COROUTINE) { opIdx -> "c$cid-op$opIdx" }
}

suspend fun processInMegaBatch(redisson: RedissonClient, mapName: String) {
    val jobs = (0 until CONCURRENCY).map { coroutineId ->
        async(Dispatchers.IO) {
            // 코루틴당 RBatch 1개 — StringCodec으로 Jackson 오버헤드 제거
            val batch = redisson.createBatch()
            val batchMap = batch.getMap<String, String>(mapName, StringCodec.INSTANCE)

            repeat(OPS_PER_COROUTINE) { opIdx ->
                val key = KEY_POOL[coroutineId][opIdx]   // 사전 계산된 키
                batchMap.fastPutAsync(key, "value-$coroutineId-$opIdx")
            }

            batch.execute()  // 100개 명령 → 1 RTT
        }
    }
    jobs.awaitAll()
}
```

### 핵심 최적화 포인트

| 최적화 | 효과 | 비고 |
|--------|------|------|
| `redisson.createBatch()` 코루틴당 1개 | RTT 100배 감소 | 가장 큰 개선 |
| `StringCodec.INSTANCE` | Jackson 직렬화 오버헤드 제거 | String 타입 Map에만 적용 |
| 사전 계산된 KEY_POOL | 문자열 보간 GC 압력 제거 | 반복 키 패턴에 유효 |

---

## 성능 벤치마크

`RedissonConcurrencyBenchmark` 기준 (50 코루틴, 코루틴당 100 ops):

| 최적화 단계 | concurrent_ops/sec | 개선율 |
|------------|-------------------|--------|
| 기준선 (개별 op) | ~11,737 | — |
| Warmup 안정화 | 16,025 | +36.5% |
| RBatch 파이프라이닝 | 28,571 | +143% |
| **메가배치 (코루틴당 1 RBatch)** | 78,125 | +566% |
| **StringCodec + KEY_POOL** | **92,592** | **+689%** |

> 벤치마크 실행: `./gradlew :bluetape4k-redisson:test --tests "*.RedissonConcurrencyBenchmark"`

---

## Redis 버전 요구사항

| 기능                                                    | 최소 Redis 버전 |
|-------------------------------------------------------|-------------|
| 기본 기능 (Client, Batch, Transaction, Leader)            | Redis 5.0+  |
| RESP3 / CLIENT TRACKING (`bluetape4k-cache-redisson`) | Redis 6.0+  |

## 빌드 및 테스트

테스트 실행 시 Redis 서버가 필요합니다. Testcontainers를 통해 자동 구성됩니다.

```bash
./gradlew :bluetape4k-redisson:test
```
