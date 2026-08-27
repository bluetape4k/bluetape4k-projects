# Module Examples - Redisson

[English](./README.md) | 한국어

[Redisson](https://github.com/redisson/redisson)을 Kotlin Coroutines와 함께 사용하는 분산 Redis 패턴 예제 모음입니다.

![Redisson demo pattern map](../../docs/images/readme-diagrams/examples-redisson-demo-diagram-01.png)

## 예제 목록

### 분산 락 (coroutines/locks/)

| 예제 파일                      | 설명                 |
|----------------------------|--------------------|
| `LockExamples.kt`          | 기본 분산 락 (RLock)    |
| `FairLockExamples.kt`      | 공정 락 (Fair Lock)   |
| `ReadWriteLockExamples.kt` | 읽기/쓰기 락            |
| `MultiLockExamples.kt`     | 다중 락 (여러 Redis 노드) |
| `SemaphoreExamples.kt`     | 분산 세마포어            |

### Redis 객체 (coroutines/objects/)

| 예제 파일                     | 설명                         |
|---------------------------|----------------------------|
| `BucketExamples.kt`       | RBucket - 단일 값 저장          |
| `BloomFilterExamples.kt`  | RBloomFilter - 확률적 멤버십 테스트 |
| `HyperLogLogExamples.kt`  | RHyperLogLog - 카디널리티 추정    |
| `GeoExamples.kt`          | RGeo - 지리적 위치 저장           |
| `AtomicLongExamples.kt`   | RAtomicLong - 원자적 카운터      |
| `RateLimiterExamples.kt`  | RRateLimiter - 속도 제한       |
| `BinaryStreamExamples.kt` | RBinaryStream - 바이너리 데이터   |
| `BatchExamples.kt`        | RBatch - 일괄 처리             |
| `TopicExamples.kt`        | RTopic - Pub/Sub 메시징       |

### 컬렉션 (coroutines/collections/)

| 예제 파일                          | 설명                              |
|--------------------------------|---------------------------------|
| `QueueExamples.kt`             | RQueue - 분산 큐                   |
| `DequeExamples.kt`             | RDeque - 분산 데크                  |
| `BlockingDequeExamples.kt`     | RBlockingDeque - 블로킹 데크         |
| `ReliableQueueExamples.kt`     | RReliableQueue - 신뢰성 큐          |
| `PriorityQueueExamples.kt`     | RPriorityQueue - 우선순위 큐         |
| `ScoredSortedSetExamples.kt`   | RScoredSortedSet - 점수 정렬 집합     |
| `SortedSetExamples.kt`         | RSortedSet - 정렬 집합              |
| `RingBufferExamples.kt`        | RRingBuffer - 링 버퍼              |
| `StreamExamples.kt`            | RStream - Redis Streams         |
| `LocalCachedMapExamples.kt`    | RLocalCachedMap - 숫자 원자적 갱신과 로컬 무효화 |
| `SetMultimapCacheExamples.kt`  | RSetMultimapCache - 멀티맵 캐시      |
| `ListMultimapCacheExamples.kt` | RListMultimapCache - 리스트 멀티맵 캐시 |

### 캐시 전략 (coroutines/cachestrategy/)

| 예제 파일                           | 설명                      |
|---------------------------------|-------------------------|
| `CacheReadThroughExample.kt`    | Read-Through 캐시 패턴      |
| `CacheWriteThroughExample.kt`   | Write-Through 캐시 패턴     |
| `CacheWriteBehindExample.kt`    | Write-Behind 캐시 패턴      |
| `CacheWriteBehindForIoTData.kt` | IoT 데이터 Write-Behind 예제 |

### Read/Write Through (coroutines/readwritethrough/)

| 예제 파일                        | 설명                     |
|------------------------------|------------------------|
| `MapReadWriteThroughTest.kt` | MapLoader/MapWriter 연동 |

## 주요 패턴 예시

### 분산 락

```kotlin
val lock = redisson.getLock("my-lock")

// Coroutines 지원
lock.useLocked {
    // 락이 걸린 상태로 실행
    criticalSection()
}
```

### Read-Through 캐시

```kotlin
val map = redisson.getMapCache<String, User>("users")
val config = MapCacheOptions.defaults<String, User>()
    .loader(MyMapLoader(userRepository))

val user = map["user-1"]  // 캐시 미스 시 DB에서 자동 로드
```

### 분산 세마포어

```kotlin
val semaphore = redisson.getSemaphore("rate-limiter")
semaphore.trySetPermits(10)

semaphore.useAcquired {
    // 최대 10개 동시 실행
    limitedResource()
}
```

### Bloom Filter

```kotlin
val bloomFilter = redisson.getBloomFilter<String>("emails")
bloomFilter.tryInit(10000, 0.01)

bloomFilter.add("user@example.com")
val exists = bloomFilter.contains("user@example.com")  // true
```

### LocalCachedMap 숫자 갱신과 무효화

`LocalCachedMapExamples.kt`는 Int와 Double 값을 서로 다른 맵에 저장하고,
로컬 view와 backend view에 동일한 `CompositeCodec`(String key와 해당 숫자
value codec)를 전달합니다. `addAndGetAsync`는 Redis의 `HINCRBYFLOAT`를
사용하므로 hash field에 저장된 값도 숫자로 해석될 수 있어야 합니다. 타입이
맞지 않는 값을 저장한 경우에는 Redisson `RedisException`이 발생하는 음수
계약을 테스트합니다.

`LocalCachedMapTest.kt`는 두 Redisson client를 사용합니다. 한 local cached
map을 통한 쓰기는 다른 client의 캐시 값을 비동기적으로 무효화합니다. 쓰기
직후의 읽기에서는 이전 값이 잠시 보일 수 있으므로, 100 ms 간격으로 최대
5초까지 대기한 뒤 갱신된 값 또는 삭제 결과를 확인합니다. 테스트는
Testcontainers로 Redis를 시작하고 동적 포트를 사용하므로 Docker daemon이
실행 중이어야 합니다.

## 실행 방법

```bash
# Redis 실행 (Docker)
docker run -d --name redis -p 6379:6379 redis:7

# 이 모듈의 모든 예제 실행
./gradlew :bluetape4k-examples-redisson-demo:test \
  --no-configuration-cache --max-workers=1

# LocalCachedMap 계약 테스트 실행
./gradlew :bluetape4k-examples-redisson-demo:test \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
  --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest' \
  --no-configuration-cache --max-workers=1
```

## 요구사항

- Redis 6.0+
- Redisson 3.37+

## 참고

- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Redisson Kotlin Coroutines](https://github.com/redisson/redisson/tree/master/redisson-kotlin)
