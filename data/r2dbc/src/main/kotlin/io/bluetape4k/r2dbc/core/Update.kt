package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.query.Query
import io.bluetape4k.r2dbc.support.bindMap
import io.bluetape4k.r2dbc.support.toParameter
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import reactor.core.publisher.Mono
import kotlin.reflect.KProperty

fun R2dbcClient.update(): UpdateTableSpec {
    return UpdateTableSpecImpl(this)
}

interface UpdateTableSpec {
    fun table(table: String): UpdateValuesSpec
    fun <T> table(domainType: Class<T>): ReactiveUpdateOperation.ReactiveUpdate
}

inline fun <reified T: Any> UpdateTableSpec.table(): ReactiveUpdateOperation.ReactiveUpdate {
    return table(T::class.java)
}

inline fun <reified T: Any> ReactiveUpdateOperation.UpdateWithQuery.using(
    obj: T,
    client: io.bluetape4k.r2dbc.R2dbcClient,
): Mono<Long> {
    val dataAccessStrategy = client.entityTemplate.dataAccessStrategy
    val idColumns = dataAccessStrategy.getIdentifierColumns(T::class.java)
    if (idColumns.isEmpty()) {
        error("Identifier columns not declared")
    }
    val columns = dataAccessStrategy.getAllColumns(T::class.java) - idColumns
    if (columns.isEmpty()) {
        error("There are no not-identifier columns to update")
    }

    val firstIdColumn = idColumns.first()
    val outboundRow = dataAccessStrategy.getOutboundRow(obj)
    val where = Criteria.where(firstIdColumn.reference).`is`(
        outboundRow[firstIdColumn].value ?: error("Identifier value not set (${firstIdColumn.reference})")
    )
    val criteria = idColumns.drop(1).fold(where) { criteria, idColumn ->
        criteria.and(idColumn.reference).`is`(
            outboundRow[idColumn].value ?: error("Identitifer value not set (${idColumn.reference})")
        )
    }

    val firstColumn = columns.first()
    val firstUpdate = Update.update(firstColumn.reference, outboundRow[firstColumn].value)
    val update = columns.drop(1).fold(firstUpdate) { update, column ->
        update.set(column.reference, outboundRow[column].value)
    }

    return matching(query(criteria)).apply(update)
}

internal class UpdateTableSpecImpl(private val client: R2dbcClient): UpdateTableSpec {
    override fun table(table: String): UpdateValuesSpec {
        return UpdateValuesSpecImpl(client, table)
    }

    override fun <T> table(domainType: Class<T>): ReactiveUpdateOperation.ReactiveUpdate {
        return client.entityTemplate.update(domainType)
    }
}

interface SetterSpec {
    val Update: SetterSpec

    fun update(field: String, value: Any): UpdateValuesSpec
    fun update(field: String, value: Any?, type: Class<*>): UpdateValuesSpec
    fun set(field: String, value: Any): UpdateValuesSpec
    fun set(field: String, value: Any?, type: Class<*>): UpdateValuesSpec
    fun set(parameters: Map<String, Any?>): UpdateValuesSpec
    val values: Map<String, Any?>

    fun update(property: KProperty<*>, value: Any) = update(property.name, value)
    fun update(property: KProperty<*>, value: Any?, type: Class<*>) = update(property.name, value, type)
    fun set(property: KProperty<*>, value: Any) = set(property.name, value)
    fun set(property: KProperty<*>, value: Any?, type: Class<*>) = set(property.name, value, type)
}

inline fun <reified T: Any> SetterSpec.updateNullable(field: String, value: T? = null) =
    update(field, value, T::class.java)

inline fun <reified T: Any> SetterSpec.updateNullable(property: KProperty<*>, value: T? = null) =
    update(property, value, T::class.java)

inline fun <reified T: Any> SetterSpec.setNullable(field: String, value: T? = null) =
    set(field, value, T::class.java)

inline fun <reified T: Any> SetterSpec.setNullable(property: KProperty<*>, value: T? = null) =
    set(property, value, T::class.java)

interface UpdateValuesSpec: SetterSpec {
    fun using(setters: SetterSpec.() -> Unit): UpdateValuesSpec
    fun matching(where: String? = null, whereParameters: Map<String, Any?>? = null): DatabaseClient.GenericExecuteSpec
    fun matching(query: Query): DatabaseClient.GenericExecuteSpec =
        matching(query.sql, query.parameters)

    fun fetch(): FetchSpec<MutableMap<String, Any>> {
        return matching().fetch()
    }

    fun then(): Mono<Void> {
        return matching().then()
    }
}

internal class UpdateValuesSpecImpl(
    private val client: R2dbcClient,
    private val table: String,
): UpdateValuesSpec {

    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()
    override val Update: SetterSpec get() = this

    override fun update(field: String, value: Any): UpdateValuesSpec = apply {
        set(field, value)
    }

    override fun update(field: String, value: Any?, type: Class<*>): UpdateValuesSpec = apply {
        set(field, value, type)
    }

    override fun set(field: String, value: Any): UpdateValuesSpec = apply {
        values[field] = value
    }

    override fun set(field: String, value: Any?, type: Class<*>): UpdateValuesSpec = apply {
        values[field] = value.toParameter(type)
    }

    override fun set(parameters: Map<String, Any?>): UpdateValuesSpec = apply {
        parameters.forEach { (key, value) ->
            when (value) {
                null -> setNullable<Any>(key, value)
                else -> set(key, value)
            }
        }
    }

    override fun using(setters: SetterSpec.() -> Unit): UpdateValuesSpec = apply {
        setters()
    }

    override fun matching(where: String?, whereParameters: Map<String, Any?>?): DatabaseClient.GenericExecuteSpec {
        val updateParameters = values
            .map { (name, _) -> "$name = :$name" }
            .joinToString(", ")
        val sql = "UPDATE $table SET $updateParameters"

        val sqlToExecute = if (where != null) "$sql WHERE $where" else sql

        log.debug { "Update SQL=$sqlToExecute" }
        return client.databaseClient.sql(sqlToExecute)
            .bindMap(values + (whereParameters ?: emptyMap()))
    }
}
