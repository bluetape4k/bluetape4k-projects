package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.whileSelect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 디바운스 구간 동안 들어온 값을 버퍼링해 리스트로 묶어 방출합니다.
 *
 * ## 동작/계약
 * - 채널 수신과 timeout을 `whileSelect`로 경쟁시켜 timeout 시점마다 누적 버퍼를 방출합니다.
 * - 입력이 연속으로 들어오면 남은 timeout을 줄여 같은 배치에 계속 누적합니다.
 * - source 완료 시 버퍼에 남은 값이 있으면 마지막으로 1회 방출합니다.
 * - 각 배치마다 새 `MutableList`를 생성합니다.
 *
 * ```kotlin
 * val batches = source.bufferingDebounce(200.milliseconds).toList()
 * // batches는 디바운스 구간별 묶음 리스트
 * ```
 *
 * @param timeout 배치를 끊을 디바운스 시간입니다.
 */
fun <T> Flow<T>.bufferingDebounce(timeout: Duration): Flow<List<T>> = flow {
    coroutineScope {
        val itemChannel = this@bufferingDebounce.produceIn(this)
        try {
            var bufferedItems = mutableListOf<T>()
            var deboundedTimeout = timeout

            whileSelect {
                var prevTimeMs = System.currentTimeMillis()
                if (bufferedItems.isNotEmpty()) {
                    onTimeout(deboundedTimeout) {
                        emit(bufferedItems)
                        bufferedItems = mutableListOf()
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
                        .onFailure { if (bufferedItems.isNotEmpty()) emit(bufferedItems) }
                        .isSuccess
                }
            }
        } finally {
            itemChannel.cancel()
        }
    }
}
