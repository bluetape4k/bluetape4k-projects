package io.bluetape4k.exposed.r2dbc.redisson.map

import kotlinx.coroutines.future.await
import org.redisson.api.AsyncIterator

/**
 * Redisson [AsyncIterator]를 끝까지 소비해 [List]로 수집합니다.
 *
 * ## 동작/계약
 * - `hasNext().await()`가 `false`가 될 때까지 `next().await()`를 반복 호출합니다.
 * - 전달한 [destination] 리스트를 직접 채우며, 같은 인스턴스를 반환합니다.
 * - iterator가 반환하는 순서를 그대로 보존합니다.
 *
 * ```kotlin
 * val values = iterator.toList()
 * // values.size >= 0
 * ```
 */
suspend fun <T> AsyncIterator<T>.toList(destination: MutableList<T> = mutableListOf()): List<T> {
    while (hasNext().await()) {
        destination.add(next().await())
    }
    return destination
}
