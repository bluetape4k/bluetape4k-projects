package io.bluetape4k.r2dbc.support

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import org.springframework.r2dbc.core.DatabaseClient

private val log by lazy { KotlinLogging.logger {} }

/**
 * Query의 named parameter 정보를 매핑합니다.
 *
 * Named parameter는 `:name` 형식으로 SQL에 지정하고, Map의 키로 파라미터 이름을 지정합니다.
 * null 값은 자동으로 NULL로 바인딩됩니다.
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
 * @param parameters named query parameters (파라미터 이름 -> 값)
 * @return 파라미터가 바인딩된 [DatabaseClient.GenericExecuteSpec]
 */
fun DatabaseClient.GenericExecuteSpec.bindMap(parameters: Map<String, Any?>): DatabaseClient.GenericExecuteSpec =
    parameters.entries.fold(this) { spec, entry ->
        log.trace { "bind map. name=${entry.key}, value=${entry.value}" }
        when (val value = entry.value) {
            null -> spec.bindNull(entry.key, String::class.java)
            else -> spec.bind(entry.key, value.toParameter())
        }
    }

/**
 * Query의 indexed parameter 정보를 매핑합니다.
 *
 * Indexed parameter는 `?` 형식으로 SQL에 지정하고, Map의 키로 인덱스(1부터 시작)를 지정합니다.
 * null 값은 자동으로 NULL로 바인딩됩니다.
 *
 * ```kotlin
 * val sql = "SELECT * FROM users WHERE username = ? AND active = ?"
 * val parameters = mapOf(
 *     1 to "john",
 *     2 to true
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
 * @param parameters indexed query parameters (인덱스 -> 값, 인덱스는 1부터 시작)
 * @return 파라미터가 바인딩된 [DatabaseClient.GenericExecuteSpec]
 */
fun DatabaseClient.GenericExecuteSpec.bindIndexedMap(parameters: Map<Int, Any?>): DatabaseClient.GenericExecuteSpec =
    parameters.entries.fold(this) { spec, entry ->
        log.trace { "bind indexed map. index=${entry.key}, value=${entry.value}" }
        when (val value = entry.value) {
            null -> spec.bindNull(entry.key, String::class.java)
            else -> spec.bind(entry.key, value.toParameter())
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
 * Indexed 파라미터에 nullable 값을 바인딩합니다.
 * null 값은 NULL로 바인딩됩니다.
 *
 * ```kotlin
 * databaseClient
 *     .sql("SELECT * FROM users WHERE name = ?")
 *     .bindNullable<String>(1, nullableName)
 *     .map { row, _ -> /* mapping */ }
 * ```
 *
 * @param V 파라미터의 타입
 * @param index 파라미터 인덱스 (1부터 시작)
 * @param value 바인딩할 값 (null 가능)
 * @return 파라미터가 바인딩된 [DatabaseClient.GenericExecuteSpec]
 */
inline fun <reified V: Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    index: Int,
    value: V? = null,
) = apply {
    bind(index, value.toParameter(V::class.java))
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
