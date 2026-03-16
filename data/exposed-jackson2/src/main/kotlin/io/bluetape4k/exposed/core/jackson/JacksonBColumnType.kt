package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.jackson.JacksonSerializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * Jackson을 사용해 JSONB 컬럼을 매핑하는 Exposed 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - [usesBinaryFormat]은 항상 `true`이며 SQL 타입은 Dialect의 `jsonBType()`으로 결정됩니다.
 * - SQLite에서는 파라미터 마커를 `JSONB(?)`로 감싸고 기본값 문자열도 `JSONB(...)` 형태로 생성합니다.
 * - [castToJsonFormat]이 `true`일 때 SQLite에서만 [needsBinaryFormatCast]가 `true`가 됩니다.
 * - 실제 직렬화/역직렬화 동작은 상위 [JacksonColumnType] 구현을 따릅니다.
 *
 * ```kotlin
 * val type = JacksonBColumnType<Map<String, Int>>({ "{}" }, { emptyMap() })
 * val isBinary = type.usesBinaryFormat
 * // isBinary == true
 * ```
 *
 * @param serialize `T`를 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 복원하는 함수입니다.
 * @param castToJsonFormat SQLite에서 JSONB 캐스트 플래그를 사용할지 여부입니다.
 */
class JacksonBColumnType<T: Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
    castToJsonFormat: Boolean = false,
): JacksonColumnType<T>(serialize, deserialize) {

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
 * 사용자 직렬화 함수를 사용해 JSONB 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [JacksonBColumnType]입니다.
 * - 수신 [Table] 메타데이터를 mutate하여 컬럼을 추가하고, 추가된 [Column]을 반환합니다.
 * - [name]이 유효하지 않으면 Exposed 컬럼 등록 단계에서 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = jacksonb<Map<String, Int>>("payload", { "{}" }, { emptyMap() })
 * }
 * // Docs.payload.name == "payload"
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serialize `T`를 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 복원하는 함수입니다.
 */
fun <T: Any> Table.jacksonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, JacksonBColumnType(serialize, deserialize))

/**
 * [JacksonSerializer] 기반 기본 변환기를 사용해 JSONB 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - [serializer]의 직렬화/역직렬화 함수를 감싸 [jacksonb] 오버로드에 위임합니다.
 * - 역직렬화 결과가 `null`이면 `!!` 때문에 `NullPointerException`이 발생합니다.
 * - 반환되는 컬럼 인스턴스는 수신 [Table]에 등록된 컬럼과 동일합니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = jacksonb<Map<String, Int>>("payload")
 * }
 * // Docs.payload.columnType is JacksonBColumnType<*>
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serializer 직렬화/역직렬화에 사용할 Jackson serializer입니다.
 */
inline fun <reified T: Any> Table.jacksonb(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): Column<T> =
    jacksonb(
        name,
        serialize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserializeFromString<T>(it)!! }
    )
