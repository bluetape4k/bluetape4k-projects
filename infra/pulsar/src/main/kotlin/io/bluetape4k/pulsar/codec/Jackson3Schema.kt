package io.bluetape4k.pulsar.codec

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.toStringBuilder
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.common.schema.SchemaInfo
import org.apache.pulsar.common.schema.SchemaType
import io.bluetape4k.jackson3.Jackson as Jackson3
import tools.jackson.databind.ObjectMapper as Jackson3ObjectMapper

/**
 * Jackson3 기반 Pulsar [Schema] 구현체를 생성합니다.
 *
 * ⚠️ **compileOnly 의존성 주의**: 사용 모듈의 `build.gradle.kts`에서
 * `implementation(project(":bluetape4k-jackson3"))`를 반드시 선언해야 합니다.
 * 누락 시 런타임에 `NoClassDefFoundError`가 발생합니다.
 *
 * Jackson3는 `tools.jackson.databind.ObjectMapper`를 사용합니다.
 * Jackson2(`com.fasterxml.jackson.databind.ObjectMapper`)와 바이너리 호환되지 않으므로
 * 런타임에 둘 중 하나만 사용해야 합니다.
 *
 * ```kotlin
 * val schema = jackson3Schema<Order>(Jackson3.defaultJsonMapper)
 * val producer = client.producer(schema) { topic("orders") }
 * ```
 *
 * @param type 직렬화/역직렬화 대상 클래스
 * @param mapper Jackson3 [Jackson3ObjectMapper]
 * @return Pulsar [Schema] 구현체
 */
fun <T> jackson3SchemaOf(
    type: Class<T>,
    mapper: Jackson3ObjectMapper = Jackson3.defaultJsonMapper,
): Schema<T> =
    Jackson3SchemaImpl(type, mapper)

/**
 * reified 타입 파라미터를 활용한 [jackson3SchemaOf] 편의 함수.
 *
 * ```kotlin
 * val schema = jackson3Schema<Order>(Jackson3.defaultJsonMapper)
 * ```
 */
inline fun <reified T> jackson3SchemaOf(mapper: Jackson3ObjectMapper = Jackson3.defaultJsonMapper): Schema<T> =
    jackson3SchemaOf(T::class.java, mapper)

private class Jackson3SchemaImpl<T>(
    private val type: Class<T>,
    private val mapper: Jackson3ObjectMapper,
    cachedInfo: SchemaInfo? = null,
): Schema<T> {

    companion object: KLogging()

    // Schema.JSON(type)에서 브로커 호환성 검증용 schema bytes를 가져오고, name은 type에서 직접 설정.
    // clone() 시 재계산을 피하기 위해 생성된 SchemaInfo를 재사용한다.
    private val info: SchemaInfo = cachedInfo ?: Schema.JSON(type).schemaInfo.let { base ->
        val name = type.simpleName.takeIf { it.isNotBlank() } ?: type.name
        SchemaInfo.builder()
            .name(name)
            .type(SchemaType.JSON)
            .schema(base.schema)
            .properties(base.properties)
            .build()
    }

    override fun encode(message: T): ByteArray = mapper.writeValueAsBytes(message)

    override fun decode(bytes: ByteArray): T = mapper.readValue(bytes, type)

    override fun getSchemaInfo(): SchemaInfo = info

    override fun clone(): Schema<T> = Jackson3SchemaImpl(type, mapper, info)

    override fun toString(): String {
        return ToStringBuilder(this)
            .add("type", type)
            .add("info", info.toStringBuilder().toString())
            .toString()
    }
}
