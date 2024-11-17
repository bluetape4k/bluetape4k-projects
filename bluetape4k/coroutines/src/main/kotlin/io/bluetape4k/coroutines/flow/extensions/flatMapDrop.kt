package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * 업스트림 항목을 [Flow]로 매핑하고 현재 내부 [Flow]가 완료될 때까지 업스트림 항목을 삭제하면서 해당 항목을 전달합니다.
 *
 * ```
 * flowRangeOf(1, 10)
 *     .onEach { delay(100) }.log("src")
 *     .flatMapDrop {
 *         flowRangeOf(it * 100, 5).onEach { delay(20) }.log("in")
 *     }
 *     .assertResult(
 *         100, 101, 102, 103, 104,
 *         300, 301, 302, 303, 304,
 *         500, 501, 502, 503, 504,
 *         700, 701, 702, 703, 704,
 *         900, 901, 902, 903, 904
 *     )
 * ```
 */
fun <T, R> Flow<T>.flatMapDrop(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    flatMapFirst(transform)
