package io.bluetape4k.coroutines.support

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.stream.consumeAsFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Java Stream 을 [Flow] 처럼 사용합니다.
 *
 * @see [consumeAsFlow]
 *
 * @param T 요소의 수형
 * @return [Flow] 인스턴스
 */
fun <T> Stream<T>.asFlow(): Flow<T> = consumeAsFlow()

/**
 * Java Stream 을 [Flow] 처럼 Coroutines 환경에서 `forEach` 처럼 사용합니다.
 *
 * ```
 * val stream = Stream.of(1, 2, 3)
 * stream.suspendForEach { delay(10); println(it) }  // 1, 2, 3
 * ```
 *
 * @see [consumeAsFlow]
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param consumer 요소를 처리하는 suspend 함수
 */
suspend fun <T> Stream<T>.suspendForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    consumer: suspend (T) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .collect { consumer(it) }
}

@Deprecated(
    message = "Use suspendForEach instead",
    replaceWith = ReplaceWith("suspendForEach(coroutineContext, consumer)")
)
suspend fun <T> Stream<T>.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    consumer: suspend (T) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .collect { consumer(it) }
}

/**
 * Java Stream 을 [Flow] 처럼 Coroutines 환경에서 `map` 처럼 실행합니다.
 *
 * ```
 * val stream = Stream.of(1, 2, 3)
 * val flow = stream.suspendMap { it * 2 }
 * flow.collect { println(it) }  // 2, 4, 6
 * ```
 *
 * @see [consumeAsFlow]
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param transform 요소를 변환하는 suspend 함수
 * @return [Flow] 인스턴스
 */
inline fun <T, R> Stream<T>.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (T) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .collect { send(transform(it)) }
}

@Deprecated(
    message = "Use suspendMap instead",
    replaceWith = ReplaceWith("suspendMap(coroutineContext, transform)")
)
inline fun <T, R> Stream<T>.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (T) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .collect { send(transform(it)) }
}

/**
 * [IntStream]을 [Flow] 처럼 사용합니다.
 */
fun IntStream.consumeAsFlow(): Flow<Int> = IntStreamFlow(this)

/**
 * [IntStream]을 [Flow] 처럼 사용합니다.
 */
fun IntStream.asFlow(): Flow<Int> = consumeAsFlow()

/**
 * [IntStream]을 [Flow] 처럼 Coroutines 환경에서 `forEach` 처럼 사용합니다.
 *
 * ```
 * val stream = IntStream.range(1, 4)
 * stream.suspendForEach { delay(10); println(it) }  // 1, 2, 3
 * ```
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param consumer 요소를 처리하는 suspend 함수
 */
suspend inline fun IntStream.suspendForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Int) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

@Deprecated(
    message = "Use suspendForEach instead",
    replaceWith = ReplaceWith("suspendForEach(coroutineContext, consumer)")
)
suspend inline fun IntStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Int) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

/**
 * [IntStream]을 [Flow] 처럼 Coroutines 환경에서 `map` 처럼 사용합니다.
 *
 * ```
 * val stream = IntStream.range(1, 4)
 * val flow = stream.suspendMap { delay(1); it * 2 }
 * flow.collect { println(it) }  // 2, 4, 6
 * ```
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param transform 요소를 변환하는 suspend 함수
 * @return [Flow] 인스턴스
 */
inline fun <R> IntStream.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Int) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(transform(it)) }
        .collect()
}

@Deprecated(
    message = "Use suspendMap instead",
    replaceWith = ReplaceWith("suspendMap(coroutineContext, transform)")
)
inline fun <R> IntStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Int) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(transform(it)) }
        .collect()
}

internal class IntStreamFlow(private val stream: IntStream): Flow<Int> {
    private val consumed = atomic(false)

    override suspend fun collect(collector: FlowCollector<Int>) {
        if (!consumed.compareAndSet(expect = false, update = true))
            error("IntStream.consumeAsFlow can be collected only once")

        stream.use { stream ->
            for (value in stream.iterator()) {
                collector.emit(value)
            }
        }
    }
}

/**
 * [LongStream]을 [Flow] 처럼 사용합니다.
 */
fun LongStream.consumeAsFlow(): Flow<Long> = LongStreamFlow(this)

/**
 * [LongStream]을 [Flow] 처럼 사용합니다.
 */
fun LongStream.asFlow(): Flow<Long> = consumeAsFlow()

/**
 * [LongStream]을 [Flow] 처럼 Coroutines 환경에서 `forEach` 처럼 사용합니다.
 *
 * ```
 * val stream = LongStream.range(1, 4)
 * stream.suspendForEach { delay(10); println(it) }  // 1, 2, 3
 * ```
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param consumer 요소를 처리하는 suspend 함수
 */
suspend inline fun LongStream.suspendForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Long) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

@Deprecated(
    message = "Use suspendForEach instead",
    replaceWith = ReplaceWith("suspendForEach(coroutineContext, consumer)")
)
suspend inline fun LongStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Long) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

/**
 * [LongStream]을 [Flow] 처럼 Coroutines 환경에서 `map` 처럼 사용합니다.
 *
 * ```
 * val stream = LongStream.range(1, 4)
 * val flow = stream.coMap { delay(1); it * 2 }
 * flow.collect { println(it) }  // 2, 4, 6
 * ```
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param transform 요소를 변환하는 suspend 함수
 * @return [Flow] 인스턴스
 */
inline fun <R> LongStream.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Long) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(transform(it)) }
        .collect()
}

@Deprecated(
    message = "Use suspendMap instead",
    replaceWith = ReplaceWith("suspendMap(coroutineContext, transform)")
)
inline fun <R> LongStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Long) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(transform(it)) }
        .collect()
}

internal class LongStreamFlow(private val stream: LongStream): Flow<Long> {
    private val consumed = AtomicBoolean(false)

    override suspend fun collect(collector: FlowCollector<Long>) {
        if (!consumed.compareAndSet(false, true))
            error("LongStream.consumeAsFlow can be collected only once")

        stream.use { stream ->
            for (value in stream.iterator()) {
                collector.emit(value)
            }
        }
    }
}

/**
 * [DoubleStream]을 [Flow] 로 변환합니다.
 */
fun DoubleStream.consumeAsFlow(): Flow<Double> = DoubleStreamFlow(this)

/**
 * [DoubleStream]을 [Flow] 로 변환합니다.
 */
fun DoubleStream.asFlow(): Flow<Double> = consumeAsFlow()

/**
 * [DoubleStream]을 [Flow] 처럼 Coroutines 환경에서 `forEach` 처럼 사용합니다.
 *
 * ```
 * val stream = DoubleStream.of(1.0, 2.0, 3.0)
 * stream.suspendForEach { delay(10); println(it) }  // 1.0, 2.0, 3.0
 * ```
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param consumer 요소를 처리하는 suspend 함수
 * @receiver [DoubleStream] 인스턴스
 */
suspend inline fun DoubleStream.suspendForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Double) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

@Deprecated(
    message = "Use suspendForEach instead",
    replaceWith = ReplaceWith("suspendForEach(coroutineContext, consumer)")
)
suspend inline fun DoubleStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Double) -> Unit,
) {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { consumer(it) }
        .collect()
}

/**
 * [DoubleStream]을 [Flow] 처럼 Coroutines 환경에서 `map` 처럼 사용합니다.
 *
 * ```
 * val stream = DoubleStream.of(1.0, 2.0, 3.0)
 * val flow = stream.coMap { delay(1); it * 2 }
 * flow.collect { println(it) }  // 2.0, 4.0, 6.0
 * ```
 *
 * @receiver [DoubleStream] 인스턴스
 *
 * @param coroutineContext [CoroutineContext] 인스턴스
 * @param mapper 요소를 변환하는 suspend 함수
 * @receiver [DoubleStream] 인스턴스
 * @return [Flow] 인스턴스
 */
inline fun <R> DoubleStream.suspendMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline mapper: suspend (Double) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(mapper(it)) }
        .collect()
}

@Deprecated(
    message = "Use suspendMap instead",
    replaceWith = ReplaceWith("suspendMap(coroutineContext, mapper)")
)
inline fun <R> DoubleStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline mapper: suspend (Double) -> R,
): Flow<R> = channelFlow {
    consumeAsFlow()
        .buffer()
        .flowOn(coroutineContext)
        .onEach { send(mapper(it)) }
        .collect()
}

internal class DoubleStreamFlow(private val stream: DoubleStream): Flow<Double> {
    private val consumed = AtomicBoolean(false)

    override suspend fun collect(collector: FlowCollector<Double>) {
        if (!consumed.compareAndSet(false, true))
            error("LongStream.consumeAsFlow can be collected only once")

        stream.use { stream ->
            for (value in stream.iterator()) {
                collector.emit(value)
            }
        }
    }
}
