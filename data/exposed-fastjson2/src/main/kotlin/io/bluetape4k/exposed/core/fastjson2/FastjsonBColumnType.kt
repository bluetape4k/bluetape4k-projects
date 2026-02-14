package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.fastjson2.deserialize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * Fastjson2 기반 JSONB 컬럼 타입입니다.
 *
 * DB 벤더가 JSONB를 지원하면 바이너리 JSON 포맷을 사용하고,
 * SQLite 에서는 `JSONB(...)` 래퍼를 사용해 값을 전달합니다.
 */
class FastjsonBColumnType<T: Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
    castToJsonFormat: Boolean = false,
): FastjsonColumnType<T>(serialize, deserialize) {

    override val usesBinaryFormat: Boolean = true

    override val needsBinaryFormatCast: Boolean = castToJsonFormat
        get() = field && currentDialect is SQLiteDialect

    override fun sqlType(): String = when (val dialect = currentDialect) {
        is H2Dialect -> dialect.originalDataTypeProvider.jsonBType()
        else -> dialect.dataTypeProvider.jsonBType()
    }

    override fun parameterMarker(value: T?): String = if (currentDialect is SQLiteDialect) {
        "JSONB(?)"
    } else {
        super.parameterMarker(value)
    }

    override fun nonNullValueAsDefaultString(value: T): String {
        return when (currentDialect) {
            is SQLiteDialect -> "(JSONB(${nonNullValueToString(value)}))"
            else -> super.nonNullValueAsDefaultString(value)
        }
    }
}

/**
 * Fastjson2 직렬화 함수를 이용한 JSONB 컬럼을 생성합니다.
 */
fun <T: Any> Table.fastjsonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, FastjsonBColumnType(serialize, deserialize))

/**
 * [FastjsonSerializer]를 이용해 JSONB 컬럼을 생성합니다.
 */
inline fun <reified T: Any> Table.fastjsonb(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): Column<T> =
    fastjsonb(
        name,
        serialize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserialize<T>(it)!! }
    )
