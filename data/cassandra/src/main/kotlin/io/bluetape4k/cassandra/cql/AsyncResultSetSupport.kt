package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await

/**
 * [AsyncResultSet]의 모든 페이지를 순회하는 [Flow]를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [asFlow]의 mapper 버전을 `it` 매퍼로 호출합니다.
 * - 현재 페이지부터 `hasMorePages()`가 `false`가 될 때까지 순차 fetch 합니다.
 * - 페이지 fetch 중 예외가 발생하면 flow 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val rows = resultSet.asFlow().toList()
 * // rows.size >= 0
 * ```
 */
fun AsyncResultSet.asFlow(): Flow<Row> = asFlow { it }

/**
 * [AsyncResultSet]의 모든 페이지를 순회하며 각 [Row]를 매핑해 방출합니다.
 *
 * ## 동작/계약
 * - 현재 페이지를 모두 emit한 뒤 다음 페이지를 `fetchNextPage().await()`로 가져옵니다.
 * - [mapper]는 각 row마다 1회 호출되며 mapper 예외는 즉시 수집자로 전파됩니다.
 * - 페이지 수와 row 수에 비례해 동작하며 중간 컬렉션을 별도로 만들지 않습니다.
 *
 * ```kotlin
 * val ids = resultSet.asFlow { row -> row.getLong("id") }.toList()
 * // ids.all { it > 0L } == true
 * ```
 */
inline fun <T> AsyncResultSet.asFlow(crossinline mapper: suspend (row: Row) -> T): Flow<T> = flow {
    var page = this@asFlow
    while (true) {
        for (row in page.currentPage()) {
            emit(mapper(row))
        }
        if (!page.hasMorePages()) break
        page = page.fetchNextPage().await()
    }
}
