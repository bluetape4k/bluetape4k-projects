package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
fun AsyncResultSet.asFlow(): Flow<Row> = asFlow { it }
//    channelFlow {
//        var current = this@asFlow
//        while (current.remaining() > 0) {
//            // emitAll(current.currentPage().asFlow())
//            current.currentPage().forEach { row ->
//                send(row)
//            }
//            if (current.hasMorePages()) {
//                current = current.fetchNextPage().await()
//            } else {
//                break
//            }
//        }
//    }

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
inline fun <T> AsyncResultSet.asFlow(
    @BuilderInference crossinline mapper: suspend (row: Row) -> T,
): Flow<T> = channelFlow {
    var current = this@asFlow
    while (current.remaining() > 0) {
        current.currentPage()
            .forEach { row ->
                send(mapper(row))
            }
        if (current.hasMorePages()) {
            current = current.fetchNextPage().await()
        } else {
            break
        }
    }
}
