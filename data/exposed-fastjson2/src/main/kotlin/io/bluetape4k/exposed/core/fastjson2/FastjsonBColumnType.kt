package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * Fastjson2를 사용해 JSONB 컬럼을 매핑하는 Exposed 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - [usesBinaryFormat]은 항상 `true`이며, SQL 타입은 Dialect별 `jsonBType()`을 사용합니다.
 * - SQLite에서는 바인딩 마커를 `JSONB(?)`로 감싸고 기본값 문자열도 `JSONB(...)` 형태로 생성합니다.
 * - [castToJsonFormat]이 `true`일 때 SQLite에서만 [needsBinaryFormatCast]가 활성화됩니다.
 * - 직렬화/역직렬화 동작은 상위 [FastjsonColumnType] 구현을 그대로 따릅니다.
 *
 * ```kotlin
 * val type = FastjsonBColumnType<Map<String, Int>>({ "{}" }, { emptyMap() })
 * val isBinary = type.usesBinaryFormat
 * // isBinary == true
 * ```
 *
 * @param serialize `T`를 JSON 문자열로 직렬화하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 역직렬화하는 함수입니다.
 * @param castToJsonFormat SQLite에서 JSONB 캐스트 문자열을 강제할지 여부입니다.
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
 * Fastjson2 사용자 직렬화 함수를 이용해 JSONB 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [FastjsonBColumnType]이며 DB의 JSONB 타입을 사용합니다.
 * - 수신 [Table]에 컬럼을 추가하므로 테이블 메타데이터가 변경됩니다.
 * - [name]이 유효하지 않으면 Exposed 컬럼 등록 과정에서 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = fastjsonb<Map<String, Int>>("payload", { "{}" }, { emptyMap() })
 * }
 * // Docs.payload.name == "payload"
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serialize `T`를 JSON 문자열로 변환하는 함수입니다.
 * @param deserialize JSON 문자열을 `T`로 복원하는 함수입니다.
 */
fun <T: Any> Table.fastjsonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, FastjsonBColumnType(serialize, deserialize))

/**
 * [FastjsonSerializer]를 사용해 JSONB 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - [serializer]의 문자열 직렬화 함수를 감싸 [fastjsonb] 오버로드에 위임합니다.
 * - 역직렬화 결과가 `null`이면 `!!` 때문에 `NullPointerException`이 발생합니다.
 * - 반환된 [Column]은 수신 [Table]에 등록된 동일 컬럼 인스턴스입니다.
 *
 * ```kotlin
 * object Docs: Table("docs") {
 *     val payload = fastjsonb<Map<String, Int>>("payload")
 * }
 * // Docs.payload.columnType is FastjsonBColumnType<*>
 * ```
 *
 * @param name 컬럼 이름입니다.
 * @param serializer 직렬화/역직렬화에 사용할 Fastjson2 serializer입니다.
 */
inline fun <reified T: Any> Table.fastjsonb(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): Column<T> =
    fastjsonb(
        name,
        serialize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserializeFromString<T>(it)!! }
    )
