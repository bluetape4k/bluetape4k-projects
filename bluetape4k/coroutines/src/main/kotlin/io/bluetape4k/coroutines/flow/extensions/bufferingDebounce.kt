package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.collections.eclipse.fastListOf
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.whileSelect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * debounced window 내에 발생한 모든 요소를 버퍼링하고, 디바운스 타이머가 만료되면 [List]로 발행합니다.
 *
 * ```
 * val source = flow {
 *    emit(1)
 *    delay(110)
 *    emit(2)
 *    delay(90)
 *    emit(3)
 *    delay(110)
 *    emit(4)
 *    delay(90)
 * }
 * val buffered = source.bufferingDebounce(200.milliseconds)  // [1, 2], [3, 4]
 * ```
 *
 * @param timeout 디바운스 타임아웃
 * @return 디바운스된 [List] 요소를 발행하는 [Flow]
 */
fun <T: Any?> Flow<T>.bufferingDebounce(timeout: Duration): Flow<List<T>> = channelFlow {
    val itemChannel = produceIn(this)
    var bufferedItems = fastListOf<T>()
    var deboundedTimeout = timeout

    whileSelect {
        var prevTimeMs = System.currentTimeMillis()
        if (bufferedItems.isNotEmpty()) {
            onTimeout(deboundedTimeout) {
                send(bufferedItems)
                bufferedItems = fastListOf()
                deboundedTimeout = timeout
                true
            }
        }
        itemChannel.onReceiveCatching { result ->
            val receiveTimeMs = System.currentTimeMillis()
            deboundedTimeout -= (receiveTimeMs - prevTimeMs).milliseconds
            prevTimeMs = receiveTimeMs
            result
                .onSuccess { item -> bufferedItems.add(item) }
                .onFailure { if (bufferedItems.isNotEmpty()) send(bufferedItems) }
                .isSuccess
        }
    }
}
