package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession

/**
 * [ReactiveSession]의 코루틴 확장 함수 모음입니다.
 */

/**
 * CQL 문자열을 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 */
/**
 * CQL 문자열을 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 */
suspend fun ReactiveSession.executeSuspending(query: String): ReactiveResultSet =
    execute(query).awaitSingle()

/**
 * CQL 문자열을 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 */
@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(query)")
)
suspend fun ReactiveSession.suspendExecute(query: String): ReactiveResultSet =
    executeSuspending(query)

/**
 * CQL 문자열과 바인딩 파라미터로 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 * @param args 바인딩 파라미터
 */
suspend fun ReactiveSession.executeSuspending(query: String, vararg args: Any?): ReactiveResultSet =
    execute(query, *args).awaitSingle()

/**
 * CQL 문자열과 바인딩 파라미터로 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 * @param args 바인딩 파라미터
 */
@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(query, *args)")
)
suspend fun ReactiveSession.suspendExecute(query: String, vararg args: Any?): ReactiveResultSet =
    executeSuspending(query, *args)

/**
 * CQL 문자열과 이름 기반 파라미터로 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 * @param args 이름 기반 파라미터
 */
suspend fun ReactiveSession.executeSuspending(query: String, args: Map<String, Any?>): ReactiveResultSet =
    execute(query, args).awaitSingle()

/**
 * CQL 문자열과 이름 기반 파라미터로 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param query 실행할 CQL
 * @param args 이름 기반 파라미터
 */
@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(query, args)")
)
suspend fun ReactiveSession.suspendExecute(query: String, args: Map<String, Any?>): ReactiveResultSet =
    executeSuspending(query, args)

/**
 * [Statement]를 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param statement 실행할 Statement
 */
suspend fun ReactiveSession.executeSuspending(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

/**
 * [Statement]를 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * @param statement 실행할 Statement
 */
@Deprecated(
    message = "executeSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("executeSuspending(statement)")
)
suspend fun ReactiveSession.suspendExecute(statement: Statement<*>): ReactiveResultSet =
    executeSuspending(statement)

/**
 * CQL 문자열을 준비하고 [PreparedStatement]를 반환합니다.
 *
 * @param query 준비할 CQL
 */
suspend fun ReactiveSession.prepareSuspending(query: String): PreparedStatement =
    prepare(query).awaitSingle()

/**
 * CQL 문자열을 준비하고 [PreparedStatement]를 반환합니다.
 *
 * @param query 준비할 CQL
 */
@Deprecated(
    message = "prepareSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("prepareSuspending(query)")
)
suspend fun ReactiveSession.suspendPrepare(query: String): PreparedStatement =
    prepareSuspending(query)

/**
 * [SimpleStatement]를 준비하고 [PreparedStatement]를 반환합니다.
 *
 * @param statement 준비할 Statement
 */
suspend fun ReactiveSession.prepareSuspending(statement: SimpleStatement): PreparedStatement =
    prepare(statement).awaitSingle()

/**
 * [SimpleStatement]를 준비하고 [PreparedStatement]를 반환합니다.
 *
 * @param statement 준비할 Statement
 */
@Deprecated(
    message = "prepareSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("prepareSuspending(statement)")
)
suspend fun ReactiveSession.suspendPrepare(statement: SimpleStatement): PreparedStatement =
    prepareSuspending(statement)
