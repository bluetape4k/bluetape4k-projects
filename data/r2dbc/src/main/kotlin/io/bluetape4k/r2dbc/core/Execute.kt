package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.query.Query
import io.bluetape4k.r2dbc.support.bindIndexedMap
import io.bluetape4k.r2dbc.support.bindMap
import io.bluetape4k.r2dbc.support.toParameter
import io.r2dbc.spi.Parameters
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import kotlin.reflect.KClass

/**
 * 지정한 [sqlString] 을 수행할 [DatabaseClient.GenericExecuteSpec] 을 생성합니다.
 *
 * @param sqlString 수행할 SQL 구문
 * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
 */
fun R2dbcClient.execute(sqlString: String): DatabaseClient.GenericExecuteSpec =
    this.databaseClient.sql(sqlString)

/**
 * 지정한 [sqlString] 을 수행할 [DatabaseClient.GenericExecuteSpec] 을 생성합니다.
 *
 * @param sqlString 수행할 SQL 구문
 * @param parameters Binding 할 parameters
 * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
 */
fun R2dbcClient.execute(
    sqlString: String,
    parameters: Map<String, Any?>,
): DatabaseClient.GenericExecuteSpec =
    databaseClient.sql(sqlString).bindMap(parameters)

/**
 * 지정한 [query]로부터 SQL 문과 Parameters 정보를 이용하여 [DatabaseClient.GenericExecuteSpec]를 생성합니다.
 *
 * @param query [io.bluetape4k.r2dbc.query.QueryBuilder]로부터 생성한 [Query]
 * @return [DatabaseClient.GenericExecuteSpec] 인스턴스
 *
 * @see [io.bluetape4k.r2dbc.query.QueryBuilder]
 */
fun R2dbcClient.execute(query: Query): DatabaseClient.GenericExecuteSpec =
    execute(query.sql, query.parameters)

inline fun <reified T: Any> R2dbcClient.execute(sql: String): BindSpec<T> =
    BindSpecImpl(this, sql, T::class)

interface BindSpec<T: Any> {
    fun bind(index: Int, value: Any): BindSpec<T>
    fun bind(index: Int, value: Any?, type: Class<*>): BindSpec<T>
    fun bindNull(index: Int, type: Class<*>): BindSpec<T>

    fun bind(name: String, value: Any): BindSpec<T>
    fun bind(name: String, value: Any?, type: Class<*>): BindSpec<T>
    fun bindNull(name: String, type: Class<*>): BindSpec<T>

    fun fetch(): RowsFetchSpec<T>
}

inline fun <T: Any, reified V: Any> BindSpec<T>.bindNullable(index: Int, value: V? = null): BindSpec<T> =
    bind(index, value, V::class.java)

inline fun <T: Any, reified V: Any> BindSpec<T>.bindNullable(name: String, value: V? = null): BindSpec<T> =
    bind(name, value, V::class.java)

@PublishedApi
internal class BindSpecImpl<T: Any>(
    private val client: R2dbcClient,
    private val sql: String,
    private val type: KClass<T>,
): BindSpec<T> {

    companion object: KLogging()

    private val indexedParameters = mutableMapOf<Int, Any?>()
    private val namedParameters = mutableMapOf<String, Any?>()

    override fun bind(index: Int, value: Any) = apply {
        indexedParameters[index] = value
    }

    override fun bind(index: Int, value: Any?, type: Class<*>) = apply {
        indexedParameters[index] = value.toParameter(type)
    }

    override fun bind(name: String, value: Any): BindSpec<T> = apply {
        namedParameters[name] = value
    }

    override fun bind(name: String, value: Any?, type: Class<*>) = apply {
        namedParameters[name] = value.toParameter(type)
    }

    override fun bindNull(index: Int, type: Class<*>) = apply {
        indexedParameters[index] = Parameters.`in`(type)
    }

    override fun bindNull(name: String, type: Class<*>) = apply {
        namedParameters[name] = Parameters.`in`(type)
    }

    override fun fetch(): RowsFetchSpec<T> {
        log.debug { "sql=$sql, named params=$namedParameters" }
        return client.databaseClient
            .sql(sql)
            .bindMap(namedParameters)
            .bindIndexedMap(indexedParameters)
            .map { row, rowMetadata ->
                client.mappingConverter.read(type.java, row, rowMetadata)
            }
    }
}

inline fun <reified T: Any> R2dbcClient.execute(
    sql: String,
    parameters: Map<String, Any?> = emptyMap(),
): RowsFetchSpec<T> {
    return databaseClient
        .sql(sql)
        .bindMap(parameters)
        .map { row, rowMetadata ->
            mappingConverter.read(T::class.java, row, rowMetadata)
        }
}

inline fun <reified T: Any> R2dbcClient.execute(query: Query): RowsFetchSpec<T> {
    return execute<T>(query.sql, query.parameters)
}
