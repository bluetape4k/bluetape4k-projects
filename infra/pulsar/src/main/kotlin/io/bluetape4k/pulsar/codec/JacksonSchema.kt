package io.bluetape4k.pulsar.codec

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.Jackson
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.common.schema.SchemaInfo
import org.apache.pulsar.common.schema.SchemaType

/**
 * Jackson2 기반 Pulsar [Schema] 구현체를 생성합니다.
 *
 * ⚠️ **compileOnly 의존성 주의**: 사용 모듈의 `build.gradle.kts`에서
 * `implementation(project(":bluetape4k-jackson2"))`를 반드시 선언해야 합니다.
 * 누락 시 런타임에 `NoClassDefFoundError`가 발생합니다.
 *
 * ```kotlin
 * val schema = jacksonSchema<Order>()
 * val producer = client.producer(schema) { topic("orders") }
 * ```
 *
 * @param type 직렬화/역직렬화 대상 클래스
 * @param mapper Jackson2 [ObjectMapper] (기본값: [Jackson.defaultJsonMapper])
 * @return Pulsar [Schema] 구현체
 */
fun <T> jacksonSchema(
    type: Class<T>,
    mapper: ObjectMapper = Jackson.defaultJsonMapper,
): Schema<T> = JacksonSchemaImpl(type, mapper)

/**
 * reified 타입 파라미터를 활용한 [jacksonSchema] 편의 함수.
 *
 * ```kotlin
 * val schema = jacksonSchema<Order>()
 * ```
 */
inline fun <reified T> jacksonSchema(
    mapper: ObjectMapper = Jackson.defaultJsonMapper,
): Schema<T> = jacksonSchema(T::class.java, mapper)

private class JacksonSchemaImpl<T>(
    private val type: Class<T>,
    private val mapper: ObjectMapper,
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

    override fun clone(): Schema<T> = JacksonSchemaImpl(type, mapper)
}
