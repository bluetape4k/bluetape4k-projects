package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.jackson.JacksonSerializer
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
 * Jackson을 사용해 JSON 문자열 기반 컬럼을 매핑하는 Exposed 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 값이 `String`/`ByteArray`면 [deserialize]로 복원하고, 그 외 타입은 `T` 캐스팅을 시도합니다.
 * - H2에서는 JSON 파라미터를 UTF-8 바이트 배열로 전달하고 PostgreSQL에서는 `?::json` 캐스트 마커를 사용합니다.
 * - 예상하지 못한 값 타입은 `error(...)`로 `IllegalStateException`을 발생시킵니다.
 * - 직렬화는 [serilaize] 호출 결과를 그대로 사용하며 별도 캐시를 두지 않습니다.
 *
 * ```kotlin
 * val type = JacksonColumnType<Map<String, Int>>(
 *     serilaize = { "{\"v\":${it.getValue("v")}}" },
 *     deserialize = { mapOf("v" to 1) }
 * )
 * val dbValue = type.notNullValueToDB(mapOf("v" to 1))
 * // dbValue == "{\"v\":1}"
 * ```
 *
 * @param serilaize `T` 값을 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T` 값으로 복원하는 함수입니다.
 */
open class JacksonColumnType<T: Any>(
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
            else      -> value as? T ?: error("Unexpected value $value of type ${value::class.qualifiedName}")
        }
    }

    override fun parameterMarker(value: T?): String = when (currentDialect) {
        is H2Dialect if value != null -> "? FORMAT JSON"
        is PostgreSQLDialect if value != null -> {
            val castType = if (usesBinaryFormat) "jsonb" else "json"
            "?::$castType"
        }
        else                          -> super.parameterMarker(value)
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

    override fun readObject(rs: RowApi, index: Int): Any? {
        return if (currentDialect is PostgreSQLDialect) {
            rs.getString(index)
        } else {
            super.readObject(rs, index)
        }
    }
}

/**
 * 사용자 직렬화 함수를 사용해 JSON 문자열 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [JacksonColumnType]입니다.
 * - 수신 [Table] 메타데이터를 mutate하여 컬럼을 추가하고, 추가된 [Column]을 반환합니다.
 * - [name]이 유효하지 않으면 Exposed 등록 단계에서 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = jackson<Map<String, Int>>("payload", { "{}" }, { emptyMap() })
 * }
 * // Docs.payload.name == "payload"
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serialize `T`를 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 복원하는 함수입니다.
 */
fun <T: Any> Table.jackson(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, JacksonColumnType(serialize, deserialize))


/**
 * [JacksonSerializer] 기반 기본 변환기를 사용해 JSON 문자열 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - [jacksonSerializer]의 문자열 변환 함수를 감싸 [jackson] 오버로드에 위임합니다.
 * - 역직렬화 결과가 `null`이면 `!!` 때문에 `NullPointerException`이 발생합니다.
 * - 반환되는 컬럼 인스턴스는 수신 [Table]에 등록된 컬럼과 동일합니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = jackson<Map<String, Int>>("payload")
 * }
 * // Docs.payload.columnType is JacksonColumnType<*>
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param jacksonSerializer 직렬화/역직렬화에 사용할 Jackson serializer입니다.
 */
inline fun <reified T: Any> Table.jackson(
    name: String,
    jacksonSerializer: JacksonSerializer = DefaultJacksonSerializer,
): Column<T> =
    jackson(
        name,
        serialize = { jacksonSerializer.serializeAsString(it) },
        deserialize = { jacksonSerializer.deserializeFromString<T>(it)!! }
    )
