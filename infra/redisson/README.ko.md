# bluetape4k-redisson

[English](./README.md) | 한국어

Redisson Redis 클라이언트를 Kotlin에서 편리하게 사용할 수 있도록 확장한 모듈입니다. DSL 방식의 클라이언트 생성, 고성능 Codec, Kotlin Coroutines 지원, NearCache 기능을 제공합니다.

## 주요 기능

| 기능                              | 설명                                                                              |
|---------------------------------|---------------------------------------------------------------------------------|
| `RedissonClientSupport`         | DSL 기반 `RedissonClient` / `RedissonReactiveClient` 팩토리, YAML 설정 로드              |
| `RedissonClientExtensions`      | `withBatch {}`, `withTransaction {}` DSL 확장 함수                                  |
| `RedissonClientCoroutine`       | `withSuspendedBatch {}`, `withSuspendedTransaction {}` suspend 확장 함수            |
| `RFutureSupport`                | `Collection<RFuture>.awaitAll()`, `Iterable<RFuture>.sequence()` Coroutines 어댑터 |
| `RedissonCodecs`                | 직렬화(Fory/Kryo5/Jackson3/Fastjson2) × 압축(LZ4/Zstd/Snappy/GZip) 조합 Codec 목록         |
| `RedissonNearCache`             | `RLocalCachedMap` 기반 2-tier Near Cache                                          |

`RedissonCacheConfig` 및 Redisson near-cache 옵션 사용 시:

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
| `RedissonCodecs.FastFory`       | FastFory               | 없음   | FastFory 직렬화만 사용                   |
| `RedissonCodecs.LZ4FastFory`    | FastFory               | LZ4  | FastFory + LZ4 압축                     |
| `RedissonCodecs.ZstdFastFory`   | FastFory               | Zstd | FastFory + Zstd 압축                    |
| `RedissonCodecs.SnappyFastFory` | FastFory               | Snappy | FastFory + Snappy 압축                  |
| `RedissonCodecs.GzipFastFory`   | FastFory               | GZip | FastFory + GZip 압축                    |
| `RedissonCodecs.FastForyComposite`       | FastFory (composite)   | 없음   | FastFory Composite 직렬화                 |
| `RedissonCodecs.LZ4FastForyComposite`    | FastFory (composite)   | LZ4  | FastFory Composite + LZ4 압축             |
| `RedissonCodecs.ZstdFastForyComposite`   | FastFory (composite)   | Zstd | FastFory Composite + Zstd 압축            |
| `RedissonCodecs.SnappyFastForyComposite` | FastFory (composite)   | Snappy | FastFory Composite + Snappy 압축          |
| `RedissonCodecs.GzipFastForyComposite`   | FastFory (composite)   | GZip | FastFory Composite + GZip 압축            |

> ⚠️ **와이어 포맷 경고**: FastFory 코덱은 `CompatibleMode.SCHEMA_CONSISTENT`를 사용합니다. `FastForyCodec`은 구 Fory 데이터를 fallback으로 읽을 수 있으나, `ForyCodec`으로 FastFory 데이터를 읽는 것은 **불가**합니다. 휘발성 캐시 전용.

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

#### 사용 목적별 팩토리 함수

`RedissonCodecs`는 내부 구현을 몰라도 적절한 Codec을 쉽게 선택할 수 있는 사용 목적 기반 팩토리 함수를 제공합니다:

| 팩토리 함수                                  | 반환값              | 설명                                                  |
|----------------------------------------------|---------------------|-------------------------------------------------------|
| `RedissonCodecs.forCache()`                  | `LZ4Fory`           | 처리량 중심 값 캐시 (1KB 이상 객체)                   |
| `RedissonCodecs.forHighThroughput()`         | `LZ4FastFory`       | `forCache()` 대비 ~27% 높은 처리량. 휘발성 캐시 전용 ⚠️ |
| `RedissonCodecs.forCacheMap()`               | `LZ4ForyComposite`  | Map 형 캐시 (RMap, RLocalCachedMap)                   |
| `RedissonCodecs.forGeneral()`                | `Fory`              | 범용 혼합 읽기/쓰기 워크로드                          |
| `RedissonCodecs.forSmallValue()`             | `Kryo5`             | 작은 값 (<1KB) — 압축 오버헤드 생략                   |
| `RedissonCodecs.forArchival()`               | `ZstdFory`          | 아카이브/콜드 스토리지 — 최고 압축률                  |
| `RedissonCodecs.forCompatibility()`          | `Jdk`               | 외부 시스템 상호 운용 (non-bluetape4k)                 |

```kotlin
val config = Config()
// 고처리량 휘발성 캐시에 FastFory + LZ4 조합 사용
config.codec = RedissonCodecs.forHighThroughput()
val redisson = Redisson.create(config)
```

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
// awaitAll()은 입력 순서를 보존하고 현재 coroutine dispatcher를 통해 재개합니다.

// CompletableFuture로 변환 후 일괄 처리 (blocking)
val future: CompletableFuture<List<String>> = rfutures.sequence()
val values: List<String> = future.get()
```

---

### 5. NearCache

Redisson `RLocalCachedMap` 기반 2-tier Near Cache입니다. 로컬 캐시 우선 조회 후 없으면 Redis에서 조회합니다.

```kotlin
import io.bluetape4k.redis.redisson.nearcache.RedissonNearCache

val options = RedissonNearCache.defaultLocalCacheOptions("my-cache")
val nearCache = RedissonNearCache(client, options)

nearCache.put("key", "value")
val value = nearCache.get("key")   // 로컬 캐시에서 우선 조회
```

> JCache 기반의 고급 NearCache (RESP3 하이브리드, Resilient write-behind 등)는 `bluetape4k-cache-redisson` 모듈을 사용하세요.

---

## 아키텍처 다이어그램

### Codec 계층 구조

![Codec 계층 구조 1](../../docs/images/readme-diagrams/infra-redisson-diagram-01.png)

### NearCache 2-Tier 캐시 흐름

![NearCache 2-Tier Cache diagram](../../docs/images/readme-diagrams/infra-redisson-sequence-01.png)

### Batch / Transaction 처리 흐름

![Batch / Transaction 처리 흐름 2](../../docs/images/readme-diagrams/infra-redisson-diagram-02.png)

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

## Codec 벤치마크

`RedissonCodecBenchmark` 기준 (JMH, Apple M4 Pro / GraalVM 21 / Warmup 3×2s / Measurement 5×3s / Fork 1 / 2026-04-27):

| Codec | ops/ms | ± 오차 |
|-------|-------:|-------:|
| **FastFory** | **3,084** | ± 287 |
| Fory | 2,504 | ± 105 |
| fastjson2 | 1,928 | ± 62 |
| Kryo5 | 1,225 | ± 67 |
| LZ4FastFory | 829 | ± 71 |
| LZ4Fory | 774 | ± 42 |
| LZ4Kryo5 | 518 | ± 114 |
| Jackson3 | 474 | ± 25 |
| ZstdFory | 196 | ± 7 |
| ZstdFastFory | 193 | ± 62 |
| ZstdKryo5 | 139 | ± 5 |
| JDK | 128 | ± 14 |
| GzipFastFory | 108 | ± 1 |

> 전체 결과 및 분석: [Benchmark.md](./Benchmark.md) · [한국어](./Benchmark.ko.md)
> 실행: `./gradlew :bluetape4k-redisson:benchmark`

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
| 기본 기능 (Client, Batch, Transaction, NearCache)            | Redis 5.0+  |
| RESP3 / CLIENT TRACKING (`bluetape4k-cache-redisson`) | Redis 6.0+  |

## 빌드 및 테스트

테스트 실행 시 Redis 서버가 필요합니다. Testcontainers를 통해 자동 구성됩니다.

```bash
./gradlew :bluetape4k-redisson:test
```
