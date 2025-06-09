package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.future.await
import org.springframework.data.cassandra.core.cql.AsyncCqlOperations
import java.util.concurrent.CompletableFuture

/**
 * Coroutine 환경에서 [AsyncCqlOperations]의 `query` 함수를 실행합니다.
 *
 * ```
 * val user = asyncCqlOperations.querySuspending<User>(
 *      "SELECT * FROM users WHERE id = ?",
 *      userId
 * ) { resultSet ->
 *     resultSet.one().map { row -> User(row) }
 * }
 * ```
 *
 * @receiver [AsyncCqlOperations] 인스턴스
 * @param T 반환할 값의 수형
 * @param cql 실행할 CQL 쿼리
 * @param args CQL 쿼리에 전달할 인자
 * @param extractor [AsyncResultSet]을 [T]로 변환하는 함수
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.suspendQuery(
    cql: String,
    vararg args: Any,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? =
    query<T>(cql, { extractor(it) }, *args).await()

@Deprecated(
    message = "Use suspendQuery instead",
    replaceWith = ReplaceWith("suspendQuery(cql, *args, extractor = extractor)"),
)
suspend inline fun <reified T: Any> AsyncCqlOperations.coQuery(
    cql: String,
    vararg args: Any,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? =
    query<T>(cql, { extractor(it) }, *args).await()

/**
 * Coroutine 환경에서 [AsyncCqlOperations]의 `query` 함수를 실행합니다.
 *
 * ```
 * val users = asyncCqlOperations.querySuspending<User>(
 *     "SELECT * FROM users WHERE id = ?",
 *     userId
 * ) { row, rowNum -> User(row) }
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.suspendQuery(
    cql: String,
    vararg args: Any,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(cql, { row, rowNum -> rowMapper(row, rowNum) }, *args).await()


@Deprecated(
    message = "Use suspendQuery instead",
    replaceWith = ReplaceWith("suspendQuery(cql, *args, rowMapper = rowMapper)")
)
suspend inline fun <reified T: Any> AsyncCqlOperations.coQuery(
    cql: String,
    vararg args: Any,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(cql, { row, rowNum -> rowMapper(row, rowNum) }, *args).await()


/**
 * Coroutine 환경에서 [AsyncCqlOperations]의 `query` 함수를 실행합니다.
 *
 * ```
 * val stmt = SimpleStatement.newInstance("SELECT * FROM users WHERE id = ?", userId)
 * val user = asyncCqlOperations.querySuspending<User>(stmt) { resultSet ->
 *     resultSet.one().map { row -> futureOf { User(row) } }
 * }
 * ```
 *
 * @receiver [AsyncCqlOperations] 인스턴스
 * @param T 반환할 값의 수형
 * @param statement 실행할 CQL 쿼리
 * @param extractor [AsyncResultSet]을 [CompletableFuture]`<T>`로 변환하는 함수
 * @return [T] 형식의 결과
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.suspendQuery(
    statement: Statement<*>,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? = query<T>(statement) { extractor(it) }.await()

@Deprecated(
    message = "Use suspendQuery instead",
    replaceWith = ReplaceWith("suspendQuery(statement, extractor = extractor)"),
)
suspend inline fun <reified T: Any> AsyncCqlOperations.coQuery(
    statement: Statement<*>,
    crossinline extractor: (AsyncResultSet) -> CompletableFuture<T?>,
): T? =
    query<T>(statement) { extractor(it) }.await()

/**
 * Coroutine 환경에서 [AsyncCqlOperations]의 `query` 함수를 실행합니다.
 *
 * ```
 * val stmt = SimpleStatement.newInstance("SELECT * FROM users WHERE id = ?", userId)
 * val users = asyncCqlOperations.querySuspending<User>(stmt) { row, rowNum -> User(row) }
 * ```
 *
 * @receiver [AsyncCqlOperations] 인스턴스
 * @param T 반환할 값의 수형
 * @param statement 실행할 CQL 쿼리
 * @param rowMapper [Row]를 [T]로 변환하는 함수
 * @return [T] 수형 객체의 컬렉션
 */
suspend inline fun <reified T: Any> AsyncCqlOperations.suspendQuery(
    statement: Statement<*>,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(statement) { row, rowNum -> rowMapper(row, rowNum) }.await()

@Deprecated(
    message = "Use suspendQuery instead",
    replaceWith = ReplaceWith("suspendQuery(statement, rowMapper = rowMapper)"),
)
suspend inline fun <reified T: Any> AsyncCqlOperations.coQuery(
    statement: Statement<*>,
    crossinline rowMapper: (row: Row, rowNum: Int) -> T,
): List<T> =
    query(statement) { row, rowNum -> rowMapper(row, rowNum) }.await()
