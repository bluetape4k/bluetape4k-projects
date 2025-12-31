package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
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

open class JacksonColumnType<T: Any>(
    /**
     * [T] 타입의 객체를 JSON 문자열로 인코딩합니다.
     */
    val serilaize: (T) -> String,

    /**
     * JSON 문자열을 [T] 타입의 객체로 디코딩합니다.
     */
    val deserialize: (String) -> T,
): ColumnType<T>(), JsonColumnMarker {

    override val usesBinaryFormat: Boolean = false

    override fun sqlType(): String = currentDialect.dataTypeProvider.jsonType()

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T? {
        return when (value) {
            is String -> deserialize(value)
            is ByteArray -> deserialize(value.toUtf8String())
            else -> value as? T ?: error("Unexpected value $value of type ${value::class.qualifiedName}")
        }
    }

    override fun parameterMarker(value: T?): String = when {
        currentDialect is H2Dialect && value != null -> "? FORMAT JSON"
        currentDialect is PostgreSQLDialect && value != null -> {
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

    override fun readObject(rs: RowApi, index: Int): Any? {
        return if (currentDialect is PostgreSQLDialect) {
            rs.getString(index)
        } else {
            super.readObject(rs, index)
        }
    }
}

/**
 * JSON 데이터를 저장할 컬럼을 생성합니다.
 *
 * **Note**: 이 컬럼은 JSON 포맷의 문자열로 저장합니다.
 * if the vendor only supports 1 format, the default JSON type format.
 * If JSON must be stored in binary format, and the vendor supports this, please use `jsonb()` instead.
 *
 * @param name 컬럼 이름
 * @param serialize [T] 타입의 객체를 JSON 문자열로 인코딩하는 함수
 * @param deserialize JSON 문자열을 [T] 타입의 객체로 디코딩하는 함수
 */
fun <T: Any> Table.jackson(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, JacksonColumnType(serialize, deserialize))


/**
 * JSON 데이터를 저장할 컬럼을 생성합니다.
 *
 * @param name 컬럼 이름
 * @param jacksonSerializer JSON 직렬화/역직렬화에 사용할 [JacksonSerializer]
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
