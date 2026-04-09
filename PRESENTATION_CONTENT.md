# Bluetape4K Modules Presentation Content

---

## MODULE 1: CORE (5 slides)

### Slide 1: Core Module Overview
**Title:** Bluetape4K Core - Kotlin Utilities Foundation

**What it solves:**
- Provides essential utility extensions for Kotlin development
- Reduces boilerplate code across common operations
- Offers DDD (Domain-Driven Design) patterns support
- Comprehensive collection, string, and concurrent utilities

**Key Components:**
- Value Objects & Domain Models
- Collection utilities (Ring Buffer, Paginated List)
- String/Codec support (Base58, Base62, Base64, Hex)
- Concurrent utilities & threading helpers
- Java Time/Date extensions
- Apache Commons Lang/Utils integration

### Slide 2: ValueObject - DDD Foundation
**Code Snippet:**
```kotlin
interface ValueObject: Serializable

data class Money(val amount: Long, val currency: String) : ValueObject {
    companion object {
        private const val serialVersionUID = 1L
    }
}

val price = Money(1000L, "KRW")
val same  = Money(1000L, "KRW")
println(price == same) // true — 값으로 동등성 판단
```

**Why it matters:**
- Enforces equality by value, not reference identity
- Compatible with distributed caches (Lettuce, Redisson)
- Serializable for distributed systems
- Foundation for domain-driven design patterns

### Slide 3: Collection Utilities - RingBuffer & PaginatedList
**RingBuffer Example:**
```kotlin
val buffer = RingBuffer<String>(maxSize = 3)
buffer.add("a"); buffer.add("b"); buffer.add("c"); buffer.add("d")
buffer.toList()  // ["b", "c", "d"] - "a" was overwritten

// Thread-safe with ReentrantLock
buffer.size      // 3
```

**PaginatedList:**
- Handles pagination elegantly
- Used for large dataset operations
- Reduces memory footprint in streaming scenarios

### Slide 4: String & Codec Support
**Available Codecs:**
- Base58: Bitcoin-style encoding
- Base62: URL-safe encoding with alphanumerics
- Base64: Standard base64 encoder
- Hex: Hexadecimal string encoding
- URL62: Compact URL-safe format

**StringSupport Functions:**
- `isWhitespace()`: null-safe blank checking
- Pattern matching utilities
- Trimming with ellipsis (TRIMMING = "...")
- Safe type conversions

### Slide 5: Concurrent & Apache Integration
**Apache Commons Extensions:**
- `ApacheStringUtils`: null-safe operations
- `ApacheClassUtils`: reflection helpers
- `ApacheExceptionUtils`: error handling
- `ApacheLocaleUtils`: internationalization

**Concurrent Utilities:**
- `ExecutorSupport`: executor creation/management
- `ThreadSupport`: thread lifecycle management
- `AtomicIntRoundrobin`: lock-free round-robin
- `CompletableFutureSupport`: async operations

---

## MODULE 2: COROUTINES (4 slides)

### Slide 1: Coroutines Module Overview
**Title:** Bluetape4K Coroutines - Structured Async Patterns

**What it solves:**
- Simplifies coroutine lifecycle management
- Provides safe scope abstractions
- Bridges between async and sync operations
- Ring buffer for non-blocking data flow

**Key Features:**
- CloseableCoroutineScope: Auto-cleanup on close
- DeferredValue: Lazy async computation
- SuspendRingBuffer: Non-blocking buffering
- Flow utilities & Reactor integration

### Slide 2: CloseableCoroutineScope - Lifecycle Management
**Code Snippet:**
```kotlin
abstract class CloseableCoroutineScope: CoroutineScope, Closeable {
    private val _closed = atomic(false)
    private val _cancelled = atomic(false)

    val scopeClosed: Boolean get() = _closed.value
    val scopeCancelled: Boolean get() = _cancelled.value

    fun clearJobs(cause: CancellationException? = null) {
        if (_cancelled.compareAndSet(false, true)) {
            coroutineContext.cancelChildren(cause)
            coroutineContext.cancel(cause)
        }
    }

    override fun close() {
        if (_closed.compareAndSet(false, true)) {
            clearJobs()
        }
    }
}
```

**Key Guarantees:**
- Atomic flag prevents duplicate cancellation
- Idempotent behavior - safe to call multiple times
- Auto-cancel on close() - resource cleanup pattern
- Child jobs cancelled before scope context

### Slide 3: DeferredValue - Lazy Async Computation
**Usage Pattern:**
```kotlin
val deferred = deferredValueOf { 21 * 2 }

// Async-style retrieval in coroutines
val value: Int = deferred.await()  // suspend friendly

// Transformations
deferred.map { it * 2 }
        .flatMap { deferredValueOf { it + 1 } }

// State inspection
deferred.isCompleted  // false | true
deferred.isActive     // true | false
deferred.isCancelled  // true | false
```

**When to use:**
- Deferred computation with value object semantics
- Serializable async results (caching)
- Composable async chains with map/flatMap
- Bridge between blocking and non-blocking code

**Deprecation Warning:**
```kotlin
@Deprecated("Use await() in coroutine code")
val value: T // may block with runBlocking
```

### Slide 4: Scope Types & Patterns
**DefaultCoroutineScope:**
- IO operations (newFixedThreadPool)
- Default thread pool
- VirtualThreadCoroutineScope for modern JDKs

**SilentSupervisor:**
- Suppresses exception propagation
- Useful for fire-and-forget operations

**SuspendRingBuffer:**
- Non-blocking ring buffer for channels
- Perfect for bounded queues in streaming

**ThreadPoolCoroutineScope:**
- Custom thread pool integration
- IoCoroutineScope for I/O-bound tasks

---

## MODULE 3: LOGGING (2 slides)

### Slide 1: KLogging - SLF4J Integration
**Code Snippet:**
```kotlin
open class KLogging {
    val log: Logger by lazy { 
        KLoggerFactory.logger(this.javaClass) 
    }
}

// Usage in class/companion
class Service {
    companion object : KLogging()
}

Service.log.info { "Service started" }
```

**Features:**
- Lazy logger initialization
- Class-based logger naming
- SLF4J backend agnostic
- Lightweight mixin pattern

### Slide 2: Logging Extensions - MDC & Coroutines
**Slf4jExtensions:**
- Log level checks (isDebugEnabled, etc.)
- Parameterized logging (avoids string concatenation)
- Exception logging helpers

**Slf4jMdcExtensions:**
- MDC (Mapped Diagnostic Context) support
- Thread-safe context propagation

**Coroutine-aware Logging:**
- `KLoggingChannel`: Logging in coroutine contexts
- Structured logging with context preservation
- Channel-based log aggregation

---

## MODULE 4: VIRTUAL THREADS (4 slides)

### Slide 1: VirtualThreads API Overview
**Title:** Virtual Threads - JDK-Agnostic Abstraction

**What it solves:**
- Abstracts Java 21+ virtual thread APIs
- Runtime auto-detection (JDK 21, 25, or fallback)
- ServiceLoader-based selection
- Single API for multiple JDK versions

**Key Abstractions:**
- VirtualThreadRuntime: Interface for JDK implementations
- VirtualThreads: Singleton selector
- StructuredTaskScopes: Structured concurrency
- Priority-based runtime selection

### Slide 2: VirtualThreads - Runtime Selection & Execution
**Code Example:**
```kotlin
// Auto-detect and use appropriate runtime
val runtimeName = VirtualThreads.runtimeName()  
// "jdk25" | "jdk21" | "platform-fallback"

// Create virtual thread factory
val factory = VirtualThreads.threadFactory(prefix = "my-vt-")

// Create executor service (task-per-thread model)
val executor = VirtualThreads.executorService().use { exec ->
    val result = exec.submit { 
        println("Running on: ${Thread.currentThread()}")
        42 
    }.get()
}
```

**Selection Strategy:**
1. ServiceLoader discovers all VirtualThreadRuntime implementations
2. Filter by isSupported() → true
3. Sort by priority (descending)
4. Select highest priority implementation

**Priority Levels:**
- JDK 25: priority = 25
- JDK 21: priority = 21
- Platform Fallback: priority = Int.MIN_VALUE

### Slide 3: StructuredTaskScopes - Structured Concurrency
**Pattern 1: All Tasks Must Succeed (ShutdownOnFailure)**
```kotlin
val results = StructuredTaskScopes.all(
    name = "fetch-all-data",
    factory = VirtualThreads.threadFactory("data-")
) { scope ->
    val task1 = scope.fork { fetchUserData() }
    val task2 = scope.fork { fetchOrderData() }
    val task3 = scope.fork { fetchInventoryData() }

    scope.join()
        .throwIfFailed { error ->
            println("Failed: ${error.message}")
        }

    Triple(task1.get(), task2.get(), task3.get())
}
```

**Pattern 2: First Success Wins (ShutdownOnSuccess)**
```kotlin
val fastestResult = StructuredTaskScopes.any<String>(
    name = "race-apis",
    factory = VirtualThreads.threadFactory("api-")
) { scope ->
    scope.fork { fetchFromApi1() }
    scope.fork { fetchFromApi2() }
    scope.fork { fetchFromApi3() }

    scope.join()
        .result { error -> RuntimeException("All failed", error) }
}
```

**Key Features:**
- Automatic cancellation of remaining tasks on success/failure
- Structured resource cleanup with use{}
- Deadline support with joinUntil(deadline)
- Strong exception semantics

### Slide 4: JDK 21 vs JDK 25 Implementation Differences
**JDK 21 Implementation (Jdk21StructuredTaskScopeProvider):**
```kotlin
// Uses existing APIs from Java 21
- StructuredTaskScope.ShutdownOnFailure (for withAll)
- StructuredTaskScope.ShutdownOnSuccess (for withAny)
- Subtask state via delegate.state() enum
- Basic join() without deadline
```

**JDK 25 Implementation (Jdk25StructuredTaskScopeProvider):**
```kotlin
// Uses new JDK 25 structured concurrency APIs
- StructuredTaskScope.open<T, T>(Joiner.awaitAll(), ...)
- StructuredTaskScope.open<T, T>(Joiner.anySuccessfulResultOrThrow(), ...)
- Configuration builder pattern for ThreadFactory & naming
- joinUntil(deadline: Instant) with timeout support
- Joiner abstraction for better composability

// Advanced timeout handling:
val scheduler = ScheduledThreadPoolExecutor(1)
scheduler.schedule({ ownerThread.interrupt() }, remainingMillis)
```

**Design Decision Differences:**
- JDK 21: Simplified wrappers around existing ShutdownOnFailure/Success
- JDK 25: Full Joiner abstraction, more flexible, deadline-aware
- Both maintain same high-level API for user code
- Priority ensures JDK 25 selected when available (better perf)

---

## PRESENTATION SUMMARY

### Architecture Principles
1. **Separation of Concerns**: Core, Coroutines, Logging, VirtualThreads are independent
2. **ServiceLoader-based Selection**: Runtime adapts to JDK version automatically
3. **Safe Concurrency**: Atomic flags, ReentrantLock, scope lifecycle management
4. **DDD Support**: ValueObject interface for domain models
5. **Kotlin Idioms**: Extension functions, lazy properties, scope functions

### Cross-Module Usage Patterns
- **Core** provides base utilities consumed by all others
- **Logging** integrated into all modules via KLogging
- **VirtualThreads** coordinates with CloseableCoroutineScope
- **Coroutines** builds on Core's concurrent utilities

### Real-World Scenarios
1. **High-throughput data processing**: RingBuffer + VirtualThreads executor
2. **API fallback/racing**: StructuredTaskScopes.any() with multiple endpoints
3. **Batch operations**: DeferredValue with map/flatMap chains
4. **Resource cleanup**: CloseableCoroutineScope with try-with-resources
5. **Distributed caching**: ValueObject serialization to Lettuce/Redisson

### Performance Characteristics
- Virtual Threads: 1000s of concurrent tasks with minimal overhead
- Ring Buffer: O(1) add/remove with bounded memory
- Atomic operations: Lock-free where possible
- Lazy initialization: Properties only initialized when accessed
