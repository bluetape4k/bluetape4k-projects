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
 * @param block 각 ResultRow에 대해 실행할 함수
 */
suspend fun Query.forEach(block: (ResultRow) -> Unit) {
    this.collect { row ->
        block(row)
    }
}

/**
 * Query의 각 ResultRow에 인덱스를 포함하여 주어진 블록을 실행합니다.
 *
 * @param block 인덱스와 ResultRow를 인자로 받는 함수
 */
suspend fun Query.forEachIndexed(block: (Int, ResultRow) -> Unit) {
    this.collectIndexed { index, row ->
        block(index, row)
    }
}

/**
 * Flow에 값이 하나라도 존재하는지 여부를 반환합니다.
 *
 * @return 값이 존재하면 true, 아니면 false
 */
suspend fun <T> Flow<T>.any(): Boolean = this.firstOrNull() != null

/**
 * Flow의 모든 요소를 정렬하여 List로 반환합니다.
 *
 * @return 정렬된 List
 */
suspend fun <T: Comparable<T>> Flow<T>.sorted(): List<T> = toList().sorted()

/**
 * Flow의 중복되지 않은 요소만 List로 반환합니다.
 *
 * @return 중복이 제거된 List
 */
suspend fun <T> Flow<T>.distinct(): List<T> =
    toList().distinct()
