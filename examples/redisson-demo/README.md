# Module Examples - Redisson

English | [한국어](./README.ko.md)

A collection of examples demonstrating distributed Redis patterns using [Redisson](https://github.com/redisson/redisson) with Kotlin Coroutines.

## Examples

### Distributed Locks (coroutines/locks/)

| Example File               | Description                          |
|----------------------------|--------------------------------------|
| `LockExamples.kt`          | Basic distributed lock (RLock)       |
| `FairLockExamples.kt`      | Fair lock                            |
| `ReadWriteLockExamples.kt` | Read/write lock                      |
| `MultiLockExamples.kt`     | Multi-lock (across Redis nodes)      |
| `SemaphoreExamples.kt`     | Distributed semaphore                |

### Redis Objects (coroutines/objects/)

| Example File              | Description                               |
|---------------------------|-------------------------------------------|
| `BucketExamples.kt`       | RBucket - single value storage            |
| `BloomFilterExamples.kt`  | RBloomFilter - probabilistic membership   |
| `HyperLogLogExamples.kt`  | RHyperLogLog - cardinality estimation     |
| `GeoExamples.kt`          | RGeo - geospatial storage                 |
| `AtomicLongExamples.kt`   | RAtomicLong - atomic counter              |
| `RateLimiterExamples.kt`  | RRateLimiter - rate limiting              |
| `BinaryStreamExamples.kt` | RBinaryStream - binary data storage       |
| `BatchExamples.kt`        | RBatch - batch operations                 |
| `TopicExamples.kt`        | RTopic - Pub/Sub messaging               |

### Collections (coroutines/collections/)

| Example File                   | Description                              |
|--------------------------------|------------------------------------------|
| `QueueExamples.kt`             | RQueue - distributed queue               |
| `DequeExamples.kt`             | RDeque - distributed deque               |
| `BlockingDequeExamples.kt`     | RBlockingDeque - blocking deque          |
| `ReliableQueueExamples.kt`     | RReliableQueue - reliable queue          |
| `PriorityQueueExamples.kt`     | RPriorityQueue - priority queue          |
| `ScoredSortedSetExamples.kt`   | RScoredSortedSet - scored sorted set     |
| `SortedSetExamples.kt`         | RSortedSet - sorted set                  |
| `RingBufferExamples.kt`        | RRingBuffer - ring buffer                |
| `StreamExamples.kt`            | RStream - Redis Streams                  |
| `LocalCachedMapExamples.kt`    | RLocalCachedMap - locally cached map     |
| `SetMultimapCacheExamples.kt`  | RSetMultimapCache - set multimap cache   |
| `ListMultimapCacheExamples.kt` | RListMultimapCache - list multimap cache |

### Cache Strategies (coroutines/cachestrategy/)

| Example File                    | Description                        |
|---------------------------------|------------------------------------|
| `CacheReadThroughExample.kt`    | Read-Through cache pattern         |
| `CacheWriteThroughExample.kt`   | Write-Through cache pattern        |
| `CacheWriteBehindExample.kt`    | Write-Behind cache pattern         |
| `CacheWriteBehindForIoTData.kt` | Write-Behind example for IoT data  |

### Read/Write Through (coroutines/readwritethrough/)

| Example File                 | Description                       |
|------------------------------|-----------------------------------|
| `MapReadWriteThroughTest.kt` | MapLoader/MapWriter integration   |

## Key Pattern Examples

### Distributed Lock

```kotlin
val lock = redisson.getLock("my-lock")

// Coroutines support
lock.useLocked {
    // Runs while the lock is held
    criticalSection()
}
```

### Read-Through Cache

```kotlin
val map = redisson.getMapCache<String, User>("users")
val config = MapCacheOptions.defaults<String, User>()
    .loader(MyMapLoader(userRepository))

val user = map["user-1"]  // Automatically loaded from DB on cache miss
```

### Distributed Semaphore

```kotlin
val semaphore = redisson.getSemaphore("rate-limiter")
semaphore.trySetPermits(10)

semaphore.useAcquired {
    // Up to 10 concurrent executions
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

## How to Run

```bash
# Start Redis (Docker)
docker run -d --name redis -p 6379:6379 redis:7

# Run all examples
./gradlew :examples:redisson:test

# Run a specific category
./gradlew :examples:redisson:test --tests "*locks*"
./gradlew :examples:redisson:test --tests "*collections*"
```

## Requirements

- Redis 6.0+
- Redisson 3.37+

## References

- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Redisson Kotlin Coroutines](https://github.com/redisson/redisson/tree/master/redisson-kotlin)
