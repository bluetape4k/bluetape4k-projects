# Module bluetape4k-coroutines

## 개요

Kotlin Coroutines 관련 Utility class를 제공합니다.

## 제공하는 기능

### DeferredValue

Coroutines로 값을 지연 계산을 수행할 수 있도록 합니다.
단순하게 지연 계산하는 `LazyValue<T>` 에 비해 값 계산 메소드가 `suspend` 메소드입니다.

```kotlin
val x = DeferredValue {
    delay(100)
    System.currentTimeMillis()
}
println(x.value)

// map, flatMap
val x1 = DeferredValue { delay(100); DeferredValue { delay(100); 42 } }
val x2 = x1.flatMap { r -> r.map { it * 2 } }
```

### Defereed extension methods

`Deferred<T>` 의 extension method 들을 제공합니다.
제공되는 함수는 `zip`, `zipWith`, `map`, `flatMap`, `concatMap` 을 지원합니다.

```kotlin
@Test
fun `zip two deferred instance`() = runSuspendTest {
        val x1 = async { delay(10); "UserId" }
        val x2 = async { delay(20); 42 }

        val value = zip(x1, x2) { a, b -> "$a-$b" }
        value.await() shouldBeEqualTo "UserId-42"
    }

@Test
fun `zipWith with other deferred`() = runSuspendTest {
    val x1 = async { delay(10); "UserId" }
    val x2 = async { delay(20); 42 }

    val value = x1.zipWith(x2) { a, b -> "$a-$b" }
    value.await() shouldBeEqualTo "UserId-42"
}

@Test
fun `map deferred`() = runSuspendTest {
    val x1 = async { delay(10); "UserId" }

    val value = x1.map { "$it-42" }
    value.await() shouldBeEqualTo "UserId-42"
}

@Test
fun `flatMap deferred`() = runSuspendTest {
    val x = async { listOf(1, 2, 3) }

    val list = x.flatMap { v -> List(v) { it + 1 } }
    list.await().toList() shouldContainAll listOf(listOf(1), listOf(1, 2), listOf(1, 2, 3))
}
```

### Flow Extensions

kotlin stdlib 에서 제공하는 `windowed`, `chunked` 메소드를 `Flow<T>`에 대해서 제공합니다.
추가로  `sliding`, `bufferedUnchanged`, `bufferedSliding` 메소드도 제공합니다.

```kotlin
@Test
fun `chunk flow`() = runSuspendTest {
    var chunkCount = 0
    (1..20).asFlow()
        .chunked(5)
        .onEach { elements ->
            log.trace { "elements=$elements" }
            elements.size shouldBeEqualTo 5
            chunkCount++
        }
        .collect()
    chunkCount shouldBeEqualTo 4
}

@Test
fun `windowed flow`() = runSuspendTest {
    var windowedCount = 0

    (1..20).asFlow()
        .windowed(5, 1)
        .onEach { elements ->
            log.trace { "elements=$elements" }
            elements.size shouldBeLessOrEqualTo 5
            windowedCount++
        }
        .collect()

    windowedCount shouldBeEqualTo 20
}
```

`Flow<T>.async` 는 각 요소들을 비동기 방식으로 작업을 수행하도록 합니다.

```kotlin
/**
 * [Flow] 를 [AsyncFlow] 로 변환하여, 각 요소처리를 비동기 방식으로 수행하게 합니다.
 * 단 `flatMapMerge` 처럼 실행완료된 순서로 반환하는 것이 아니라, Flow 의 처음 요소의 순서대로 반환합니다. (Deferred 형식으로)
 */
inline fun <T, R> Flow<T>.async(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    crossinline block: suspend CoroutineScope.(T) -> R,
): AsyncFlow<R> {
    val deferredFlow = map { input -> LazyDeferred(coroutineContext) { block(input) } }
    return AsyncFlow(deferredFlow)
}

/**
 * [AsyncFlow] 의 요소들을 비동기로 매핑합니다.
 */
inline fun <T, R> AsyncFlow<T>.map(crossinline transform: suspend (value: T) -> R): AsyncFlow<R> =
    AsyncFlow(deferredFlow.map { input ->
        LazyDeferred(input.coroutineContext) {
            input.start(this)
            transform(input.await())
        }
    })

/**
 * [AsyncFlow] 의 요소들을 비동기로 실행하고, 순차적으로 수집합니다.
 */
suspend fun <T> AsyncFlow<T>.collect(capacity: Int = Channel.BUFFERED, collector: FlowCollector<T> = NoopCollector) {
    channelFlow {
        deferredFlow
            .buffer(capacity)
            .collect {
                it.start(this)
                send(it)
            }
    }.collect {
        collector.emit(it.await())
    }
}

/**
 * [AsyncFlow] 의 요소들을 [collector]을 통해 비동기로 실행하고, 수집합니다.
 */
suspend inline fun <T> AsyncFlow<T>.collect(
    capacity: Int = Channel.BUFFERED,
    crossinline collector: suspend (value: T) -> Unit,
) {
    collect(capacity, FlowCollector { value -> collector(value) })
}

fun `map asynchronously with dispatcher`() = runSuspendTest {
    val results = intArrayListOf()
    
    (1..20).asFlow()
        .async(dispatcher) {
            delay(Random.nextLong(3))
            log.trace { "Started $it" }
            it
        }
        .map {
            delay(Random.nextLong(3))
            it * it / it
        }
        .collect { curr ->
            // 순서대로 들어와야 한다
            results.lastOrNull()?.let { prev -> curr shouldBeEqualTo prev + 1 }
            results.add(curr)
        }
}
```

## 참고 자료

* [Kotlin Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html)
* [Kotlin Coroutines Best practices](https://kt.academy/article/cc-best-practices)
