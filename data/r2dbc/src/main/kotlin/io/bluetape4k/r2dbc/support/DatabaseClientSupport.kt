package io.bluetape4k.r2dbc.support

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import org.springframework.r2dbc.core.DatabaseClient

private val log by lazy { KotlinLogging.logger {} }

/**
 * Query의 parameter 정보를 매핑합니다.
 *
 * ```
 * val sql = "SELECT * FROM Persons where username = :username"
 * val parameters = mapOf("username", "Scott")
 *
 * val rows = return databaseClient
 *     .sql(sql)
 *     .bindMap(parameters)
 *     .map { row, rowMetadata ->
 *         mappingConverter.read(T::class.java, row, rowMetadata)
 * ```
 *
 * @param parameters named query parameters
 */
fun DatabaseClient.GenericExecuteSpec.bindMap(parameters: Map<String, Any?>): DatabaseClient.GenericExecuteSpec {
    return parameters.entries.fold(this) { spec, entry ->
        log.trace { "bind map. name=${entry.key}, value=${entry.value}" }
        val value = entry.value
        when (value) {
            null -> spec.bindNull(entry.key, String::class.java)
            else -> spec.bind(entry.key, value.toParameter())
        }
    }
}

/**
 * Query의 parameter 정보를 매핑합니다.
 *
 * ```
 * val sql = "SELECT * FROM Persons where username = ?1"
 * val parameters = mapOf(1, "Scott")
 *
 * val rows = return databaseClient
 *     .sql(sql)
 *     .bindIndexedMap(parameters)
 *     .map { row, rowMetadata ->
 *         mappingConverter.read(T::class.java, row, rowMetadata)
 * ```
 *
 * @param parameters named query parameters
 */
fun DatabaseClient.GenericExecuteSpec.bindIndexedMap(parameters: Map<Int, Any?>): DatabaseClient.GenericExecuteSpec {
    return parameters.entries.fold(this) { spec, entry ->
        log.trace { "bind indexed map. index=${entry.key}, value=${entry.value}" }
        val value = entry.value
        when (value) {
            null -> spec.bindNull(entry.key, String::class.java)
            else -> spec.bind(entry.key, value.toParameter())
        }
    }
}

/**
 * [sql]을 실행합니다.
 *
 * @param sql SQL 구문
 */
fun DatabaseClient.execute(sql: String): DatabaseClient.GenericExecuteSpec {
    return sql(sql)
}

fun DatabaseClient.execute(sql: String, parameters: Map<String, Any?>): DatabaseClient.GenericExecuteSpec {
    return sql(sql).bindMap(parameters)
}

inline fun <reified V: Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    index: Int,
    value: V? = null,
) = apply {
    bind(index, value.toParameter(V::class.java))
}

inline fun <reified V: Any> DatabaseClient.GenericExecuteSpec.bindNullable(
    name: String,
    value: V? = null,
) = apply {
    bind(name, value.toParameter(V::class.java))
}
