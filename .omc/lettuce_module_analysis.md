# Lettuce Module Complete Analysis

## Overview

**Location:** `/infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/`  
**Total Files:** 21 across 10 directories  
**Architecture:** 3-tier API pattern (sync → async → suspend) for all distributed primitives

---

## Directory Structure & File Sizes

```
lettuce/
├── atomic/
│   └── RedisAtomicLong.kt (324 lines)
├── codec/
│   ├── LettuceBinaryCodec.kt
│   └── LettuceBinaryCodecs.kt (112 lines)
├── leader/
│   ├── LettuceLeaderElection.kt
│   ├── LettuceLeaderElectionOptions.kt
│   ├── LettuceLeaderElectionSupport.kt
│   ├── LettuceLeaderGroupElection.kt
│   └── coroutines/
│       ├── LettuceSuspendLeaderElection.kt
│       └── LettuceSuspendLeaderGroupElection.kt
├── lock/
│   └── RedisLock.kt (329 lines)
├── map/
│   └── RedisMap.kt (442 lines)
├── memoizer/ (3 files: Async, Sync, Suspend)
├── memorizer/ (3 files: Async, Sync, Suspend)
├── semaphore/
│   └── RedisSemaphore.kt
├── LettuceClients.kt
├── LettuceConst.kt (36 lines)
└── RedisFutureSupport.kt
```

---

## Core Classes & APIs

### 1. RedisMap (442 lines)

**Definition:**

```kotlin
class RedisMap(
    private val connection: StatefulRedisConnection<String, String>,
    val mapKey: String
)
```

**Type Parameters:** NONE (not generic)

- Keys: `String` (hardcoded)
- Values: `String` (hardcoded)
- Cannot be parameterized with `RedisMap<K, V>`

**3-Tier API:**

| Sync                                         | Async                                                 | Suspend                                 |
|----------------------------------------------|-------------------------------------------------------|-----------------------------------------|
| `get(field: String): String?`                | `getAsync(field): CompletableFuture<String?>`         | `getSuspending(field): String?`         |
| `put(field: String, value: String): Boolean` | `putAsync(field, value): CompletableFuture<Boolean>`  | `putSuspending(field, value): Boolean`  |
| `remove(field: String): Boolean`             | `removeAsync(field): CompletableFuture<Boolean>`      | `removeSuspending(field): Boolean`      |
| `size(): Long`                               | `sizeAsync(): CompletableFuture<Long>`                | `sizeSuspending(): Long`                |
| `containsKey(field): Boolean`                | `containsKeyAsync(field): CompletableFuture<Boolean>` | `containsKeySuspending(field): Boolean` |
| `keys(): Set<String>`                        | `keysAsync(): CompletableFuture<Set<String>>`         | `keysSuspending(): Set<String>`         |
| `clear()`                                    | `clearAsync(): CompletableFuture<Void>`               | `clearSuspending()`                     |

**Lettuce API Used:**

- `RedisCommands<String, String>` (sync) via `connection.sync()`
- `RedisAsyncCommands<String, String>` (async) via `connection.async()`
- Redis Hash commands: `HGET`, `HSET`, `HDEL`, `HLEN`, `HEXISTS`, `HKEYS`, `HGETALL`, `HCLEAR`

**Helper Functions:**

- `awaitSuspending()` from `RedisFutureSupport` — converts `CompletableFuture<T>` to suspend
- `log.debug()` — operation logging via `KLogging`

---

### 2. RedisSemaphore

**Definition:**

```kotlin
class RedisSemaphore(
    private val connection: StatefulRedisConnection<String, String>,
    val semaphoreKey: String,
    val totalPermits: Int
)
```

**Type Parameters:** NONE

**Key Methods:**

| Sync                                                    | Async                                                  | Suspend                                                       |
|---------------------------------------------------------|--------------------------------------------------------|---------------------------------------------------------------|
| `initialize()` / `trySetPermits(permits: Int): Boolean` | `initializeAsync()` / `trySetPermitsAsync(permits)`    | `initializeSuspending()` / `trySetPermitsSuspending(permits)` |
| `tryAcquire(permits: Int = 1): Boolean`                 | `tryAcquireAsync(permits): CompletableFuture<Boolean>` | `tryAcquireSuspending(permits): Boolean`                      |
| `acquire(permits: Int = 1, waitTime: Duration = 30s)`   | `acquireAsync(permits, waitTime)`                      | `acquireSuspending(permits, waitTime)`                        |
| `release(permits: Int = 1): Long`                       | `releaseAsync(permits): CompletableFuture<Long>`       | `releaseSuspending(permits): Long`                            |

**Implementation Details:**

- Uses **Lua script** `ACQUIRE_SCRIPT` for atomic increment/decrement
- `syncCommands.eval<Long>(ACQUIRE_SCRIPT, ScriptOutputType.INTEGER, arrayOf(semaphoreKey), permits.toString())`
- Sync `acquire()` uses polling loop with `Thread.sleep(RETRY_DELAY_MS)` until deadline
- Throws `IllegalStateException` on timeout

---

### 3. RedisAtomicLong (324 lines)

**Definition:**

```kotlin
class RedisAtomicLong(
    private val connection: StatefulRedisConnection<String, String>,
    val atomicKey: String
)
```

**Type Parameters:** NONE

**3-Tier API:**

| Sync                                     | Async                                 | Suspend                                   |
|------------------------------------------|---------------------------------------|-------------------------------------------|
| `get(): Long`                            | `getAsync(): CompletableFuture<Long>` | `getSuspending(): Long`                   |
| `set(newValue: Long)`                    | `setAsync(newValue)`                  | `setSuspending(newValue)`                 |
| `incrementAndGet(): Long`                | `incrementAndGetAsync()`              | `incrementAndGetSuspending()`             |
| `decrementAndGet(): Long`                | `decrementAndGetAsync()`              | `decrementAndGetSuspending()`             |
| `addAndGet(delta: Long): Long`           | `addAndGetAsync(delta)`               | `addAndGetSuspending(delta)`              |
| `getAndAdd(delta: Long): Long`           | `getAndAddAsync(delta)`               | `getAndAddSuspending(delta)`              |
| `compareAndSet(expect, update): Boolean` | `compareAndSetAsync(expect, update)`  | `compareAndSetSuspending(expect, update)` |

**Lettuce Commands:** `INCRBY`, `GET`, `SET`, `INCR`, `DECR`, `GETSET`

---

### 4. RedisLock (329 lines)

**Definition:**

```kotlin
class RedisLock(
    private val connection: StatefulRedisConnection<String, String>,
    val lockKey: String,
    val holdDuration: Duration = 30.seconds,
    val retryDelayMs: Long = 100L
)
```

**3-Tier API:**

| Sync                                        | Async                                    | Suspend                      |
|---------------------------------------------|------------------------------------------|------------------------------|
| `lock(): String`                            | `lockAsync(): CompletableFuture<String>` | `lockSuspending(): String`   |
| `unlock(lockId: String)`                    | `unlockAsync(lockId)`                    | `unlockSuspending(lockId)`   |
| `tryLock(timeout: Duration = 30s): String?` | `tryLockAsync(timeout)`                  | `tryLockSuspending(timeout)` |

**Locking Mechanism:**

- **Lock Acquisition:** `SET lockKey lockId NX EX holdDuration.toSeconds()`
- **Unlock:** Lua script checks owner before deleting (prevents accidental unlock of other's lock)
- `lockId` = UUID to verify lock ownership

---

## Codec System

### LettuceBinaryCodec<V: Any>

**Implements:**

- `RedisCodec<String, V>`
- `ToByteBufEncoder<String, V>`

**Type Parameter Constraint:** `V: Any`

- Keys always `String` (Lettuce/Redis convention)
- Values are generic `V` serialized by `BinarySerializer`

**Core Methods:**

```kotlin
override fun encodeKey(key: String?): ByteBuffer
override fun encodeKey(key: String?, target: ByteBuf)
override fun encodeValue(value: V): ByteBuffer
override fun encodeValue(value: V, target: ByteBuf?)

override fun decodeKey(bytes: ByteBuffer): String
override fun decodeValue(bytes: ByteBuffer): V
```

**Serialization Logic:**

- `encodeValue(v)` → `serializer.serialize(v)` → `ByteBuffer`
- `decodeValue(bytes)` → `serializer.deserialize(bytes)` → `V`

### LettuceBinaryCodecs (Factory Object)

**Purpose:** Provides factory methods for creating
`LettuceBinaryCodec<V>` with various serializer/compressor combinations.

**Available Codecs:**

- `LettuceBinaryCodecs.Default` → `lz4Fory()` (lazy-initialized)
- `jdk()`, `kryo()`, `fory()`
- `gzipJdk()`, `gzipKryo()`, `gzipFory()`
- `lz4Jdk()`, `lz4Kryo()`, `lz4Fory()`
- `zstdJdk()`, `zstdKryo()`, `zstdFory()`

**Example Usage:**

```kotlin
val codec = LettuceBinaryCodecs.lz4Fory() // LettuceBinaryCodec<Any>
val connection = LettuceClients.connect(client, codec)
val commands = connection.async() // RedisAsyncCommands<String, Any>
```

**Leverages:** `BinarySerializers` from `io/io` module for all serialization/compression logic.

---

## LettuceClients (Factory & Connection Manager)

**Purpose:** Central factory for creating Lettuce clients and managing connection pool.

**Type-Safe Connection Caching:**

```kotlin
private val defaultConnections: ConcurrentHashMap<RedisClient, StatefulRedisConnection<String, String>>
private val codecConnections: ConcurrentHashMap<CodecConnectionKey<*>, StatefulRedisConnection<String, *>>

private data class CodecConnectionKey<V: Any>(
    val client: RedisClient,
    val codec: RedisCodec<String, V>
)
```

**API Methods:**

| Method                                  | Return Type                               | Usage                                     |
|-----------------------------------------|-------------------------------------------|-------------------------------------------|
| `commands(client)`                      | `RedisCommands<String, String>`           | Sync commands, StringCodec                |
| `<V> commands(client, codec)`           | `RedisCommands<String, V>`                | Sync commands, custom codec               |
| `asyncCommands(client)`                 | `RedisAsyncCommands<String, String>`      | Async, StringCodec                        |
| `<V> asyncCommands(client, codec)`      | `RedisAsyncCommands<String, V>`           | Async, custom codec                       |
| `coroutinesCommands(client)`            | `RedisCoroutinesCommands<String, String>` | Coroutines (experimental), StringCodec    |
| `<V> coroutinesCommands(client, codec)` | `RedisCoroutinesCommands<String, V>`      | Coroutines (experimental), custom codec   |
| `shutdown(client)`                      | `Unit`                                    | Closes all connections, shuts down client |

**Connection Management:**

- Reuses connections for same (client, codec) pair
- Auto-reconnects if connection is closed
- `@Suppress("UNCHECKED_CAST")` for generic caching safety

---

## Leader Election Module

### Sync/Async Variants

- `LettuceLeaderElection` — single leader election
- `LettuceLeaderGroupElection` — group-based elections
- `LettuceLeaderElectionOptions` — configuration
- `LettuceLeaderElectionSupport` — shared utilities

### Coroutines Variants

- `LettuceSuspendLeaderElection` — suspend version
- `LettuceSuspendLeaderGroupElection` — suspend group version

**Implementation:** Lua scripts + polling (similar to semaphore pattern)

---

## Key Design Patterns

### 1. 3-Tier API Pattern

All distributed primitives (Map, Semaphore, AtomicLong, Lock, Leader Election) follow:

```
Sync (blocking)
  ↓
Async (CompletableFuture)
  ↓
Suspend (coroutines)
```

Benefits:

- Sync-first for simplicity
- Async for non-blocking flows
- Suspend for coroutine-based code

### 2. Lua Scripts for Atomicity

- Semaphore: `ACQUIRE_SCRIPT` for atomic permit management
- Lock: Lua script for safe ownership-verified unlock
- No distributed lock abstraction; operations are script-based

### 3. String Keys Hardcoded

- `RedisMap`, `RedisSemaphore`, `RedisAtomicLong`, `RedisLock` all use `String` keys
- NOT generic: no `RedisMap<K, V>` where K varies
- For typed values: use `LettuceBinaryCodec<V>` with `LettuceClients`

### 4. Connection Pooling

- Single pool per (RedisClient, Codec) pair
- Lazy initialization & reuse
- Auto-reconnect on closed connections

### 5. No Separate Suspend Classes

- Unlike some libraries, there is **NO** separate `RedisSuspendMap` class
- `RedisMap` itself provides both sync and suspend methods
- Suspend methods use `awaitSuspending()` internally

---

## Important Notes

### No RedisSuspendMap

**Finding:** No `RedisSuspendMap` class exists.

- `RedisMap` includes suspend methods directly
- Pattern: `map.getSuspending(field)` not a separate class
- Follows same design as other primitives

### No Generic RedisMap<K, V>

**Constraint:** `RedisMap` is hardcoded to `String` keys and values.

- Cannot parameterize as `RedisMap<MyType, MyOtherType>`
- For typed values: use `LettuceBinaryCodec<V: Any>` factory methods
- Example:
  ```kotlin
  val codec = LettuceBinaryCodecs.lz4Fory() // LettuceBinaryCodec<Any>
  val connection = LettuceClients.connect(client, codec)
  val commands = connection.async() // RedisAsyncCommands<String, Any>
  // Now use commands to work with Any type
  ```

### Protobuf Support

- `LettuceBinaryCodecs` is for general binary serializers
- For Protobuf values: use `io.bluetape4k.protobuf.serializers.LettuceProtobufCodecs` (separate module)

### Memoizer vs Memorizer

- Both directories exist: `memoizer/` and `memorizer/`
- Both contain 3 versions each (Async, Sync, Suspend)
- Slight naming difference; check documentation for semantic distinction

---

## File Locations Reference

All paths relative to
`/Users/debop/work/bluetape4k/bluetape4k-projects/infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/`:

- `map/RedisMap.kt` — distributed map
- `semaphore/RedisSemaphore.kt` — distributed semaphore
- `atomic/RedisAtomicLong.kt` — distributed atomic long
- `lock/RedisLock.kt` — distributed lock
- `codec/LettuceBinaryCodec.kt` — codec interface & implementation
- `codec/LettuceBinaryCodecs.kt` — codec factory methods
- `LettuceClients.kt` — connection factory & pool manager
- `RedisFutureSupport.kt` — async-to-suspend helpers
- `LettuceConst.kt` — constants (host, port, timeout)
- `leader/LettuceLeaderElection.kt` — leader election
- `leader/coroutines/LettuceSuspendLeaderElection.kt` — suspend leader election

---

## Summary

The Lettuce module provides a **cohesive 3-tier API** for Redis distributed primitives:

| Primitive             | Type               | Status                         |
|-----------------------|--------------------|--------------------------------|
| RedisMap              | `<String, String>` | Complete (sync/async/suspend)  |
| RedisSemaphore        | Semaphore          | Complete (sync/async/suspend)  |
| RedisAtomicLong       | AtomicLong         | Complete (sync/async/suspend)  |
| RedisLock             | Lock               | Complete (sync/async/suspend)  |
| LettuceLeaderElection | Leader Elect       | Complete (sync/async/suspend)  |
| Codec System          | `<String, V: Any>` | Complete (10+ factory methods) |

All leverage Lettuce's low-level APIs (`StatefulRedisConnection`, `RedisCommands`, `RedisAsyncCommands`,
`RedisCoroutinesCommands`) with type-safe connection pooling and factory patterns.
