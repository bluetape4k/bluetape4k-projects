package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.AsyncCqlSession
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.PrepareRequest
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import com.datastax.oss.driver.internal.core.cql.DefaultPrepareRequest
import kotlinx.coroutines.future.await

/**
 * [CqlSession] query 를 suspend 환경에서 실행합니다.
 *
 * ```
 * val session = CqlSession.builder().build()
 * val result = session.executeSuspending("SELECT * FROM table")
 * ```
 */
suspend inline fun AsyncCqlSession.executeSuspending(cql: String, vararg values: Any?): AsyncResultSet =
    executeSuspending(statementOf(cql, *values))

/**
 * [CqlSession] named parameter query 를 suspend 환경에서 실행합니다.
 *
 * ```
 * val session = CqlSession.builder().build()
 * val result = session.executeSuspending("SELECT * FROM table where key=:key", mapOf("key" to "value"))
 * ```
 */
suspend inline fun AsyncCqlSession.executeSuspending(cql: String, values: Map<String, Any?>): AsyncResultSet =
    executeSuspending(statementOf(cql, values))

suspend inline fun AsyncCqlSession.executeSuspending(cql: String): AsyncResultSet =
    executeSuspending(statementOf(cql))

/**
 * [Statement] 를 suspend 환경에서 실행합니다.
 *
 * ```
 * val session = CqlSession.builder().build()
 * val statement = SimpleStatement.newInstance("SELECT * FROM table")
 * val result = session.executeSuspending(statement)
 * ```
 */
suspend inline fun AsyncCqlSession.executeSuspending(statement: Statement<*>): AsyncResultSet =
    executeAsync(statement).await()


/**
 * [PreparedStatement] 를 suspend 환경에서 준비합니다.
 *
 * ```
 * val session = CqlSession.builder().build()
 * val statement = session.prepareSuspending("SELECT * FROM table where key=:key")
 * ```
 */
suspend inline fun AsyncCqlSession.prepareSuspending(cql: String): PreparedStatement =
    prepareSuspending(DefaultPrepareRequest(cql))

/**
 * [SimpleStatement] 기반 [PreparedStatement] 를 suspend 환경에서 준비합니다.
 *
 * ```
 * val session = CqlSession.builder().build()
 * val statement = SimpleStatement.newInstance("SELECT * FROM table where key=:key")
 * val prepared = session.prepareSuspending(statement)
 * ```
 */
suspend inline fun AsyncCqlSession.prepareSuspending(statement: SimpleStatement): PreparedStatement =
    prepareSuspending(DefaultPrepareRequest(statement))

suspend inline fun AsyncCqlSession.prepareSuspending(request: PrepareRequest): PreparedStatement =
    prepareAsync(request).await()

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(cql, *values)"))
suspend inline fun AsyncCqlSession.suspendExecute(cql: String, vararg values: Any?): AsyncResultSet =
    executeSuspending(cql, *values)

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(cql, values)"))
suspend inline fun AsyncCqlSession.suspendExecute(cql: String, values: Map<String, Any?>): AsyncResultSet =
    executeSuspending(cql, values)

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(statement)"))
suspend inline fun AsyncCqlSession.suspendExecute(statement: Statement<*>): AsyncResultSet =
    executeSuspending(statement)

@Deprecated("Use prepareSuspending", replaceWith = ReplaceWith("prepareSuspending(cql)"))
suspend inline fun AsyncCqlSession.suspendPrepare(cql: String): PreparedStatement =
    prepareSuspending(cql)

@Deprecated("Use prepareSuspending", replaceWith = ReplaceWith("prepareSuspending(statement)"))
suspend inline fun AsyncCqlSession.suspendPrepare(statement: SimpleStatement): PreparedStatement =
    prepareSuspending(statement)

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(cql, *values)"))
suspend inline fun AsyncCqlSession.execute(cql: String, vararg values: Any?): AsyncResultSet =
    executeSuspending(cql, *values)

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(cql, values)"))
suspend inline fun AsyncCqlSession.execute(cql: String, values: Map<String, Any?>): AsyncResultSet =
    executeSuspending(cql, values)

@Deprecated("Use executeSuspending", replaceWith = ReplaceWith("executeSuspending(statement)"))
suspend inline fun AsyncCqlSession.execute(statement: Statement<*>): AsyncResultSet =
    executeSuspending(statement)

@Deprecated("Use prepareSuspending", replaceWith = ReplaceWith("prepareSuspending(cql)"))
suspend inline fun AsyncCqlSession.prepare(cql: String): PreparedStatement =
    prepareSuspending(cql)

@Deprecated("Use prepareSuspending", replaceWith = ReplaceWith("prepareSuspending(statement)"))
suspend inline fun AsyncCqlSession.prepare(statement: SimpleStatement): PreparedStatement =
    prepareSuspending(statement)
