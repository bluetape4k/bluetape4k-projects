package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await

/**
 * [AsyncResultSet]의 현재 Page 뿐 아니라 Next Page들을 계속 읽어서 emit 하는 [Flow]로 반환합니다.
 *
 * ```
 * val result = session.executeAsync("SELECT * FROM table")
 * result.asFlow().collect { row ->
 *    println(row)
 *    // do something
 *    // ...
 * }
 */
fun AsyncResultSet.asFlow(): Flow<Row> = flow {
    var current = this@asFlow
    while (current.remaining() > 0) {
        emitAll(current.currentPage().asFlow())
        if (current.hasMorePages()) {
            current = current.fetchNextPage().await()
        } else {
            break
        }
    }
}

/**
 * [AsyncResultSet]의 현재 Page 뿐 아니라 Next Page들을 계속 읽어서 [mapper]를 이용한 변환한 값을 emit 하는 [Flow]로 반환합니다.
 *
 * ```
 * val result = session.executeAsync("SELECT * FROM table")
 * val entities = result.asFlow { row -> row.toEntity() }.toList()
 * ```
 *
 * @param mapper [Row]를 변환할 함수
 * @return [Flow] 인스턴스
 */
fun <T> AsyncResultSet.asFlow(mapper: suspend (row: Row) -> T): Flow<T> = flow {
    var current = this@asFlow
    while (current.remaining() > 0) {
        emitAll(current.currentPage().asFlow().map(mapper))
        if (current.hasMorePages()) {
            current = current.fetchNextPage().await()
        } else {
            break
        }
    }
}
