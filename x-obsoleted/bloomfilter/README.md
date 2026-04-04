# Module bluetape4k-bloomfilter

English | [한국어](./README.ko.md)

A Bloom Filter is a probabilistic data structure used to test whether an element is a member of a set.

## Features

- **Probabilistic data structure**: When an element is judged to belong to the set, it may in fact not — this is known as a **false positive**. However, false positives are bounded by the configured error rate.
- **No false negatives**: If an element is judged not to belong to the set, it is guaranteed to be absent — **false negatives never occur**.
- **No deletion (by default)**: Elements can be added to the set, but the basic Bloom Filter does not support removal. (Use `MutableBloomFilter` for deletion support.)
- **Space-efficient**: Represents large datasets using very little memory.

**Note**: As the number of elements in the set grows, the probability of false positives also increases.

![Bloom Filter](../doc/720px-Bloom_filter.svg.png)

## Architecture

### Interface Hierarchy

```
BloomFilter<T>              Synchronous API (add, contains, clear)
├── MutableBloomFilter<T>   Deletion support (remove, approximateCount)
│
SuspendBloomFilter<T>       Coroutines API (suspend add, contains, clear)
```

### Implementations

| Implementation | Backend | Deletion | Coroutines |
|--------|--------|------|--------|
| `InMemoryBloomFilter<T>` | `BitSet` (JVM memory) | No | No |
| `InMemoryMutableBloomFilter` | Counting buckets (`LongArray`) | Yes | No |
| `InMemorySuspendBloomFilter<T>` | `BitSet` (JVM memory) | No | Yes |
| `RedissonBloomFilter<T>` | Redis `BitSet` (Redisson) | No | No |
| `RedissonSuspendBloomFilter<T>` | Redis `BitSet` (async Redisson) | No | Yes |
| `LettuceBloomFilter<T>` | Redis `SETBIT/GETBIT` (Lettuce, Lua Script) | No | No |
| `LettuceAsyncBloomFilter<T>` | Redis `SETBIT/GETBIT` (Lettuce Async, Lua Script) | No | No |
| `LettuceSuspendBloomFilter<T>` | Redis `SETBIT/GETBIT` (Lettuce Async, Lua Script) | No | Yes |

### Hashing Strategy

- **Algorithm**: Murmur3 (via the zero-allocation-hashing library)
- **Offset derivation**: Derives `k` unique offsets from a single hash value
- **Supported types**: `Int`, `Long`, `String`, `ByteArray`, `Serializable` (others are converted via `toString()` before hashing)

## Use Cases

- **Cache filtering**: Check the Bloom Filter before hitting the DB to avoid unnecessary queries
- **Duplicate event detection**: Prevent duplicate event processing in Event Sourcing systems
- **Notification deduplication**: Prevent sending duplicate notifications in alerting services
- **ID deduplication**: Prevent collisions when generating time-based IDs in distributed systems

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-bloomfilter:${bluetape4kVersion}")
}
```

## Basic Usage

### InMemoryBloomFilter

A Bloom Filter implementation backed by an in-memory `BitSet`.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemoryBloomFilter

val bloomFilter = InMemoryBloomFilter<String>()

// Add elements
val items = listOf("item1", "item2", "item3")
items.forEach { bloomFilter.add(it) }

// Check membership
bloomFilter.contains("item1")  // true
bloomFilter.contains("item4")  // false (or false positive)

// Check all elements
items.all { bloomFilter.contains(it) }  // true
```

### InMemoryMutableBloomFilter

A Bloom Filter implementation that supports element removal.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemoryMutableBloomFilter

val bloomFilter = InMemoryMutableBloomFilter()

bloomFilter.add("test-item")
bloomFilter.contains("test-item")  // true

// Remove element
bloomFilter.remove("test-item")
bloomFilter.contains("test-item")  // false

// Approximate count
bloomFilter.approximateCount("test-item")  // 0
```

### SuspendBloomFilter (Coroutines Support)

A Bloom Filter for use in coroutine contexts.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemorySuspendBloomFilter
import kotlinx.coroutines.runBlocking

val bloomFilter = InMemorySuspendBloomFilter<String>()

runBlocking {
    // Add element (suspend function)
    bloomFilter.add("coroutine-item")
    
    // Check membership (suspend function)
    bloomFilter.contains("coroutine-item")  // true
    
    // Clear (suspend function)
    bloomFilter.clear()
}
```

### RedissonBloomFilter

A distributed Bloom Filter backed by Redis via Redisson.

```kotlin
import io.bluetape4k.bloomfilter.redis.RedissonBloomFilter
import org.redisson.Redisson

val redisson = Redisson.create()
val bloomFilter = RedissonBloomFilter<String>(
    redisson = redisson,
    bloomName = "my-bloom-filter"
)

// Same API as InMemoryBloomFilter
bloomFilter.add("redis-item")
bloomFilter.contains("redis-item")  // true
```

### RedissonSuspendBloomFilter

A coroutines-enabled Bloom Filter backed by Redis via Redisson.

```kotlin
import io.bluetape4k.bloomfilter.redis.RedissonSuspendBloomFilter

val bloomFilter = RedissonSuspendBloomFilter<String>(
    redisson = redisson,
    bloomName = "my-suspend-bloom-filter"
)

runBlocking {
    bloomFilter.add("suspend-redis-item")
    bloomFilter.contains("suspend-redis-item")  // true
}
```

### LettuceBloomFilter

A distributed Bloom Filter backed by Redis via Lettuce. Batch bit operations are executed atomically using Lua scripts.

```kotlin
import io.bluetape4k.bloomfilter.redis.LettuceBloomFilter
import io.lettuce.core.RedisClient

val client = RedisClient.create("redis://localhost")
val connection = client.connect()
val bloomFilter = LettuceBloomFilter<String>(
    connection = connection,
    bloomName = "my-lettuce-bloom-filter"
)

// Same API as InMemoryBloomFilter
bloomFilter.add("lettuce-item")
bloomFilter.contains("lettuce-item")  // true
```

### LettuceAsyncBloomFilter

A Bloom Filter using the Lettuce async API. All operations return a `RedisFuture`.

```kotlin
import io.bluetape4k.bloomfilter.redis.LettuceAsyncBloomFilter

val bloomFilter = LettuceAsyncBloomFilter<String>(
    connection = connection,
    bloomName = "my-async-bloom-filter"
)

// Async add
val addFuture = bloomFilter.addAsync("async-item")
addFuture.get()  // Wait for completion

// Async membership check
val containsFuture = bloomFilter.containsAsync("async-item")
val exists = containsFuture.get() == 1L  // true
```

### LettuceSuspendBloomFilter

A coroutines-enabled Bloom Filter backed by Lettuce.

```kotlin
import io.bluetape4k.bloomfilter.redis.LettuceSuspendBloomFilter

val bloomFilter = LettuceSuspendBloomFilter<String>(
    connection = connection,
    bloomName = "my-suspend-lettuce-bloom-filter"
)

runBlocking {
    bloomFilter.add("suspend-lettuce-item")
    bloomFilter.contains("suspend-lettuce-item")  // true
}
```

## Advanced Configuration

### Setting Maximum Elements and Error Rate

```kotlin
val bloomFilter = InMemoryBloomFilter<String>(
    maxNum = 100_000L,      // Up to 100,000 elements
    errorRate = 0.001       // 0.1% error rate
)

// Check automatically calculated parameters
println("Bit size: ${bloomFilter.m}")      // Bloom Filter size (bits)
println("Hash count: ${bloomFilter.k}")    // Number of hash functions
```

### Calculating False Positive Probability

```kotlin
val bloomFilter = InMemoryBloomFilter<String>()

// Probability of a bit being zero after n insertions
val zeroProb = bloomFilter.getBitZeroProbability(n = 1000)

// False positive probability after n insertions
val fpProb = bloomFilter.getFalsePositiveProbability(n = 1000)

// Bits per element
val bitsPerElement = bloomFilter.getBitsPerElement(n = 1000)
```

## Performance

- **Time complexity**: O(k) — k is the number of hash functions (typically 1–10)
- **Space complexity**: O(m) — m is the Bloom Filter size in bits
- **Hashing algorithm**: Murmur3

## Caveats

1. **False Positives**: A Bloom Filter guarantees no false negatives, but "present" results may occasionally be incorrect.
2. **Deletion limitation**: The basic Bloom Filter does not support element removal. Use `MutableBloomFilter` if deletion is needed.
3. **Capacity planning**: Configure the filter size based on the expected number of elements and the acceptable false positive rate.

## References

- [Bloom Filter (Wikipedia)](https://en.wikipedia.org/wiki/Bloom_filter)
- [Redisson](https://github.com/redisson/redisson)
- [Lettuce](https://github.com/redis/lettuce)
