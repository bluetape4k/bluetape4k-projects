package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.support.bindMap
import io.bluetape4k.r2dbc.support.int
import io.bluetape4k.r2dbc.support.long
import io.bluetape4k.r2dbc.support.toParameter
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveInsertOperation
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import org.springframework.r2dbc.core.RowsFetchSpec
import org.springframework.r2dbc.core.awaitOne
import reactor.core.publisher.Mono
import kotlin.reflect.KProperty

fun R2dbcClient.insert(): InsertIntoSpec {
    return InsertIntoSpecImpl(this)
}

interface InsertIntoSpec {
    fun into(table: String): InsertValuesSpec
    fun into(table: String, idColumn: String): InsertValuesKeySpec
    fun <T> into(type: Class<T>): ReactiveInsertOperation.ReactiveInsert<T>
}

inline fun <reified T: Any> InsertIntoSpec.into(): ReactiveInsertOperation.ReactiveInsert<T> = into(T::class.java)

suspend inline fun <T: Any> ReactiveInsertOperation.TerminatingInsert<T>.usingAwaitSingle(supplier: () -> T): T =
    using(supplier.invoke()).awaitSingle()

internal class InsertIntoSpecImpl(private val client: io.bluetape4k.r2dbc.R2dbcClient): InsertIntoSpec {
    override fun into(table: String): InsertValuesSpec {
        return InsertValuesSpecImpl(client, table)
    }

    override fun into(table: String, idColumn: String): InsertValuesKeySpec {
        return InsertValuesKeySpecImpl(client, table, idColumn)
    }

    override fun <T> into(type: Class<T>): ReactiveInsertOperation.ReactiveInsert<T> {
        return client.entityTemplate.insert(type)
    }
}

interface InsertValuesSpec {
    fun value(field: String, value: Any): InsertValuesSpec
    fun value(field: String, value: Any?, type: Class<*>): InsertValuesSpec
    fun value(property: KProperty<*>, value: Any): InsertValuesSpec = value(property.name, value)
    fun value(property: KProperty<*>, value: Any?, type: Class<*>): InsertValuesSpec = value(property.name, value, type)

    fun nullValue(field: String): InsertValuesSpec
    fun nullValue(field: String, type: Class<*>): InsertValuesSpec
    fun nullValue(property: KProperty<*>): InsertValuesSpec = nullValue(property.name)
    fun nullValue(property: KProperty<*>, type: Class<*>): InsertValuesSpec = nullValue(property.name, type)

    fun fetch(): FetchSpec<out Any>
    fun then(): Mono<Void>
    suspend fun await()
    val values: Map<String, Any?>
}

inline fun <reified T: Any> InsertValuesSpec.valueNullable(field: String, value: T? = null) =
    value(field, value, T::class.java)

inline fun <reified T: Any> InsertValuesSpec.valueNullable(property: KProperty<*>, value: T? = null) =
    value(property, value, T::class.java)

internal class InsertValuesSpecImpl(
    private val client: io.bluetape4k.r2dbc.R2dbcClient,
    private val table: String,
): InsertValuesSpec {

    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()

    override fun value(field: String, value: Any): InsertValuesSpec = apply {
        values[field] = value
    }

    override fun value(field: String, value: Any?, type: Class<*>): InsertValuesSpec = apply {
        values[field] = value.toParameter(type)
    }

    override fun nullValue(field: String): InsertValuesSpec = apply {
        values[field] = null
    }

    override fun nullValue(field: String, type: Class<*>): InsertValuesSpec = apply {
        values[field] = type.toParameter()
    }

    private fun execute(): DatabaseClient.GenericExecuteSpec {
        if (values.isEmpty()) {
            error("No values specified")
        }
        val names = values.keys.joinToString(", ")
        val namedArguments = values.keys.joinToString(", ") { ":$it" }
        val sql = "INSERT INTO $table ($names) VALUES ($namedArguments)"
        log.debug { "Insert sql=$sql" }

        return client.databaseClient.sql(sql).bindMap(values)
    }

    override fun fetch(): FetchSpec<out Any> {
        return execute().fetch()
    }

    override fun then(): Mono<Void> {
        return execute().then()
    }

    override suspend fun await() {
        execute().then().awaitFirstOrNull()
    }
}

interface InsertValuesKeySpec {
    fun value(field: String, value: Any): InsertValuesKeySpec
    fun value(field: String, value: Any?, type: Class<*>): InsertValuesKeySpec

    fun value(property: KProperty<*>, value: Any) = value(property.name, value)
    fun value(property: KProperty<*>, value: Any?, type: Class<*>) = value(property.name, value, type)

    fun nullValue(field: String): InsertValuesKeySpec
    fun nullValue(field: String, type: Class<*>): InsertValuesKeySpec
    fun nullValue(property: KProperty<*>) = nullValue(property.name)
    fun nullValue(property: KProperty<*>, type: Class<*>) = nullValue(property.name, type)

    fun fetch(): RowsFetchSpec<Int>
    suspend fun awaitOne(): Int

    fun fetchLong(): RowsFetchSpec<Long>
    suspend fun awaitOneLong(): Long

    fun then(): Mono<Void>
    val values: Map<String, Any?>
}

inline fun <reified T: Any> InsertValuesKeySpec.valueNullable(field: String, value: T? = null) =
    value(field, value, T::class.java)

inline fun <reified T: Any> InsertValuesKeySpec.valueNullable(property: KProperty<*>, value: T? = null) =
    value(property, value, T::class.java)

internal class InsertValuesKeySpecImpl(
    private val client: R2dbcClient,
    private val table: String,
    private val idColumn: String,
): InsertValuesKeySpec {

    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()

    override fun value(field: String, value: Any): InsertValuesKeySpec = apply {
        values[field] = value
    }

    override fun value(field: String, value: Any?, type: Class<*>): InsertValuesKeySpec = apply {
        values[field] = value.toParameter(type)
    }

    override fun nullValue(field: String): InsertValuesKeySpec = apply {
        values[field] = null
    }

    override fun nullValue(field: String, type: Class<*>): InsertValuesKeySpec = apply {
        values[field] = type.toParameter()
    }

    override fun fetch(): RowsFetchSpec<Int> {
        val executeSpec = executeSpec()
        return executeSpec
            .filter { s -> s.returnGeneratedValues(idColumn) }
            .map { row -> row.int(idColumn) }
    }

    override suspend fun awaitOne(): Int {
        return fetch().awaitOne()
    }

    override fun fetchLong(): RowsFetchSpec<Long> {
        val executeSpec = executeSpec()
        return executeSpec
            .filter { s -> s.returnGeneratedValues(idColumn) }
            .map { row -> row.long(idColumn) }
    }

    override suspend fun awaitOneLong(): Long {
        return fetchLong().awaitOne()
    }

    override fun then(): Mono<Void> {
        return then()
    }

    private fun executeSpec(): DatabaseClient.GenericExecuteSpec {
        if (values.isEmpty()) {
            error("No value specified")
        }
        val names = values.keys.joinToString(", ")
        val namedArguments = values.keys.map { ":$it" }.joinToString(", ")
        val sql = "INSERT INTO $table ($names) VALUES ($namedArguments)"
        log.debug { "Insert sql=$sql" }

        return client.databaseClient.sql(sql).bindMap(values)
    }
}
