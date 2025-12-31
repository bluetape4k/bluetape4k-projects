package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.jackson3.JacksonSerializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * Jackson을 사용하여 객체를 JSONB 타입으로 직렬화/역직렬화하는 Exposed 컬럼 타입입니다.
 *
 * @param T 직렬화/역직렬화할 객체의 타입
 * @param serialize 객체를 JSON 문자열로 변환하는 함수
 * @param deserialize JSON 문자열을 객체로 변환하는 함수
 */
class JacksonBColumnType<T: Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
): JacksonColumnType<T>(serialize, deserialize) {

    /**
     * 이 컬럼 타입이 바이너리 포맷을 사용하는지 여부를 나타냅니다.
     */
    override val usesBinaryFormat: Boolean = true

    /**
     * 현재 DB Dialect에 맞는 JSONB 타입의 SQL 타입 문자열을 반환합니다.
     */
    override fun sqlType(): String = when (val dialect = currentDialect) {
        is H2Dialect -> (currentDialect as H2Dialect).originalDataTypeProvider.jsonBType()
        else -> dialect.dataTypeProvider.jsonBType()
    }
}

/**
 * Table에 JSONB 컬럼을 등록합니다.
 *
 * @param T 직렬화/역직렬화할 객체의 타입
 * @param name 컬럼명
 * @param serialize 객체를 JSON 문자열로 변환하는 함수
 * @param deserialize JSON 문자열을 객체로 변환하는 함수
 * @return 등록된 컬럼
 */
fun <T: Any> Table.jacksonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, JacksonBColumnType(serialize, deserialize))

/**
 * JacksonSerializer를 사용하여 Table에 JSONB 컬럼을 등록합니다.
 *
 * @param T 직렬화/역직렬화할 객체의 타입
 * @param name 컬럼명
 * @param serializer JacksonSerializer 인스턴스 (기본값: DefaultJacksonSerializer)
 * @return 등록된 컬럼
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
