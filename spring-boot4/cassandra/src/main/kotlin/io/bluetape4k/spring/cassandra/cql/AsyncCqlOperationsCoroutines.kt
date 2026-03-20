package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.future.await
import org.springframework.data.cassandra.core.cql.AsyncCqlOperations
import java.util.concurrent.CompletableFuture

/**
 * CQL 문자열을 실행하고 [AsyncResultSet] 기반 추출 결과를 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `query(cql, extractor, *args).await()`를 호출합니다.
 * - [extractor]가 반환한 [CompletableFuture]가 실패하면 동일 예외가 전파됩니다.
 *
 * ```kotlin
 * val id = asyncOps.querySuspending<Long>("SELECT now() FROM system.local") { rs ->
 *     CompletableFuture.completedFuture(rs.one()?.getInstant(0)?.toEpochMilli())
 * }
 * // result == id
 * ```
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.querySuspending(
    cql: String,
    vararg args: Any,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? =
    query<T>(cql, { extractor(it) }, *args).await()

/**
 * CQL 문자열을 실행하고 [Row] 매퍼로 변환한 결과 목록을 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `query(cql, rowMapper, *args).await()`를 호출합니다.
 * - Cassandra 결과가 비어 있으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val names = asyncOps.querySuspending<String>("SELECT firstname FROM users") { row, _ ->
 *     row.getString("firstname")
 * }
 * // result == names.size
 * ```
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.querySuspending(
    cql: String,
    vararg args: Any,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(
        cql,
        { row, rowNum ->
            rowMapper(row, rowNum)
        },
        *args
    ).await()

/**
 * [Statement]를 실행하고 [AsyncResultSet] 기반 추출 결과를 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `query(statement, extractor).await()`를 호출합니다.
 * - [extractor] 실행 예외는 가공하지 않고 그대로 전파됩니다.
 *
 * ```kotlin
 * val value = asyncOps.querySuspending<String>(statement) { rs ->
 *     CompletableFuture.completedFuture(rs.one()?.getString("firstname"))
 * }
 * // result == value
 * ```
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.querySuspending(
    statement: Statement<*>,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? =
    query<T>(statement) { extractor(it) }.await()

/**
 * [Statement]를 실행하고 [Row] 매퍼로 변환한 결과 목록을 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `query(statement, rowMapper).await()`를 호출합니다.
 * - 반환 리스트의 원소 순서는 Cassandra 드라이버가 전달한 row 순서를 유지합니다.
 *
 * ```kotlin
 * val rows = asyncOps.querySuspending<String>(statement) { row, _ -> row.getString("firstname") }
 * // result == rows.size
 * ```
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.querySuspending(
    statement: Statement<*>,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(statement) { row, rowNum ->
        rowMapper(row, rowNum)
    }.await()
