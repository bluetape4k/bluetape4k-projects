package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.query.Query
import io.bluetape4k.r2dbc.support.bindMap
import io.bluetape4k.r2dbc.support.toParameter
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation
import org.springframework.data.r2dbc.mapping.OutboundRow
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.FetchSpec
import reactor.core.publisher.Mono
import kotlin.reflect.KProperty

/**
 * R2DBC UPDATE 연산을 시작합니다.
 *
 * @return [UpdateTableSpec] 인스턴스
 */
fun R2dbcClient.update(): UpdateTableSpec = UpdateTableSpecImpl(this)

/**
 * UPDATE 연산의 대상 테이블을 지정하는 인터페이스
 */
interface UpdateTableSpec {
    /**
     * 업데이트할 테이블 이름을 지정합니다.
     */
    fun table(table: String): UpdateValuesSpec

    /**
     * 엔티티 타입으로 업데이트할 테이블을 지정합니다.
     */
    fun <T> table(domainType: Class<T>): ReactiveUpdateOperation.ReactiveUpdate
}

/**
 * 제네릭 타입으로 UPDATE 대상을 지정합니다.
 */
inline fun <reified T: Any> UpdateTableSpec.table(): ReactiveUpdateOperation.ReactiveUpdate = table(T::class.java)

/**
 * 엔티티 객체를 사용하여 UPDATE를 수행합니다.
 *
 * @param T 엔티티 타입
 * @param obj 업데이트할 엔티티 객체
 * @param client R2DBC 클라이언트
 * @return 업데이트된 행 수를 담은 Mono
 */
inline fun <reified T: Any> ReactiveUpdateOperation.UpdateWithQuery.using(
    obj: T,
    client: R2dbcClient,
): Mono<Long> {
    val dataAccessStrategy = client.entityTemplate.dataAccessStrategy
    val idColumns = dataAccessStrategy.getIdentifierColumns(T::class.java)
    if (idColumns.isEmpty()) {
        error("Identifier 컬럼이 정의되어 있지 않습니다.")
    }
    val columns = dataAccessStrategy.getAllColumns(T::class.java) - idColumns
    if (columns.isEmpty()) {
        error("identifier 컬럼을 제외한 Update할 컬럼이 없습니다.")
    }

    val firstIdColumn: SqlIdentifier = idColumns.first()
    val outboundRow: OutboundRow = dataAccessStrategy.getOutboundRow(obj)
    val where: Criteria =
        Criteria.where(firstIdColumn.reference).`is`(
            outboundRow[firstIdColumn]?.value
                ?: error("Identifier value not set for column [${firstIdColumn.reference}]"),
        )
    val criteria: Criteria =
        idColumns.drop(1).fold(where) { criteria, idColumn ->
            criteria.and(idColumn.reference).`is`(
                outboundRow[idColumn]?.value ?: error("Identitifer value not set for column [${idColumn.reference}]"),
            )
        }

    val firstColumn: SqlIdentifier = columns.first()
    val firstUpdate: Update = Update.update(firstColumn.reference, outboundRow[firstColumn]?.value)
    val update =
        columns.drop(1).fold(firstUpdate) { update, column ->
            update.set(column.reference, outboundRow[column]?.value)
        }

    return matching(query(criteria)).apply(update)
}

internal class UpdateTableSpecImpl(
    private val client: R2dbcClient,
): UpdateTableSpec {
    override fun table(table: String): UpdateValuesSpec = UpdateValuesSpecImpl(client, table)

    override fun <T> table(domainType: Class<T>): ReactiveUpdateOperation.ReactiveUpdate =
        client.entityTemplate.update(domainType)
}

/**
 * UPDATE 값 설정 인터페이스
 */
interface SetterSpec {
    val Update: SetterSpec

    /**
     * 컬럼 값을 업데이트합니다. (set의 별칭)
     */
    fun update(
        field: String,
        value: Any,
    ): UpdateValuesSpec

    /**
     * nullable 컬럼 값을 업데이트합니다.
     */
    fun update(
        field: String,
        value: Any?,
        type: Class<*>,
    ): UpdateValuesSpec

    /**
     * 컬럼 값을 설정합니다.
     */
    fun set(
        field: String,
        value: Any,
    ): UpdateValuesSpec

    /**
     * nullable 컬럼 값을 설정합니다.
     */
    fun set(
        field: String,
        value: Any?,
        type: Class<*>,
    ): UpdateValuesSpec

    /**
     * 여러 컬럼 값을 한번에 설정합니다.
     */
    fun set(parameters: Map<String, Any?>): UpdateValuesSpec

    /**
     * 설정된 값들의 맵을 반환합니다.
     */
    val values: Map<String, Any?>

    fun update(
        property: KProperty<*>,
        value: Any,
    ) = update(property.name, value)

    fun update(
        property: KProperty<*>,
        value: Any?,
        type: Class<*>,
    ) = update(property.name, value, type)

    fun set(
        property: KProperty<*>,
        value: Any,
    ) = set(property.name, value)

    fun set(
        property: KProperty<*>,
        value: Any?,
        type: Class<*>,
    ) = set(property.name, value, type)
}

/**
 * nullable 값을 타입 안전하게 업데이트합니다.
 */
inline fun <reified T: Any> SetterSpec.updateNullable(
    field: String,
    value: T? = null,
) = update(field, value, T::class.java)

inline fun <reified T: Any> SetterSpec.updateNullable(
    property: KProperty<*>,
    value: T? = null,
) = update(property, value, T::class.java)

inline fun <reified T: Any> SetterSpec.setNullable(
    field: String,
    value: T? = null,
) = set(field, value, T::class.java)

inline fun <reified T: Any> SetterSpec.setNullable(
    property: KProperty<*>,
    value: T? = null,
) = set(property, value, T::class.java)

/**
 * UPDATE 값 설정 및 실행 인터페이스
 */
interface UpdateValuesSpec: SetterSpec {
    /**
     * 설정 DSL을 사용하여 값을 설정합니다.
     */
    fun using(setters: SetterSpec.() -> Unit): UpdateValuesSpec

    /**
     * WHERE 절과 파라미터로 매칭 조건을 지정합니다.
     */
    fun matching(
        where: String? = null,
        whereParameters: Map<String, Any?>? = null,
    ): DatabaseClient.GenericExecuteSpec

    /**
     * [Query] 객체로 매칭 조건을 지정합니다.
     */
    fun matching(query: Query): DatabaseClient.GenericExecuteSpec = matching(query.sql, query.parameters)

    /**
     * UPDATE 결과를 조회합니다.
     */
    fun fetch(): FetchSpec<MutableMap<String, Any>> = matching().fetch()

    /**
     * UPDATE 실행 후 완료 신호를 반환합니다.
     */
    fun then(): Mono<Void> = matching().then()
}

internal class UpdateValuesSpecImpl(
    private val client: R2dbcClient,
    private val table: String,
): UpdateValuesSpec {
    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()
    override val Update: SetterSpec get() = this

    override fun update(
        field: String,
        value: Any,
    ): UpdateValuesSpec =
        apply {
            set(field, value)
        }

    override fun update(
        field: String,
        value: Any?,
        type: Class<*>,
    ): UpdateValuesSpec =
        apply {
            set(field, value, type)
        }

    override fun set(
        field: String,
        value: Any,
    ): UpdateValuesSpec =
        apply {
            values[field] = value
        }

    override fun set(
        field: String,
        value: Any?,
        type: Class<*>,
    ): UpdateValuesSpec =
        apply {
            values[field] = value.toParameter(type)
        }

    override fun set(parameters: Map<String, Any?>): UpdateValuesSpec =
        apply {
            parameters.forEach { (key, value) ->
                when (value) {
                    null -> setNullable<Any>(key, value)
                    else -> set(key, value)
                }
            }
        }

    override fun using(setters: SetterSpec.() -> Unit): UpdateValuesSpec =
        apply {
            setters()
        }

    override fun matching(
        where: String?,
        whereParameters: Map<String, Any?>?,
    ): DatabaseClient.GenericExecuteSpec {
        val updateParameters =
            values
                .map { (name, _) -> "$name = :$name" }
                .joinToString(", ")
        val sql = "UPDATE $table SET $updateParameters"

        val sqlToExecute = if (where != null) "$sql WHERE $where" else sql

        log.debug { "Update SQL=$sqlToExecute" }

        return client.databaseClient
            .sql(sqlToExecute)
            .bindMap(values + (whereParameters ?: emptyMap()))
    }
}
