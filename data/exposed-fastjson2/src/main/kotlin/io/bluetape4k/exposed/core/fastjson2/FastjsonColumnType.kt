package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.JsonColumnMarker
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.core.statements.api.RowApi
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * Fastjson2를 사용해 JSON 문자열 기반 컬럼을 매핑하는 Exposed 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 값이 `String` 또는 `ByteArray`이면 [deserialize]로 복원하고, 그 외 타입은 `T` 캐스팅을 시도합니다.
 * - H2에서는 JSON 바인딩 시 바이트 배열로 전달하고, PostgreSQL에서는 `?::json` 캐스트 마커를 사용합니다.
 * - 예상하지 못한 DB 타입이 들어오면 `error(...)`로 `IllegalStateException`이 발생합니다.
 * - 직렬화는 [serilaize]를 그대로 호출하며, 별도 캐시 없이 호출 시점에 문자열을 새로 생성합니다.
 *
 * ```kotlin
 * val type = FastjsonColumnType<Map<String, Int>>(
 *     serilaize = { "{\"v\":${it.getValue("v")}}" },
 *     deserialize = { mapOf("v" to 1) }
 * )
 * val dbValue = type.notNullValueToDB(mapOf("v" to 1))
 * // dbValue == "{\"v\":1}"
 * ```
 *
 * @param serilaize `T` 값을 JSON 문자열로 변환합니다.
 * @param deserialize JSON 문자열을 `T` 값으로 복원합니다.
 */
open class FastjsonColumnType<T: Any>(
    val serilaize: (T) -> String,
    val deserialize: (String) -> T,
): ColumnType<T>(), JsonColumnMarker {

    override val usesBinaryFormat: Boolean = false

    override val needsBinaryFormatCast: Boolean = false

    override fun sqlType(): String = currentDialect.dataTypeProvider.jsonType()

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T? {
        return when (value) {
            is String -> deserialize(value)
            is ByteArray -> deserialize(value.toUtf8String())
            else -> value as? T ?: error("Unexpected value $value of type ${value::class.qualifiedName}")
        }
    }

    override fun parameterMarker(value: T?): String = when (currentDialect) {
        is H2Dialect if value != null -> "? FORMAT JSON"
        is PostgreSQLDialect if value != null -> {
            val castType = if (usesBinaryFormat) "jsonb" else "json"
            "?::$castType"
        }
        else -> super.parameterMarker(value)
    }

    override fun notNullValueToDB(value: T): Any = serilaize(value)

    override fun valueToString(value: T?): String = when (value) {
        is Iterable<*> -> nonNullValueToString(value)
        else -> super.valueToString(value)
    }

    override fun nonNullValueToString(value: T): String = when (currentDialect) {
        is H2Dialect -> "JSON '${notNullValueToDB(value)}'"
        else -> "'${notNullValueToDB(value)}'"
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue = when (currentDialect) {
            is H2Dialect -> (value as? String)?.toUtf8Bytes()
            else -> value
        }
        super.setParameter(stmt, index, parameterValue)
    }

    override fun readObject(rs: RowApi, index: Int): Any? = when (currentDialect) {
        is PostgreSQLDialect -> rs.getString(index)
        else -> super.readObject(rs, index)
    }
}

/**
 * Fastjson2 직렬화 함수를 사용해 JSON 문자열 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 생성된 컬럼은 DB의 JSON 타입(`jsonType()`)을 사용하며, 저장 시 문자열 JSON을 기록합니다.
 * - 수신 [Table]을 mutate하여 컬럼 메타데이터를 등록하고, 등록된 [Column]을 반환합니다.
 * - [name]이 빈 문자열이면 Exposed의 컬럼 등록 과정에서 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = fastjson<Map<String, Int>>("payload", { "{}" }, { emptyMap() })
 * }
 * // Docs.payload.name == "payload"
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serialize `T`를 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 복원하는 함수입니다.
 */
fun <T: Any> Table.fastjson(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, FastjsonColumnType(serialize, deserialize))


/**
 * [FastjsonSerializer] 기반 기본 변환기를 사용해 JSON 문자열 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - [fastjsonSerializer]의 `serializeAsString`/`deserializeFromString`을 래핑해 [fastjson]에 위임합니다.
 * - 역직렬화 결과가 `null`이면 `!!` 때문에 `NullPointerException`이 발생합니다.
 * - 컬럼 등록 시 [Table]은 mutate되고, 반환되는 [Column]은 동일 테이블 메타데이터에 연결됩니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = fastjson<Map<String, Int>>("payload")
 * }
 * // Docs.payload.columnType is FastjsonColumnType<*>
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param fastjsonSerializer 직렬화/역직렬화에 사용할 Fastjson2 serializer입니다.
 */
inline fun <reified T: Any> Table.fastjson(
    name: String,
    fastjsonSerializer: FastjsonSerializer = FastjsonSerializer.Default,
): Column<T> =
    fastjson(
        name,
        { fastjsonSerializer.serializeAsString(it) },
        { fastjsonSerializer.deserializeFromString<T>(it)!! }
    )
