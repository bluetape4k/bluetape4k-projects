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

@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(action)")
)
fun <T: Any> ReactiveCqlOperations.suspendExecute(action: (ReactiveSession) -> Flow<T>): Flow<T> =
    executeSuspending(action)

suspend fun ReactiveCqlOperations.executeSuspending(cql: String): Boolean? =
    execute(cql).awaitSingleOrNull()

@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(cql)")
)
suspend fun ReactiveCqlOperations.suspendExecute(cql: String): Boolean? =
    executeSuspending(cql)

suspend fun ReactiveCqlOperations.coExecute(psc: ReactivePreparedStatementCreator): Boolean? =
    execute(psc).awaitSingleOrNull()

fun ReactiveCqlOperations.executeSuspending(cql: String, args: () -> Flow<Array<Any?>>): Flow<Boolean?> =
    execute(cql, args().asPublisher()).asFlow()

@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(cql, args)")
)
fun ReactiveCqlOperations.suspendExecute(cql: String, args: () -> Flow<Array<Any?>>): Flow<Boolean?> =
    executeSuspending(cql, args)

suspend fun <T: Any> ReactiveCqlOperations.queryForObjectSuspending(
    cql: String,
    vararg args: Any?,
    rowMapper: (Row, Int) -> T?,
): T? {
    return queryForObject(cql, rowMapper, *args).awaitSingleOrNull()
}

suspend inline fun <reified T: Any> ReactiveCqlOperations.queryForObjectSuspending(
    cql: String,
    vararg args: Any,
): T? = queryForObject<T>(cql, *args).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCqlOperations.queryForObjectSuspending(statement: Statement<*>): T? =
    queryForObject<T>(statement).awaitSingleOrNull()

@Deprecated(
    message = "queryForObjectSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForObjectSuspending(cql, *args, rowMapper)")
)
suspend fun <T: Any> ReactiveCqlOperations.suspendQueryForObject(
    cql: String,
    vararg args: Any?,
    rowMapper: (Row, Int) -> T?,
): T? = queryForObjectSuspending(cql, *args, rowMapper = rowMapper)

@Deprecated(
    message = "queryForObjectSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForObjectSuspending(cql, *args)")
)
suspend inline fun <reified T: Any> ReactiveCqlOperations.suspendQueryForObject(cql: String, vararg args: Any): T? =
    queryForObjectSuspending(cql, *args)

@Deprecated(
    message = "queryForObjectSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForObjectSuspending(statement)")
)
suspend inline fun <reified T: Any> ReactiveCqlOperations.suspendQueryForObject(statement: Statement<*>): T? =
    queryForObjectSuspending(statement)

suspend fun ReactiveCqlOperations.queryForMapSuspending(cql: String, vararg args: Any): Map<String, Any?> =
    queryForMap(cql, args).awaitSingle()

@Deprecated(
    message = "queryForMapSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForMapSuspending(cql, *args)")
)
suspend fun ReactiveCqlOperations.suspendQueryForMap(cql: String, vararg args: Any): Map<String, Any?> =
    queryForMapSuspending(cql, *args)

inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(cql: String, vararg args: Any): Flow<T> =
    queryForFlux<T>(cql, *args).asFlow()

fun ReactiveCqlOperations.queryForMapFlow(cql: String, vararg args: Any): Flow<Map<String, Any?>> =
    queryForFlux(cql, *args).asFlow()

suspend fun ReactiveCqlOperations.queryForResultSetSuspending(cql: String, vararg args: Any): ReactiveResultSet =
    queryForResultSet(cql, *args).awaitSingle()

@Deprecated(
    message = "queryForResultSetSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForResultSetSuspending(cql, *args)")
)
suspend fun ReactiveCqlOperations.suspendQueryForResultSet(cql: String, vararg args: Any): ReactiveResultSet =
    queryForResultSetSuspending(cql, *args)

fun ReactiveCqlOperations.queryForRowsFlow(statement: Statement<*>): Flow<Row> =
    queryForRows(statement).asFlow()

fun ReactiveCqlOperations.queryForRowsFlow(cql: String, vararg args: Any): Flow<Row> =
    queryForRows(cql, *args).asFlow()


fun ReactiveCqlOperations.executeForFlow(statementFlow: Flow<String>): Flow<Boolean> =
    execute(statementFlow.asPublisher()).asFlow()

suspend fun ReactiveCqlOperations.executeSuspending(statement: Statement<*>): Boolean =
    execute(statement).awaitSingle()

@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(statement)")
)
suspend fun ReactiveCqlOperations.suspendExecute(statement: Statement<*>): Boolean =
    executeSuspending(statement)

fun <T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>, rse: (ReactiveResultSet) -> Flow<T>): Flow<T> =
    query(statement) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>, rowMapper: (Row, Int) -> T): Flow<T> =
    query(statement) { row, rowNum -> rowMapper(row, rowNum) }.asFlow()

suspend fun ReactiveCqlOperations.queryForMapSuspending(statement: Statement<*>): Map<String, Any?> =
    queryForMap(statement).awaitSingle()

@Deprecated(
    message = "queryForMapSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForMapSuspending(statement)")
)
suspend fun ReactiveCqlOperations.suspendQueryForMap(statement: Statement<*>): Map<String, Any?> =
    queryForMapSuspending(statement)

inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>): Flow<T> =
    queryForFlux<T>(statement).asFlow()

fun ReactiveCqlOperations.queryForMapFlow(statement: Statement<*>): Flow<Map<String, Any?>> =
    queryForFlux(statement).asFlow()

suspend fun ReactiveCqlOperations.queryForResultSetSuspending(statement: Statement<*>): ReactiveResultSet =
    queryForResultSet(statement).awaitSingle()

@Deprecated(
    message = "queryForResultSetSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("queryForResultSetSuspending(statement)")
)
suspend fun ReactiveCqlOperations.suspendQueryForResultSet(statement: Statement<*>): ReactiveResultSet =
    queryForResultSetSuspending(statement)

fun <T: Any> ReactiveCqlOperations.executeForFlow(
    psc: ReactivePreparedStatementCreator,
    action: (ReactiveSession, PreparedStatement) -> Flow<T>,
): Flow<T> =
    execute(psc) { rs, ps -> action(rs, ps).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.executeForFlow(
    cql: String,
    action: (ReactiveSession, PreparedStatement) -> Flow<T>,
): Flow<T> =
    execute(cql) { rs, ps -> action(rs, ps).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    vararg args: Any?,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> =
    query(cql, { rs -> rse(rs).asPublisher() }, *args).asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    vararg args: Any,
    rowMapper: (Row, Int) -> T,
): Flow<T> =
    query(cql, rowMapper, *args).asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> =
    query(psc) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    cql: String,
    psb: PreparedStatementBinder? = null,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> =
    query(cql, psb) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    psb: PreparedStatementBinder? = null,
    rse: (ReactiveResultSet) -> Flow<T>,
): Flow<T> =
    query(psc, psb) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    rowMapper: (row: Row, rowNum: Int) -> T,
): Flow<T> =
    query(psc, rowMapper).asFlow()


fun <T: Any> ReactiveCqlOperations.queryForFlow(
    psc: ReactivePreparedStatementCreator,
    psb: PreparedStatementBinder? = null,
    rowMapper: (row: Row, rowNum: Int) -> T,
): Flow<T> =
    query(psc, psb, rowMapper).asFlow()
