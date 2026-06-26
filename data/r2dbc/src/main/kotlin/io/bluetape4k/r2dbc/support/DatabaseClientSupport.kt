package io.bluetape4k.r2dbc.support

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.springframework.r2dbc.core.DatabaseClient

private val log by lazy { KotlinLogging.logger {} }

/**
 * Binds named query parameters from a map.
 *
 * Named parameters are referenced as `:name` in SQL and mapped by key.
 * Raw null values are rejected because they do not carry R2DBC type
 * information. Use [typedNullParameter] or an explicit `Parameter` value when
 * binding nullable map entries.
 *
 * ```kotlin
 * val sql = "SELECT * FROM users WHERE username = :username AND active = :active"
 * val parameters = mapOf(
 *     "username" to "john",
 *     "active" to true
 * )
 *
 * val users = databaseClient
 *     .sql(sql)
 *     .bindMap(parameters)
 *     .map { row, metadata ->
 *         User(
 *             id = row.get("id") as Int,
 *             name = row.get("name") as String
 *         )
 *     }
 *     .all()
 * ```
 *
 * @param parameters named query parameters.
 * @return [DatabaseClient.GenericExecuteSpec] with the parameters bound.
 * @throws IllegalArgumentException when a map entry contains a raw null value.
 */
fun DatabaseClient.GenericExecuteSpec.bindMap(parameters: Map<String, Any?>): DatabaseClient.GenericExecuteSpec =
    parameters.entries.fold(this) { spec, entry ->
        log.trace { "bind map. name=${entry.key}, value=${entry.value}" }
        when (val value = entry.value) {
            null -> throw rawNullBindingException(entry.key)
            else -> spec.bind(entry.key, value.toParameter())
        }
    }

/**
 * Binds indexed query parameters.
 *
 * Indexed parameters follow Spring R2DBC's zero-based binding contract. The first
 * positional parameter is index `0`, the second is index `1`, and so on.
 * Raw null values are rejected because they do not carry R2DBC type
 * information. Use [typedNullParameter] or an explicit `Parameter` value when
 * binding nullable map entries.
 *
 * ```kotlin
 * val sql = "SELECT * FROM users WHERE username = ? AND active = ?"
 * val parameters = mapOf(
 *     0 to "john",
 *     1 to true
 * )
 *
 * val users = databaseClient
 *     .sql(sql)
 *     .bindIndexedMap(parameters)
 *     .map { row, metadata ->
 *         User(
 *             id = row.get("id") as Int,
 *             name = row.get("name") as String
 *         )
 *     }
 *     .all()
 * ```
 *
 * @param parameters indexed query parameters from zero-based index to value.
 * @return [DatabaseClient.GenericExecuteSpec] with the parameters bound.
 * @throws IllegalArgumentException when any index is negative.
 * @throws IllegalArgumentException when a map entry contains a raw null value.
 */
fun DatabaseClient.GenericExecuteSpec.bindIndexedMap(parameters: Map<Int, Any?>): DatabaseClient.GenericExecuteSpec =
    parameters.entries.fold(this) { spec, entry ->
        val index = entry.key.requireZeroOrPositiveNumber("index")
        log.trace { "bind indexed map. index=$index, value=${entry.value}" }
        when (val value = entry.value) {
            null -> throw rawNullBindingException("index $index")
            else -> spec.bind(index, value.toParameter())
        }
    }

/**
 * SQL 문을 실행할 [DatabaseClient.GenericExecuteSpec]을 생성합니다.
 *
 * ```kotlin
 * val result = databaseClient
 *     .execute("SELECT * FROM users")
 *     .map { row, _ -> /* mapping */ }
 *     .all()
 * ```
 *
 * @param sqlString 실행할 SQL 문
 * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
 */
fun DatabaseClient.execute(sqlString: String): DatabaseClient.GenericExecuteSpec = sql(sqlString)

/**
 * SQL 문을 실행할 [DatabaseClient.GenericExecuteSpec]을 생성하고 파라미터를 바인딩합니다.
 *
 * ```kotlin
 * val parameters = mapOf("active" to true, "limit" to 10)
 * val users = databaseClient
 *     .execute("SELECT * FROM users WHERE active = :active LIMIT :limit", parameters)
 *     .map { row, _ -> /* mapping */ }
 *     .all()
 * ```
 *
 * @param sqlString 실행할 SQL 문
 * @param parameters named query parameters
 * @return 파라미터가 바인딩된 [DatabaseClient.GenericExecuteSpec]
 */
fun DatabaseClient.execute(
    sqlString: String,
    parameters: Map<String, Any?>,
): DatabaseClient.GenericExecuteSpec = sql(sqlString).bindMap(parameters)

/**
 * Binds a nullable value to an indexed parameter.
 *
 * The [index] follows Spring R2DBC's zero-based binding contract. The first
 * positional parameter is index `0`. Null values are bound as typed NULL values.
 *
 * ```kotlin
 * databaseClient
 *     .sql("SELECT * FROM users WHERE name = ?")
 *     .bindNullable<String>(0, nullableName)
 *     .map { row, _ -> /* mapping */ }
 * ```
 *
 * @param V Parameter type.
 * @param index Zero-based parameter index.
 * @param value Value to bind.
 * @return [DatabaseClient.GenericExecuteSpec] with the parameter bound.
 * @throws IllegalArgumentException when [index] is negative.
 */
inline fun <reified V: Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    index: Int,
    value: V? = null,
) = apply {
    bind(index.requireZeroOrPositiveNumber("index"), value.toParameter(V::class.java))
}

/**
 * Named 파라미터에 nullable 값을 바인딩합니다.
 * null 값은 NULL로 바인딩됩니다.
 *
 * ```kotlin
 * databaseClient
 *     .sql("SELECT * FROM users WHERE name = :name")
 *     .bindNullable<String>("name", nullableName)
 *     .map { row, _ -> /* mapping */ }
 * ```
 *
 * @param V 파라미터의 타입
 * @param name 파라미터 이름
 * @param value 바인딩할 값 (null 가능)
 * @return 파라미터가 바인딩된 [DatabaseClient.GenericExecuteSpec]
 */
inline fun <reified V: Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    name: String,
    value: V? = null,
) = apply {
    bind(name, value.toParameter(V::class.java))
}
