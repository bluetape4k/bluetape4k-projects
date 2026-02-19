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

/**
 * R2DBC INSERT 연산을 시작합니다.
 *
 * @return [InsertIntoSpec] 인스턴스
 */
fun R2dbcClient.insert(): InsertIntoSpec = InsertIntoSpecImpl(this)

/**
 * INSERT 연산의 대상 테이블을 지정하는 인터페이스
 */
interface InsertIntoSpec {
    /**
     * 삽입할 테이블 이름을 지정합니다.
     *
     * @param table 테이블 이름
     * @return [InsertValuesSpec] 인스턴스
     */
    fun into(table: String): InsertValuesSpec

    /**
     * 삽입할 테이블 이름과 자동 생성되는 키 컬럼을 지정합니다.
     *
     * @param table 테이블 이름
     * @param idColumn 자동 생성되는 키 컬럼 이름
     * @return [InsertValuesKeySpec] 인스턴스
     */
    fun into(
        table: String,
        idColumn: String,
    ): InsertValuesKeySpec

    /**
     * 엔티티 타입으로 삽입할 테이블을 지정합니다.
     *
     * @param T 엔티티 타입
     * @param type 엔티티 클래스
     * @return [ReactiveInsertOperation.ReactiveInsert] 인스턴스
     */
    fun <T> into(type: Class<T>): ReactiveInsertOperation.ReactiveInsert<T>
}

/**
 * 제네릭 타입으로 INSERT 대상을 지정합니다.
 */
inline fun <reified T: Any> InsertIntoSpec.into(): ReactiveInsertOperation.ReactiveInsert<T> = into(T::class.java)

/**
 * 엔티티를 생성하여 삽입하고 결과를 기다립니다.
 */
suspend inline fun <T: Any> ReactiveInsertOperation.TerminatingInsert<T>.usingAwaitSingle(supplier: () -> T): T =
    using(supplier.invoke()).awaitSingle()

internal class InsertIntoSpecImpl(
    private val client: io.bluetape4k.r2dbc.R2dbcClient,
): InsertIntoSpec {
    override fun into(table: String): InsertValuesSpec = InsertValuesSpecImpl(client, table)

    override fun into(
        table: String,
        idColumn: String,
    ): InsertValuesKeySpec = InsertValuesKeySpecImpl(client, table, idColumn)

    override fun <T> into(type: Class<T>): ReactiveInsertOperation.ReactiveInsert<T> =
        client.entityTemplate.insert(type)
}

/**
 * INSERT 값 설정 인터페이스
 */
interface InsertValuesSpec {
    /**
     * 컬럼에 값을 설정합니다.
     *
     * @param field 컬럼 이름
     * @param value 값
     * @return [InsertValuesSpec] 인스턴스
     */
    fun value(
        field: String,
        value: Any,
    ): InsertValuesSpec

    /**
     * 컬럼에 nullable 값을 설정합니다.
     *
     * @param field 컬럼 이름
     * @param value 값 (null 허용)
     * @param type 값의 타입
     * @return [InsertValuesSpec] 인스턴스
     */
    fun value(
        field: String,
        value: Any?,
        type: Class<*>,
    ): InsertValuesSpec

    fun value(
        property: KProperty<*>,
        value: Any,
    ): InsertValuesSpec = value(property.name, value)

    fun value(
        property: KProperty<*>,
        value: Any?,
        type: Class<*>,
    ): InsertValuesSpec = value(property.name, value, type)

    /**
     * 컬럼에 null 값을 설정합니다.
     */
    fun nullValue(field: String): InsertValuesSpec

    /**
     * 컬럼에 타입이 지정된 null 값을 설정합니다.
     */
    fun nullValue(
        field: String,
        type: Class<*>,
    ): InsertValuesSpec

    fun nullValue(property: KProperty<*>): InsertValuesSpec = nullValue(property.name)

    fun nullValue(
        property: KProperty<*>,
        type: Class<*>,
    ): InsertValuesSpec = nullValue(property.name, type)

    /**
     * INSERT 결과를 조회합니다.
     */
    fun fetch(): FetchSpec<out Any>

    /**
     * INSERT 실행 후 완료 신호를 반환합니다.
     */
    fun then(): Mono<Void>

    /**
     * INSERT를 실행하고 완료될 때까지 대기합니다.
     */
    suspend fun await()

    /**
     * 설정된 값들의 맵을 반환합니다.
     */
    val values: Map<String, Any?>
}

/**
 * nullable 값을 타입 안전하게 설정합니다.
 */
inline fun <reified T: Any> InsertValuesSpec.valueNullable(
    field: String,
    value: T? = null,
) = value(field, value, T::class.java)

inline fun <reified T: Any> InsertValuesSpec.valueNullable(
    property: KProperty<*>,
    value: T? = null,
) = value(property, value, T::class.java)

internal class InsertValuesSpecImpl(
    private val client: io.bluetape4k.r2dbc.R2dbcClient,
    private val table: String,
): InsertValuesSpec {
    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()

    override fun value(
        field: String,
        value: Any,
    ): InsertValuesSpec =
        apply {
            values[field] = value
        }

    override fun value(
        field: String,
        value: Any?,
        type: Class<*>,
    ): InsertValuesSpec =
        apply {
            values[field] = value.toParameter(type)
        }

    override fun nullValue(field: String): InsertValuesSpec =
        apply {
            values[field] = null
        }

    override fun nullValue(
        field: String,
        type: Class<*>,
    ): InsertValuesSpec =
        apply {
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

    override fun fetch(): FetchSpec<out Any> = execute().fetch()

    override fun then(): Mono<Void> = execute().then()

    override suspend fun await() {
        execute().then().awaitFirstOrNull()
    }
}

/**
 * 자동 생성 키를 반환하는 INSERT 값 설정 인터페이스
 */
interface InsertValuesKeySpec {
    /**
     * 컬럼에 값을 설정합니다.
     */
    fun value(
        field: String,
        value: Any,
    ): InsertValuesKeySpec

    /**
     * 컬럼에 nullable 값을 설정합니다.
     */
    fun value(
        field: String,
        value: Any?,
        type: Class<*>,
    ): InsertValuesKeySpec

    fun value(
        property: KProperty<*>,
        value: Any,
    ) = value(property.name, value)

    fun value(
        property: KProperty<*>,
        value: Any?,
        type: Class<*>,
    ) = value(property.name, value, type)

    /**
     * 컬럼에 null 값을 설정합니다.
     */
    fun nullValue(field: String): InsertValuesKeySpec

    /**
     * 컬럼에 타입이 지정된 null 값을 설정합니다.
     */
    fun nullValue(
        field: String,
        type: Class<*>,
    ): InsertValuesKeySpec

    fun nullValue(property: KProperty<*>) = nullValue(property.name)

    fun nullValue(
        property: KProperty<*>,
        type: Class<*>,
    ) = nullValue(property.name, type)

    /**
     * 자동 생성된 키(Int)를 조회합니다.
     */
    fun fetch(): RowsFetchSpec<Int>

    /**
     * 자동 생성된 키(Int)를 기다립니다.
     */
    suspend fun awaitOne(): Int

    /**
     * 자동 생성된 키(Long)를 조회합니다.
     */
    fun fetchLong(): RowsFetchSpec<Long>

    /**
     * 자동 생성된 키(Long)를 기다립니다.
     */
    suspend fun awaitOneLong(): Long

    /**
     * INSERT 실행 후 완료 신호를 반환합니다.
     */
    fun then(): Mono<Void>

    /**
     * 설정된 값들의 맵을 반환합니다.
     */
    val values: Map<String, Any?>
}

/**
 * nullable 값을 타입 안전하게 설정합니다.
 */
inline fun <reified T: Any> InsertValuesKeySpec.valueNullable(
    field: String,
    value: T? = null,
) = value(field, value, T::class.java)

inline fun <reified T: Any> InsertValuesKeySpec.valueNullable(
    property: KProperty<*>,
    value: T? = null,
) = value(property, value, T::class.java)

internal class InsertValuesKeySpecImpl(
    private val client: R2dbcClient,
    private val table: String,
    private val idColumn: String,
): InsertValuesKeySpec {
    companion object: KLogging()

    override val values = mutableMapOf<String, Any?>()

    override fun value(
        field: String,
        value: Any,
    ): InsertValuesKeySpec =
        apply {
            values[field] = value
        }

    override fun value(
        field: String,
        value: Any?,
        type: Class<*>,
    ): InsertValuesKeySpec =
        apply {
            values[field] = value.toParameter(type)
        }

    override fun nullValue(field: String): InsertValuesKeySpec =
        apply {
            values[field] = null
        }

    override fun nullValue(
        field: String,
        type: Class<*>,
    ): InsertValuesKeySpec =
        apply {
            values[field] = type.toParameter()
        }

    override fun fetch(): RowsFetchSpec<Int> {
        val executeSpec = executeSpec()
        return executeSpec
            .filter { s -> s.returnGeneratedValues(idColumn) }
            .map { row -> row.int(idColumn) }
    }

    override suspend fun awaitOne(): Int = fetch().awaitOne()

    override fun fetchLong(): RowsFetchSpec<Long> {
        val executeSpec = executeSpec()
        return executeSpec
            .filter { s -> s.returnGeneratedValues(idColumn) }
            .map { row -> row.long(idColumn) }
    }

    override suspend fun awaitOneLong(): Long = fetchLong().awaitOne()

    override fun then(): Mono<Void> = executeSpec().then()

    private fun executeSpec(): DatabaseClient.GenericExecuteSpec {
        if (values.isEmpty()) {
            error("No value specified")
        }
        val names = values.keys.joinToString(", ")
        val namedArguments = values.keys.map { ":$it" }.joinToString(", ")
        val sql = "INSERT INTO $table ($names) VALUES ($namedArguments)"
        log.debug { "Insert sql=$sql" }

        return client.databaseClient
            .sql(sql)
            .bindMap(values)
    }
}
