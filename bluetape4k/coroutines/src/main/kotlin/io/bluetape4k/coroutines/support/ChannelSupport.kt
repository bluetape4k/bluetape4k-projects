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
 * 수신된 요소가 연속해서 중복되는 요소는 무시하도록 합니다.
 *
 * ```
 * flowOf(1, 1, 2, 2, 3, 3)
 *    .asChannel()
 *    .distinctUntilChanged()
 *    .collect { println(it) }  // 1, 2, 3
 * ```
 *
 * @param context
 * @return 중복되는 요소를 제거한 [ReceiveChannel]
 */
suspend fun <E> ReceiveChannel<E>.distinctUntilChanged(
    context: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<E> = coroutineScope {
    val self = this@distinctUntilChanged
    produce(context, Channel.BUFFERED) {
        val producer = this
        var prev: E? = null

        self.consumeEach { received ->
            log.trace { "Received: $received" }
            if (received != prev) {
                log.trace { "Send: $received" }
                producer.send(received)
                prev = received
            }
        }
        producer.close()
    }
}

/**
 * 수신된 요소가 연속해서 [equalOperator]에 의해 중복되는 요소는 무시하도록 합니다.
 *
 * ```
 * flowOf(1, 1, 2, 2, 3, 3)
 *    .asChannel()
 *    .distinctUntilChanged { a, b -> a == b }
 *    .collect { println(it) }  // 1, 2, 3
 * ```
 *
 * @param context
 * @param equalOperator 요소들을 비교해서 같은지 판단하도록 한다 (두 요소가 같으면 true를 반환)
 * @return 중복되는 요소를 제거한 [ReceiveChannel]
 */
suspend inline fun <E> ReceiveChannel<E>.distinctUntilChanged(
    context: CoroutineContext = Dispatchers.Default,
    crossinline equalOperator: suspend (E, E) -> Boolean,
): ReceiveChannel<E> = coroutineScope {
    val self = this@distinctUntilChanged
    produce(context, Channel.BUFFERED) {
        val producer = this
        var prev: E = self.receive()
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
 * 수신 받은 요소들을 이용하여 [accumulator]를 통해 reduce 한 값을 send 하는 [ReceiveChannel]을 반환합니다.
 *
 * ```
 * flowOf(1, 2, 3, 4, 5)
 *   .asChannel()
 *   .reduce { acc, item -> acc + item }
 *   .collect { println(it) }  // 15
 * ```
 *
 * @param E 요소의 타입
 * @param context [ReceiveChannel]을 실행할 [CoroutineContext]
 * @param accumulator reduce 하는 함수
 * @return reduced 된 값을 제공하는 [ReceiveChannel]
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
 * 수신 받은 요소들을 이용하여 [accumulator]를 통해 reduce 한 값을 send 하는 [ReceiveChannel]을 반환합니다.
 *
 * ```
 * flowOf(1, 2, 3, 4, 5)
 *   .asChannel()
 *   .reduce(0) { acc, item -> acc + item }
 *   .collect { println(it) }  // 15
 * ```
 *
 * @param E 요소의 타입
 * @param initValue 초기값
 * @param context [ReceiveChannel]을 실행할 [CoroutineContext]
 * @param accumulator reduce 하는 함수
 * @return reduced 된 값을 제공하는 [ReceiveChannel]
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
 * 두 개의 [ReceiveChannel] 에서 수신 받은 것을 교대로 produce 합니다.
 *
 * ```
 * val first = flowOf(1, 3, 5).asChannel()
 * val second = flowOf(2, 4, 6).asChannel()
 * first.concatWith(second)
 *    .collect { println(it) }  // 1, 2, 3, 4, 5, 6
 * ```
 *
 * @param E 요소의 타입
 * @receiver 첫 번째 [ReceiveChannel]
 * @param other 두 번째 [ReceiveChannel]
 * @param context [ReceiveChannel]을 실행할 [CoroutineContext]
 * @return 두 [ReceiveChannel]의 요소를 교대로 제공하는 [ReceiveChannel]
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
 * 두 개의 [ReceiveChannel] 에서 수신 받은 것을 교대로 produce 합니다.
 *
 * ```
 * val first = flowOf(1, 3, 5).asChannel()
 * val second = flowOf(2, 4, 6).asChannel()
 *
 * concat(first, second)
 *   .collect { println(it) }  // 1, 2, 3, 4, 5, 6
 * ```
 *
 * @param E  요소의 타입
 * @param context [ReceiveChannel]을 실행할 [CoroutineContext]
 * @param first  첫 번째 [ReceiveChannel]
 * @param second 두 번째 [ReceiveChannel]
 * @return 두 [ReceiveChannel]의 요소를 교대로 제공하는 [ReceiveChannel]
 */
suspend fun <E> concat(
    first: ReceiveChannel<E>,
    second: ReceiveChannel<E>,
    context: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<E> = first.concatWith(second, context)

/**
 * [waitDuration] 만큼 지연 시키고, 가장 최신의 수신 요소를 전송 합니다.
 *
 * ```
 * flowOf(1, 2, 3, 4, 5)
 *  .asChannel()
 *  .debounce(1.seconds)
 *  .collect { println(it) }  // 5
 * ```
 *
 * @param E  요소의 타입
 * @param waitDuration 지연 시간
 * @param context [ReceiveChannel]을 실행할 [CoroutineContext]
 * @return 지연된 요소를 제공하는 [ReceiveChannel]
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
                delay(minOf(nextTime - currentTime, waitMillis))
                var mostRecent = received
                // channel에 요소가 있다면 가장 최신의 요소를 얻기 위해 계속 수신합니다. (중간 요소들은 모두 무시됩니다)
                while (!self.isEmpty) {
                    self.receiveCatching().getOrNull()?.run { mostRecent = this } ?: break
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
