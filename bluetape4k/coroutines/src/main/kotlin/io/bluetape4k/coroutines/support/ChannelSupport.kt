package io.bluetape4k.coroutines.support

import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * 연속 중복 값을 제거한 `ReceiveChannel`을 생성합니다.
 *
 * ## 동작/계약
 * - 인접한 두 값이 `!=` 비교에서 같은 경우 뒤 값을 건너뜁니다.
 * - 원본 채널을 mutate 하지 않고, `produce`로 새 채널을 반환합니다.
 * - 첫 값이 `null`이어도 정상적으로 첫 항목으로 전달됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 1, 2, 2, 3).asChannel().distinctUntilChanged()
 * // out == [1, 2, 3]
 * ```
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 */
suspend fun <E> ReceiveChannel<E>.distinctUntilChanged(
    context: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<E> = coroutineScope {
    val self = this@distinctUntilChanged
    produce(context, Channel.BUFFERED) {
        val producer = this
        var hasPrev = false
        var prev: E? = null

        self.consumeEach { received ->
            log.trace { "Received: $received" }
            // 첫 값이 null 일때 누락되던 문제 수정
            if (!hasPrev || received != prev) {
                log.trace { "Send: $received" }
                producer.send(received)
                prev = received
                hasPrev = true
            }
        }
        producer.close()
    }
}

/**
 * 사용자 비교 함수로 연속 중복 값을 제거한 `ReceiveChannel`을 생성합니다.
 *
 * ## 동작/계약
 * - 첫 원소는 항상 전달하고, 이후에는 `equalOperator(received, prev)`가 `false`일 때만 전달합니다.
 * - 원본 채널을 mutate 하지 않고 새 채널을 생성해 반환합니다.
 * - 입력 채널이 비어 있으면 빈 채널을 반환합니다.
 *
 * ```kotlin
 * val out = flowOf("A", "a", "B").asChannel().distinctUntilChanged { a, b -> a.equals(b, true) }
 * // out == ["A", "B"]
 * ```
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 * @param equalOperator 연속 두 값을 동일 항목으로 간주할지 결정하는 비교 함수입니다.
 */
suspend inline fun <E> ReceiveChannel<E>.distinctUntilChanged(
    context: CoroutineContext = Dispatchers.Default,
    crossinline equalOperator: suspend (E, E) -> Boolean,
): ReceiveChannel<E> = coroutineScope {
    val self = this@distinctUntilChanged
    produce(context, Channel.BUFFERED) {
        val producer = this
        val first = self.receiveCatching().getOrNull() ?: run {
            producer.close()
            return@produce
        }
        var prev: E = first
        producer.send(prev)

        self.consumeEach { received ->
            log.trace { "Received: $received" }
            if (!equalOperator(received, prev)) {
                log.debug { "Send: $received" }
                producer.send(received)
                prev = received
            }
        }
        producer.close()
    }
}

/**
 * 채널의 모든 원소를 누적해 단일 결과를 방출하는 채널을 생성합니다.
 *
 * ## 동작/계약
 * - 첫 원소를 초기 누적값으로 사용하고, 이후 원소에 `accumulator`를 반복 적용합니다.
 * - 입력 채널이 비어 있으면 `receive()`에서 실패할 수 있습니다.
 * - 결과 채널에는 최종 누적값 1개만 전달됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).asChannel().reduce { acc, v -> acc + v }
 * // out == [6]
 * ```
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 * @param accumulator 누적 함수입니다.
 */
suspend inline fun <E> ReceiveChannel<E>.reduce(
    context: CoroutineContext = Dispatchers.Default,
    crossinline accumulator: suspend (acc: E, item: E) -> E,
): ReceiveChannel<E> = coroutineScope {
    produce(context, Channel.BUFFERED) {
        var acc = receive()
        consumeEach { received ->
            acc = accumulator(acc, received)
        }
        send(acc)
    }
}

/**
 * 초기값을 사용해 채널 원소를 누적하고 단일 결과를 방출하는 채널을 생성합니다.
 *
 * ## 동작/계약
 * - `initValue`에서 시작해 모든 원소에 `accumulator`를 적용합니다.
 * - 입력 채널이 비어 있어도 `initValue`가 최종 결과로 전달됩니다.
 * - 결과 채널에는 최종 누적값 1개만 전달됩니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).asChannel().reduce(0) { acc, v -> acc + v }
 * // out == [6]
 * ```
 * @param initValue 초기 누적값입니다.
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 * @param accumulator 누적 함수입니다.
 */
suspend inline fun <E> ReceiveChannel<E>.reduce(
    initValue: E,
    context: CoroutineContext = Dispatchers.Default,
    crossinline accumulator: suspend (acc: E, item: E) -> E,
): ReceiveChannel<E> = coroutineScope {
    produce(context, Channel.BUFFERED) {
        var acc = initValue
        consumeEach { received ->
            acc = accumulator(acc, received)
        }
        send(acc)
    }
}

/**
 * 현재 채널 뒤에 `other` 채널을 이어 붙인 새 채널을 생성합니다.
 *
 * ## 동작/계약
 * - 먼저 수신 채널의 모든 항목을 전달한 뒤 `other` 항목을 전달합니다.
 * - 두 채널의 원소를 병합하지 않고 순차적으로 연결합니다.
 * - 원본 채널은 소비되며 결과는 새 채널로 제공합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).asChannel().concatWith(flowOf(3, 4).asChannel())
 * // out == [1, 2, 3, 4]
 * ```
 * @param other 뒤에 연결할 채널입니다.
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 */
suspend fun <E> ReceiveChannel<E>.concatWith(
    other: ReceiveChannel<E>,
    context: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<E> = coroutineScope {
    produce(context, Channel.BUFFERED) {
        consumeEach { send(it) }
        other.consumeEach { send(it) }
    }
}

/**
 * 두 채널을 순서대로 연결한 새 채널을 생성합니다.
 *
 * ## 동작/계약
 * - `first`를 모두 전달한 뒤 `second`를 전달합니다.
 * - 내부적으로 `first.concatWith(second, context)`를 호출합니다.
 * - 결과는 새 채널로 제공됩니다.
 *
 * ```kotlin
 * val out = concat(flowOf(1, 2).asChannel(), flowOf(3, 4).asChannel())
 * // out == [1, 2, 3, 4]
 * ```
 * @param first 앞쪽 채널입니다.
 * @param second 뒤쪽 채널입니다.
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 */
suspend fun <E> concat(
    first: ReceiveChannel<E>,
    second: ReceiveChannel<E>,
    context: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<E> = first.concatWith(second, context)

/**
 * 지정한 지연 구간에서 마지막 원소만 전달하도록 디바운스 채널을 생성합니다.
 *
 * ## 동작/계약
 * - `waitDuration`이 음수면 `require` 검증으로 예외가 발생합니다.
 * - 지연 구간 내에 여러 값이 들어오면 중간 값은 버리고 가장 최신 값만 전달합니다.
 * - 원본 채널을 소비하고 결과는 새 채널로 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3).asChannel().debounce(1.seconds)
 * // out == [3]
 * ```
 * @param waitDuration 디바운스 지연 시간입니다. 0 이상이어야 합니다.
 * @param context 내부 producer를 실행할 코루틴 컨텍스트입니다.
 */
suspend fun <E> ReceiveChannel<E>.debounce(
    waitDuration: Duration,
    context: CoroutineContext = EmptyCoroutineContext,
): ReceiveChannel<E> = coroutineScope {
    val self = this@debounce
    require(!waitDuration.isNegative()) { "waitDuration must be zero or positive value." }

    produce(context, Channel.BUFFERED) {
        val producer = this@produce
        val waitMillis = waitDuration.inWholeMilliseconds
        var nextTime = 0L
        self.consumeEach { received ->
            val currentTime = System.currentTimeMillis()
            if (currentTime < nextTime) {
                // 지연시키기
                delay(timeMillis = minOf(nextTime - currentTime, waitMillis))
                var mostRecent = received
                // channel에 요소가 있다면 가장 최신의 요소를 얻기 위해 계속 수신합니다. (중간 요소들은 모두 무시됩니다)
                while (true) {
                    val next = self.tryReceive().getOrNull() ?: break
                    mostRecent = next
                }
                nextTime += waitMillis
                producer.send(mostRecent)
            } else {
                nextTime = currentTime + waitMillis
                producer.send(received)
            }
        }
        producer.close()
    }
}
