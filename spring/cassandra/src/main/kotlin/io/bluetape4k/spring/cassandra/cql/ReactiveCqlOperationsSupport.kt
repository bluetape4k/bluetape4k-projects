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
fun <T: Any> ReactiveCqlOperations.coExecute(action: (ReactiveSession) -> Flow<T>): Flow<T> =
    execute(ReactiveSessionCallback { session -> action(session).asPublisher() }).asFlow()

suspend fun ReactiveCqlOperations.coExecute(cql: String): Boolean? =
    execute(cql).awaitSingleOrNull()

suspend fun ReactiveCqlOperations.coExecute(psc: ReactivePreparedStatementCreator): Boolean? =
    execute(psc).awaitSingleOrNull()

fun ReactiveCqlOperations.coExecute(cql: String, args: () -> Flow<Array<Any?>>): Flow<Boolean?> =
    execute(cql, args().asPublisher()).asFlow()

suspend fun <T: Any> ReactiveCqlOperations.coQueryForObject(
    cql: String,
    vararg args: Any?,
    rowMapper: (Row, Int) -> T?,
): T? {
    return queryForObject(cql, rowMapper, *args).awaitSingleOrNull()
}

suspend inline fun <reified T: Any> ReactiveCqlOperations.coQueryForObject(cql: String, vararg args: Any): T? {
    return queryForObject(cql, T::class.java, *args).awaitSingleOrNull()
}

suspend inline fun <reified T: Any> ReactiveCqlOperations.coQueryForObject(statement: Statement<*>): T? {
    return queryForObject(statement, T::class.java).awaitSingleOrNull()
}


suspend fun ReactiveCqlOperations.coQueryForMap(cql: String, vararg args: Any): Map<String, Any?> =
    queryForMap(cql, args).awaitSingle()

inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(cql: String, vararg args: Any): Flow<T> =
    queryForFlux(cql, T::class.java, *args).asFlow()

fun ReactiveCqlOperations.queryForMapFlow(cql: String, vararg args: Any): Flow<Map<String, Any?>> =
    queryForFlux(cql, *args).asFlow()

suspend fun ReactiveCqlOperations.coQueryForResultSet(cql: String, vararg args: Any): ReactiveResultSet =
    queryForResultSet(cql, *args).awaitSingle()

fun ReactiveCqlOperations.queryForRowsFlow(statement: Statement<*>): Flow<Row> =
    queryForRows(statement).asFlow()

fun ReactiveCqlOperations.queryForRowsFlow(cql: String, vararg args: Any): Flow<Row> =
    queryForRows(cql, *args).asFlow()


fun ReactiveCqlOperations.executeForFlow(statementFlow: Flow<String>): Flow<Boolean> =
    execute(statementFlow.asPublisher()).asFlow()

suspend fun ReactiveCqlOperations.coExecute(statement: Statement<*>): Boolean =
    execute(statement).awaitSingle()

fun <T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>, rse: (ReactiveResultSet) -> Flow<T>): Flow<T> =
    query(statement) { rs -> rse(rs).asPublisher() }.asFlow()

fun <T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>, rowMapper: (Row, Int) -> T): Flow<T> =
    query(statement) { row, rowNum -> rowMapper(row, rowNum) }.asFlow()

suspend fun ReactiveCqlOperations.coQueryForMap(statement: Statement<*>): Map<String, Any?> =
    queryForMap(statement).awaitSingle()

inline fun <reified T: Any> ReactiveCqlOperations.queryForFlow(statement: Statement<*>): Flow<T> =
    queryForFlux(statement, T::class.java).asFlow()

fun ReactiveCqlOperations.queryForMapFlow(statement: Statement<*>): Flow<Map<String, Any?>> =
    queryForFlux(statement).asFlow()

suspend fun ReactiveCqlOperations.coQueryForResultSet(statement: Statement<*>): ReactiveResultSet =
    queryForResultSet(statement).awaitSingle()

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
