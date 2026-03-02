package io.bluetape4k.exposed.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.r2dbc.Query

/**
 * Query의 각 ResultRow에 대해 주어진 블록을 실행합니다.
 *
 * ## 동작/계약
 * - Query가 방출하는 row를 순서대로 collect하여 [block]에 전달합니다.
 * - row를 별도 버퍼에 저장하지 않고 즉시 소비합니다.
 *
 * ```kotlin
 * var count = 0
 * query.forEach { count++ }
 * // count == expectedRowCount
 * ```
 *
 * @param block 각 ResultRow에 대해 실행할 함수입니다.
 */
suspend fun Query.forEach(block: (ResultRow) -> Unit) {
    this.collect { row ->
        block(row)
    }
}

/**
 * Query의 각 ResultRow에 인덱스를 포함하여 주어진 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 첫 row 인덱스는 0부터 시작합니다.
 * - collect 순서를 유지한 채 `(index, row)`를 [block]에 전달합니다.
 *
 * ```kotlin
 * val indices = mutableListOf<Int>()
 * query.forEachIndexed { index, _ -> indices += index }
 * // indices.firstOrNull() == 0
 * ```
 *
 * @param block 인덱스와 ResultRow를 인자로 받는 함수입니다.
 */
suspend fun Query.forEachIndexed(block: (Int, ResultRow) -> Unit) {
    this.collectIndexed { index, row ->
        block(index, row)
    }
}

/**
 * Flow에 값이 하나라도 존재하는지 여부를 반환합니다.
 *
 * ## 동작/계약
 * - 첫 원소가 존재하면 즉시 `true`를 반환하고 수집을 종료합니다.
 * - 원소가 없으면 `false`를 반환합니다.
 *
 * ```kotlin
 * val hasAny = flowOf(1, 2, 3).any()
 * // hasAny == true
 * ```
 */
suspend fun <T> Flow<T>.any(): Boolean = this.firstOrNull() != null

/**
 * Flow의 모든 요소를 정렬하여 List로 반환합니다.
 *
 * ## 동작/계약
 * - Flow 전체를 메모리에 수집한 뒤 `sorted()`를 적용합니다.
 * - 비교 가능한 타입([Comparable])에 대해서만 동작합니다.
 *
 * ```kotlin
 * val values = flowOf(3, 1, 2).sorted()
 * // values == listOf(1, 2, 3)
 * ```
 */
suspend fun <T: Comparable<T>> Flow<T>.sorted(): List<T> = toList().sorted()

/**
 * Flow의 중복되지 않은 요소만 List로 반환합니다.
 *
 * ## 동작/계약
 * - Flow 전체를 수집한 뒤 `distinct()`를 적용합니다.
 * - 원소의 첫 등장 순서를 유지합니다.
 *
 * ```kotlin
 * val values = flowOf(1, 1, 2, 1).distinct()
 * // values == listOf(1, 2)
 * ```
 */
suspend fun <T> Flow<T>.distinct(): List<T> =
    toList().distinct()
