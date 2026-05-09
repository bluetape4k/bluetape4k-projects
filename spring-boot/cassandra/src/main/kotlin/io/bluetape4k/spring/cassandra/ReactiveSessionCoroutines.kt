package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession

/**
 * [ReactiveSession]의 Reactor API를 코루틴 suspend 함수로 감싼 확장 함수 모음입니다.
 */

/**
 * CQL 문자열을 실행하고 결과 [ReactiveResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(query).awaitSingle()`을 호출합니다.
 * - CQL 실행 실패 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val resultSet = reactiveSession.executeSuspending("SELECT * FROM users")
 * // result == resultSet.availableRows()
 * ```
 */
suspend fun ReactiveSession.executeSuspending(query: String): ReactiveResultSet =
    executeSuspending(SimpleStatement.newInstance(query))

/**
 * CQL 문자열과 위치 기반 파라미터를 실행하고 결과 [ReactiveResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(query, *args).awaitSingle()`을 호출합니다.
 * - 인자 개수/타입 불일치 예외는 드라이버에서 발생한 그대로 전파됩니다.
 *
 * ```kotlin
 * val resultSet = reactiveSession.executeSuspending("SELECT * FROM users WHERE id = ?", "user-1")
 * // result == resultSet.rows().hasElements()
 * ```
 */
suspend fun ReactiveSession.executeSuspending(
    query: String,
    vararg args: Any,
): ReactiveResultSet =
    executeSuspending(SimpleStatement.newInstance(query, *args))

/**
 * CQL 문자열과 이름 기반 파라미터를 실행하고 결과 [ReactiveResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(query, args).awaitSingle()`을 호출합니다.
 * - 누락된 바인딩 이름/타입 오류 예외는 드라이버에서 발생한 그대로 전파됩니다.
 *
 * ```kotlin
 * val resultSet = reactiveSession.executeSuspending(
 *     "SELECT * FROM users WHERE id = :id",
 *     mapOf("id" to "user-1")
 * )
 * // result == resultSet.availableRows()
 * ```
 */
suspend fun ReactiveSession.executeSuspending(
    query: String,
    args: Map<String, Any?>,
): ReactiveResultSet =
    executeSuspending(SimpleStatement.newInstance(query, args))

/**
 * [Statement]를 실행하고 결과 [ReactiveResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(statement).awaitSingle()`을 호출합니다.
 * - Statement 옵션(fetch size, consistency level 등)은 전달된 객체 설정을 그대로 따릅니다.
 *
 * ```kotlin
 * val resultSet = reactiveSession.executeSuspending(boundStatement)
 * // result == resultSet.availableRows()
 * ```
 */
suspend fun ReactiveSession.executeSuspending(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

/**
 * CQL 문자열을 준비(prepare)해 [PreparedStatement]를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `prepare(query).awaitSingle()`을 호출합니다.
 * - 잘못된 CQL 문법이면 prepare 예외가 그대로 전파됩니다.
 *
 * ```kotlin
 * val prepared = reactiveSession.prepareSuspending("SELECT * FROM users WHERE id = ?")
 * // result == prepared.variableDefinitions.size()
 * ```
 */
suspend fun ReactiveSession.prepareSuspending(query: String): PreparedStatement = prepare(query).awaitSingle()

/**
 * [SimpleStatement]를 준비(prepare)해 [PreparedStatement]를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `prepare(statement).awaitSingle()`을 호출합니다.
 * - Statement에 포함된 CQL/옵션 정보를 기반으로 prepare가 수행됩니다.
 *
 * ```kotlin
 * val prepared = reactiveSession.prepareSuspending(simpleStatement)
 * // result == prepared.variableDefinitions.size()
 * ```
 */
suspend fun ReactiveSession.prepareSuspending(statement: SimpleStatement): PreparedStatement =
    prepare(statement).awaitSingle()
