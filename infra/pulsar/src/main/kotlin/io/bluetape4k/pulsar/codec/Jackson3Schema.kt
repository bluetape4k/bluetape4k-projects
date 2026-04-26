package io.bluetape4k.pulsar.codec

import io.bluetape4k.jackson3.Jackson as Jackson3
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.common.schema.SchemaInfo
import org.apache.pulsar.common.schema.SchemaType
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
fun <T> jackson3Schema(
    type: Class<T>,
    mapper: Jackson3ObjectMapper = Jackson3.defaultJsonMapper,
): Schema<T> = Jackson3SchemaImpl(type, mapper)

/**
 * reified 타입 파라미터를 활용한 [jackson3Schema] 편의 함수.
 *
 * ```kotlin
 * val schema = jackson3Schema<Order>(Jackson3.defaultJsonMapper)
 * ```
 */
inline fun <reified T> jackson3Schema(
    mapper: Jackson3ObjectMapper = Jackson3.defaultJsonMapper,
): Schema<T> = jackson3Schema(T::class.java, mapper)

private class Jackson3SchemaImpl<T>(
    private val type: Class<T>,
    private val mapper: Jackson3ObjectMapper,
) : Schema<T> {

    // Schema.JSON(type)에서 브로커 호환성 검증용 schema bytes를 가져오고, name은 type에서 직접 설정
    private val info: SchemaInfo = Schema.JSON(type).schemaInfo.let { base ->
        val name = type.simpleName?.takeIf { it.isNotBlank() } ?: type.name
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

    override fun clone(): Schema<T> = Jackson3SchemaImpl(type, mapper)
}
