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
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [Stream]을 코루틴 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - Java Stream은 단일 소비 모델이므로 같은 Stream 인스턴스를 다시 소비하면 예외가 발생할 수 있습니다.
 * - 수신 객체를 변경하지 않고, 요소를 순회하는 Flow 래퍼를 새로 반환합니다.
 * - 별도 입력 검증은 없으며, 스트림 순회 중 발생한 예외는 그대로 전파됩니다.
 * - 변환 자체는 가볍고, 실제 요소 소비는 Flow 수집 시점에 수행됩니다.
 *
 * ```kotlin
 * val out = Stream.of(1, 2, 3).asFlow().toList()
 * // out == [1, 2, 3]
 * ```
 */
fun <T> Stream<T>.asFlow(): Flow<T> = consumeAsFlow()

/**
 * [Stream] 요소를 suspend 소비 함수로 순차 처리합니다.
 *
 * ## 동작/계약
 * - 수신 Stream을 Flow로 변환한 뒤 buffer/flowOn을 거쳐 각 요소에 [consumer]를 적용합니다.
 * - 수신 Stream 자체를 mutate 하지는 않지만, Stream은 소비되며 재사용할 수 없습니다.
 * - [consumer] 또는 스트림 순회 중 발생한 예외는 호출자에게 전파됩니다.
 * - 내부 버퍼를 사용하므로 처리량 개선을 위해 추가 채널 할당이 발생할 수 있습니다.
 *
 * ```kotlin
 * val out = mutableListOf<Int>()
 * IntStream.of(1, 2, 3).boxed().suspendForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param consumer 각 요소를 처리하는 suspend 콜백입니다.
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
/**
 * [suspendForEach]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendForEach]와 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 [suspendForEach]와 동일합니다.
 *
 * ```kotlin
 * val out = mutableListOf<Int>()
 * IntStream.of(1, 2, 3).boxed().coForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 */
suspend fun <T> Stream<T>.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    consumer: suspend (T) -> Unit,
) = suspendForEach(coroutineContext, consumer)

/**
 * [Stream] 요소를 suspend 변환해 [Flow]로 방출합니다.
 *
 * ## 동작/계약
 * - 각 요소에 [transform]을 적용한 결과를 새로운 Flow로 방출합니다.
 * - 수신 Stream은 소비되며, 수집 완료 후 재사용할 수 없습니다.
 * - [transform] 또는 업스트림 순회 중 예외는 수집자에게 전파됩니다.
 * - channelFlow/buffer를 사용하므로 변환 중 채널 할당이 발생합니다.
 *
 * ```kotlin
 * val out = Stream.of(1, 2, 3).suspendMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param transform 각 요소를 다른 값으로 변환하는 suspend 함수입니다.
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
/**
 * [suspendMap]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendMap]과 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 [suspendMap]과 동일합니다.
 *
 * ```kotlin
 * val out = Stream.of(1, 2, 3).coMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 */
inline fun <T, R> Stream<T>.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (T) -> R,
): Flow<R> = suspendMap(coroutineContext) { transform(it) }

/**
 * [IntStream]을 단일 소비 가능한 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 반환 Flow는 한 번만 수집할 수 있으며, 두 번째 수집 시 예외를 던집니다.
 * - 수신 객체를 변경하지 않고 Int 전용 Flow 래퍼를 생성합니다.
 * - 별도 입력 검증은 없고, 스트림 순회 예외는 그대로 전파됩니다.
 * - Int 박싱 없이 순회하다 emit 시점에 Int 값이 전달됩니다.
 *
 * ```kotlin
 * val out = IntStream.of(1, 2, 3).consumeAsFlow().toList()
 * // out == [1, 2, 3]
 * ```
 */
fun IntStream.consumeAsFlow(): Flow<Int> = IntStreamFlow(this)

/**
 * [IntStream]을 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 동작은 [consumeAsFlow]와 동일합니다.
 * - 반환 Flow는 한 번만 수집할 수 있습니다.
 * - 스트림 순회 중 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = IntStream.of(1, 2, 3).asFlow().toList()
 * // out == [1, 2, 3]
 * ```
 */
fun IntStream.asFlow(): Flow<Int> = consumeAsFlow()

/**
 * [IntStream] 요소를 suspend 소비 함수로 순차 처리합니다.
 *
 * ## 동작/계약
 * - IntStream을 Flow로 변환한 뒤 각 요소에 [consumer]를 호출합니다.
 * - Stream은 소비되며 동일 인스턴스를 다시 사용할 수 없습니다.
 * - [consumer] 예외는 호출자에게 전파됩니다.
 * - buffer 사용으로 추가 채널 할당이 발생할 수 있습니다.
 *
 * ```kotlin
 * val out = mutableListOf<Int>()
 * IntStream.of(1, 2, 3).suspendForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param consumer 각 Int 요소를 처리하는 suspend 콜백입니다.
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
/**
 * [IntStream.suspendForEach]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendForEach]와 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = mutableListOf<Int>()
 * IntStream.of(1, 2, 3).coForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 */
suspend inline fun IntStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Int) -> Unit,
) = suspendForEach(coroutineContext) { consumer(it) }

/**
 * [IntStream] 요소를 suspend 변환해 [Flow]로 방출합니다.
 *
 * ## 동작/계약
 * - 각 Int 요소에 [transform]을 적용한 값을 순서대로 방출합니다.
 * - 수신 Stream은 단일 소비됩니다.
 * - [transform] 예외는 수집자에게 전파됩니다.
 * - channelFlow/buffer 사용으로 변환 시 추가 할당이 발생합니다.
 *
 * ```kotlin
 * val out = IntStream.of(1, 2, 3).suspendMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param transform 각 Int 요소를 다른 값으로 변환하는 suspend 함수입니다.
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
/**
 * [IntStream.suspendMap]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendMap]과 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = IntStream.of(1, 2, 3).coMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 */
inline fun <R> IntStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Int) -> R,
): Flow<R> = suspendMap(coroutineContext) { transform(it) }

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
 * [LongStream]을 단일 소비 가능한 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 반환 Flow는 한 번만 수집할 수 있으며, 두 번째 수집 시 예외를 던집니다.
 * - 수신 객체를 변경하지 않고 Long 전용 Flow 래퍼를 생성합니다.
 * - 별도 입력 검증은 없고, 스트림 순회 예외는 그대로 전파됩니다.
 * - Long 값을 순회해 수집 시점에 방출합니다.
 *
 * ```kotlin
 * val out = LongStream.of(1L, 2L, 3L).consumeAsFlow().toList()
 * // out == [1, 2, 3]
 * ```
 */
fun LongStream.consumeAsFlow(): Flow<Long> = LongStreamFlow(this)

/**
 * [LongStream]을 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 동작은 [consumeAsFlow]와 동일합니다.
 * - 반환 Flow는 한 번만 수집할 수 있습니다.
 * - 스트림 순회 중 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = LongStream.of(1L, 2L, 3L).asFlow().toList()
 * // out == [1, 2, 3]
 * ```
 */
fun LongStream.asFlow(): Flow<Long> = consumeAsFlow()

/**
 * [LongStream] 요소를 suspend 소비 함수로 순차 처리합니다.
 *
 * ## 동작/계약
 * - LongStream을 Flow로 변환한 뒤 각 요소에 [consumer]를 호출합니다.
 * - Stream은 소비되며 동일 인스턴스를 다시 사용할 수 없습니다.
 * - [consumer] 예외는 호출자에게 전파됩니다.
 * - buffer 사용으로 추가 채널 할당이 발생할 수 있습니다.
 *
 * ```kotlin
 * val out = mutableListOf<Long>()
 * LongStream.of(1L, 2L, 3L).suspendForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param consumer 각 Long 요소를 처리하는 suspend 콜백입니다.
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
/**
 * [LongStream.suspendForEach]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendForEach]와 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = mutableListOf<Long>()
 * LongStream.of(1L, 2L, 3L).coForEach { out += it }
 * // out == [1, 2, 3]
 * ```
 */
suspend inline fun LongStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Long) -> Unit,
) = suspendForEach(coroutineContext) { consumer(it) }

/**
 * [LongStream] 요소를 suspend 변환해 [Flow]로 방출합니다.
 *
 * ## 동작/계약
 * - 각 Long 요소에 [transform]을 적용한 값을 순서대로 방출합니다.
 * - 수신 Stream은 단일 소비됩니다.
 * - [transform] 예외는 수집자에게 전파됩니다.
 * - channelFlow/buffer 사용으로 변환 시 추가 할당이 발생합니다.
 *
 * ```kotlin
 * val out = LongStream.of(1L, 2L, 3L).suspendMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param transform 각 Long 요소를 다른 값으로 변환하는 suspend 함수입니다.
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
/**
 * [LongStream.suspendMap]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendMap]과 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = LongStream.of(1L, 2L, 3L).coMap { it * 10 }.toList()
 * // out == [10, 20, 30]
 * ```
 */
inline fun <R> LongStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline transform: suspend (Long) -> R,
): Flow<R> = suspendMap(coroutineContext) { transform(it) }

internal class LongStreamFlow(private val stream: LongStream): Flow<Long> {
    private val consumed = atomic(false)

    override suspend fun collect(collector: FlowCollector<Long>) {
        if (!consumed.compareAndSet(expect = false, update = true))
            error("LongStream.consumeAsFlow can be collected only once")

        stream.use { stream ->
            for (value in stream.iterator()) {
                collector.emit(value)
            }
        }
    }
}

/**
 * [DoubleStream]을 단일 소비 가능한 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 반환 Flow는 한 번만 수집할 수 있으며, 두 번째 수집 시 예외를 던집니다.
 * - 수신 객체를 변경하지 않고 Double 전용 Flow 래퍼를 생성합니다.
 * - 별도 입력 검증은 없고, 스트림 순회 예외는 그대로 전파됩니다.
 * - Double 값을 순회해 수집 시점에 방출합니다.
 *
 * ```kotlin
 * val out = DoubleStream.of(1.0, 2.0, 3.0).consumeAsFlow().toList()
 * // out == [1.0, 2.0, 3.0]
 * ```
 */
fun DoubleStream.consumeAsFlow(): Flow<Double> = DoubleStreamFlow(this)

/**
 * [DoubleStream]을 [Flow]로 변환합니다.
 *
 * ## 동작/계약
 * - 동작은 [consumeAsFlow]와 동일합니다.
 * - 반환 Flow는 한 번만 수집할 수 있습니다.
 * - 스트림 순회 중 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = DoubleStream.of(1.0, 2.0, 3.0).asFlow().toList()
 * // out == [1.0, 2.0, 3.0]
 * ```
 */
fun DoubleStream.asFlow(): Flow<Double> = consumeAsFlow()

/**
 * [DoubleStream] 요소를 suspend 소비 함수로 순차 처리합니다.
 *
 * ## 동작/계약
 * - DoubleStream을 Flow로 변환한 뒤 각 요소에 [consumer]를 호출합니다.
 * - Stream은 소비되며 동일 인스턴스를 다시 사용할 수 없습니다.
 * - [consumer] 예외는 호출자에게 전파됩니다.
 * - buffer 사용으로 추가 채널 할당이 발생할 수 있습니다.
 *
 * ```kotlin
 * val out = mutableListOf<Double>()
 * DoubleStream.of(1.0, 2.0, 3.0).suspendForEach { out += it }
 * // out == [1.0, 2.0, 3.0]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param consumer 각 Double 요소를 처리하는 suspend 콜백입니다.
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
/**
 * [DoubleStream.suspendForEach]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendForEach]와 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = mutableListOf<Double>()
 * DoubleStream.of(1.0, 2.0, 3.0).coForEach { out += it }
 * // out == [1.0, 2.0, 3.0]
 * ```
 */
suspend inline fun DoubleStream.coForEach(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline consumer: suspend (Double) -> Unit,
) = suspendForEach(coroutineContext) { consumer(it) }

/**
 * [DoubleStream] 요소를 suspend 변환해 [Flow]로 방출합니다.
 *
 * ## 동작/계약
 * - 각 Double 요소에 [mapper]를 적용한 값을 순서대로 방출합니다.
 * - 수신 Stream은 단일 소비됩니다.
 * - [mapper] 예외는 수집자에게 전파됩니다.
 * - channelFlow/buffer 사용으로 변환 시 추가 할당이 발생합니다.
 *
 * ```kotlin
 * val out = DoubleStream.of(1.0, 2.0, 3.0).suspendMap { it * 10 }.toList()
 * // out == [10.0, 20.0, 30.0]
 * ```
 * @param coroutineContext 업스트림 Flow 처리에 적용할 CoroutineContext입니다.
 * @param mapper 각 Double 요소를 다른 값으로 변환하는 suspend 함수입니다.
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
/**
 * [DoubleStream.suspendMap]의 이전 이름입니다.
 *
 * ## 동작/계약
 * - 동작은 [suspendMap]과 동일합니다.
 * - 수신 Stream은 한 번 소비됩니다.
 * - 예외 전파 규칙도 동일합니다.
 *
 * ```kotlin
 * val out = DoubleStream.of(1.0, 2.0, 3.0).coMap { it * 10 }.toList()
 * // out == [10.0, 20.0, 30.0]
 * ```
 */
inline fun <R> DoubleStream.coMap(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline mapper: suspend (Double) -> R,
): Flow<R> = suspendMap(coroutineContext) { mapper(it) }

internal class DoubleStreamFlow(private val stream: DoubleStream): Flow<Double> {
    private val consumed = atomic(false)

    override suspend fun collect(collector: FlowCollector<Double>) {
        if (!consumed.compareAndSet(expect = false, update = true))
            error("DoubleStream.consumeAsFlow can be collected only once")

        stream.use { stream ->
            for (value in stream.iterator()) {
                collector.emit(value)
            }
        }
    }
}
