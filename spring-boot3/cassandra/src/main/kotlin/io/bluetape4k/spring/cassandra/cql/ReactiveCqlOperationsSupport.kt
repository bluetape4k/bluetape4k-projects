package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession
import org.springframework.data.cassandra.core.cql.PreparedStatementBinder
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations
import org.springframework.data.cassandra.core.cql.ReactivePreparedStatementCreator
import org.springframework.data.cassandra.core.cql.ReactiveSessionCallback
import org.springframework.data.cassandra.core.cql.queryForFlux
import org.springframework.data.cassandra.core.cql.queryForObject

/**
 * [ReactiveCqlOperations]의 비동기 함수를 코루틴 환경에서 사용할 수 있도록 확장합니다.
 * [ReactiveCqlOperations.execute] 를 호출하면 [Flow]를 반환합니다.
 *
 * ```
 * val user: Flow<User> = reactiveCqlOperations.executeSuspending<User>(
 *     "SELECT * FROM users WHERE id = ?",
 *     userId
 * ) { resultSet ->
 *    resultSet.map { row -> User(row) }
 * }
 * ```
 *
 * @receiver [ReactiveCqlOperations] 인스턴스
 * @param T 반환할 값의 수형
 * @param action [ReactiveSession]을 받아 [Flow]를 반환하는 함수
 * @return [Flow] 형식의 결과
 */
fun <T: Any> ReactiveCqlOperations.executeSuspending(action: (ReactiveSession) -> Flow<T>): Flow<T> =
    execute(ReactiveSessionCallback { session -> action(session).asPublisher() }).asFlow()

/**
 * CQL 문자열을 실행하고 적용 여부를 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(cql).awaitSingleOrNull()`을 호출합니다.
 * - 발행 값이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val applied = reactiveCqlOperations.executeSuspending("TRUNCATE users")
 * // result == applied
 * ```
 */
suspend fun ReactiveCqlOperations.executeSuspending(cql: String): Boolean? = execute(cql).awaitSingleOrNull()

/**
 * [ReactivePreparedStatementCreator]를 실행하고 적용 여부를 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(psc).awaitSingleOrNull()`을 호출합니다.
 * - 발행 값이 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val applied = reactiveCqlOperations.coExecute(psc)
 * // result == applied
 * ```
 */
suspend fun ReactiveCqlOperations.coExecute(psc: ReactivePreparedStatementCreator): Boolean? =
    execute(psc).awaitSingleOrNull()

/**
 * CQL 문자열과 바인딩 인자 [Flow]를 실행하고 적용 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - `args()`가 반환한 `Flow<Array<Any?>>`를 Publisher로 변환해 `execute(cql, ...)`에 전달합니다.
 * - 결과 Flow는 각 실행의 적용 여부(Boolean?)를 순서대로 발행합니다.
 *
 * ```kotlin
 * val result = reactiveCqlOperations.executeSuspending(cql) { flowOf(arrayOf("user-1")) }
 * // result == result
 * ```
 */
fun ReactiveCqlOperations.executeSuspending(
    cql: String,
    args: () -> Flow<Array<Any?>>,
): Flow<Boolean?> = execute(cql, args().asPublisher()).asFlow()

/**
 * CQL 문자열 실행 결과를 단건으로 매핑해 반환하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForObject(cql, rowMapper, *args).awaitSingleOrNull()`을 호출합니다.
 * - [rowMapper] 예외와 조회 예외는 가공 없이 전파됩니다.
 *
 * ```kotlin
 * val firstName = reactiveCqlOperations.queryForObjectSuspending(
 *     "SELECT firstname FROM users WHERE id = ?",
 *     "user-1"
 * ) { row, _ -> row.getString("firstname") }
 * // result == firstName
 * ```
 */
suspend fun <T: Any> ReactiveCqlOperations.queryForObjectSuspending(
    cql: String,
    vararg args: Any?,
    rowMapper: (Row, Int) -> T?,
): T? = queryForObject(cql, rowMapper, *args).awaitSingleOrNull()

/**
 * CQL 문자열 실행 결과를 타입 기반 단건 매핑으로 반환하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForObject<T>(cql, *args).awaitSingleOrNull()`을 호출합니다.
 * - 매핑 예외는 수집 시점에 그대로 전파됩니다.
 *
 * ```kotlin
 * val user = reactiveCqlOperations.queryForObjectSuspending<User>("SELECT * FROM users WHERE id = ?", "user-1")
 * // result == user
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCqlOperations.queryForObjectSuspending(
    cql: String,
    vararg args: Any,
): T? = queryForObject<T>(cql, *args).awaitSingleOrNull()

/**
 * [Statement] 실행 결과를 타입 기반 단건 매핑으로 반환하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForObject<T>(statement).awaitSingleOrNull()`을 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = reactiveCqlOperations.queryForObjectSuspending<User>(statement)
 * // result == user
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCqlOperations.queryForObjectSuspending(statement: Statement<*>): T? =
    queryForObject<T>(statement).awaitSingleOrNull()

/**
 * CQL 문자열 실행 결과를 단일 행 맵으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForMap(cql, args).awaitSingle()`을 호출합니다.
 * - 결과가 없거나 다중 행인 경우 예외 여부는 Spring Data Cassandra 규칙을 따릅니다.
 *
 * ```kotlin
 * val row = reactiveCqlOperations.queryForMapSuspending("SELECT * FROM users WHERE id = ?", "user-1")
 * // result == row["id"]
 * ```
 */
suspend fun ReactiveCqlOperations.queryForMapSuspending(
    cql: String,
    vararg args: Any,
): Map<String, Any?> = queryForMap(cql, args).awaitSingle()

/**
 * CQL 문자열 실행 결과를 지정 타입 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForFlux<T>(cql, *args).asFlow()`를 호출합니다.
 * - 결과가 없으면 빈 Flow를 반환합니다.
 *
 * ```kotlin
 * val users = reactiveCqlOperations.queryForFlow<User>("SELECT * FROM users")
 * // result == users
 * ```
 */
inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    vararg args: Any,
): Flow<T> = queryForFlux<T>(cql, *args).asFlow()

/**
 * CQL 문자열 실행 결과를 `Map<String, Any?>` [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForFlux(cql, *args).asFlow()`를 호출합니다.
 * - 각 요소는 컬럼명 기준 key를 가진 맵입니다.
 *
 * ```kotlin
 * val rows = reactiveCqlOperations.queryForMapFlow("SELECT * FROM users")
 * // result == rows
 * ```
 */
fun ReactiveCqlOperations.queryForMapFlow(
    cql: String,
    vararg args: Any,
): Flow<Map<String, Any?>> = queryForFlux(cql, *args).asFlow()

/**
 * CQL 문자열 실행 결과를 [ReactiveResultSet]으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `queryForResultSet(cql, *args).awaitSingle()`을 호출합니다.
 * - ResultSet 발행 실패 시 예외가 전파됩니다.
 *
 * ```kotlin
 * val rs = reactiveCqlOperations.queryForResultSetSuspending("SELECT * FROM users")
 * // result == rs.availableRows()
 * ```
 */
suspend fun ReactiveCqlOperations.queryForResultSetSuspending(
    cql: String,
    vararg args: Any,
): ReactiveResultSet = queryForResultSet(cql, *args).awaitSingle()

fun ReactiveCqlOperations.queryForRowsFlow(statement: Statement<*>): Flow<Row> = queryForRows(statement).asFlow()

fun ReactiveCqlOperations.queryForRowsFlow(
    cql: String,
    vararg args: Any,
): Flow<Row> = queryForRows(cql, *args).asFlow()

fun ReactiveCqlOperations.executeForFlow(statementFlow: Flow<String>): Flow<Boolean> =
    execute(statementFlow.asPublisher()).asFlow()

suspend fun ReactiveCqlOperations.executeSuspending(statement: Statement<*>): Boolean = execute(statement).awaitSingle()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    statement: Statement<*>,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> = query(statement) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    statement: Statement<*>,
    rowMapper: (Row, Int) -> T,
): Flow<T> = query(statement) { row, rowNum -> rowMapper(row, rowNum) }.asFlow()

suspend fun ReactiveCqlOperations.queryForMapSuspending(statement: Statement<*>): Map<String, Any?> =
    queryForMap(statement).awaitSingle()

inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>): Flow<T> =
    queryForFlux<T>(statement).asFlow()

fun ReactiveCqlOperations.queryForMapFlow(statement: Statement<*>): Flow<Map<String, Any?>> =
    queryForFlux(statement).asFlow()

suspend fun ReactiveCqlOperations.queryForResultSetSuspending(statement: Statement<*>): ReactiveResultSet =
    queryForResultSet(statement).awaitSingle()

fun <T: Any> ReactiveCqlOperations.executeForFlow(
    psc: ReactivePreparedStatementCreator,
    action: (ReactiveSession, PreparedStatement) -> Flow<T>,
): Flow<T> = execute(psc) { rs, ps -> action(rs, ps).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.executeForFlow(
    cql: String,
    action: (ReactiveSession, PreparedStatement) -> Flow<T>,
): Flow<T> = execute(cql) { rs, ps -> action(rs, ps).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    vararg args: Any?,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> = query(cql, { rs -> rse(rs).asPublisher() }, *args).asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    vararg args: Any,
    rowMapper: (Row, Int) -> T,
): Flow<T> = query(cql, rowMapper, *args).asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> = query(psc) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    psb: PreparedStatementBinder? = null,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> = query(cql, psb) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    psb: PreparedStatementBinder? = null,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> = query(psc, psb) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    rowMapper: (row: Row, rowNum: Int) -> T,
): Flow<T> = query(psc, rowMapper).asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    psb: PreparedStatementBinder? = null,
    rowMapper: (row: Row, rowNum: Int) -> T,
): Flow<T> = query(psc, psb, rowMapper).asFlow()
