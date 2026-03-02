package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow

/**
 * Flow 요소를 고정 크기 청크(List)로 묶어 방출합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `windowed(size, size, partialWindow)`를 사용합니다.
 * - `partialWindow=true`면 마지막 불완전 청크도 방출하고, `false`면 버립니다.
 * - `size` 유효성 검증/예외 규칙은 위임 대상인 `windowed` 구현을 따릅니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2, 3, 4, 5).chunked(2).toList()
 * // result == [[1, 2], [3, 4], [5]]
 * ```
 *
 * @param size 청크 크기입니다.
 * @param partialWindow 마지막 불완전 청크 방출 여부입니다.
 */
fun <T> Flow<T>.chunked(size: Int, partialWindow: Boolean = true): Flow<List<T>> =
    windowed(size, size, partialWindow)
