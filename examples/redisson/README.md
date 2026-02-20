# Examples - Redisson

[Redisson](https://github.com/redisson/redisson)을 Kotlin Coroutines와 함께 사용하는 분산 Redis 패턴 예제 모음입니다.

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

| 예제 파일                             | 설명                                  |
|-----------------------------------|-------------------------------------|
| `QueueExamples.kt`                | RQueue - 분산 큐                       |
| `DequeExamples.kt`                | RDeque - 분산 데크                      |
| `BlockingDequeExamples.kt`        | RBlockingDeque - 블로킹 데크             |
| `BoundedBlockingQueueExamples.kt` | RBoundedBlockingQueue - 크기 제한 블로킹 큐 |
| `PriorityQueueExamples.kt`        | RPriorityQueue - 우선순위 큐             |
| `ScoredSortedSetExamples.kt`      | RScoredSortedSet - 점수 정렬 집합         |
| `SortedSetExamples.kt`            | RSortedSet - 정렬 집합                  |
| `RingBufferExamples.kt`           | RRingBuffer - 링 버퍼                  |
| `StreamExamples.kt`               | RStream - Redis Streams             |
| `LocalCachedMapExamples.kt`       | RLocalCachedMap - 로컬 캐시 맵           |
| `SetMultimapCacheExamples.kt`     | RSetMultimapCache - 멀티맵 캐시          |
| `ListMultimapCacheExamples.kt`    | RListMultimapCache - 리스트 멀티맵 캐시     |

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

## 실행 방법

```bash
# Redis 실행 (Docker)
docker run -d --name redis -p 6379:6379 redis:7

# 모든 예제 실행
./gradlew :examples:redisson:test

# 특정 카테고리만 실행
./gradlew :examples:redisson:test --tests "*locks*"
./gradlew :examples:redisson:test --tests "*collections*"
```

## 요구사항

- Redis 6.0+
- Redisson 3.37+

## 참고

- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Redisson Kotlin Coroutines](https://github.com/redisson/redisson/tree/master/redisson-kotlin)
