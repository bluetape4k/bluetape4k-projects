package io.bluetape4k.jackson

import com.example.disallowed.DisallowedTypedPayload
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.jackson.uuid.JsonUuidModule
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.io.Serializable

class JacksonTest {

    companion object: KLogging()

    @Test
    fun `classpath에 있는 모듈을 자동으로 등록하기`() {
        val mapper = Jackson.defaultJsonMapper

        mapper.registeredModuleIds.forEach { moduleId ->
            println(moduleId)
        }
        mapper.registeredModuleIds.size shouldBeGreaterThan 0

        val modules = ObjectMapper.findModules()
        mapper.registeredModuleIds shouldContainAll modules.map { it.typeId.toString() }

        // classpath 에 있는 JsonUuidModule 을 자동으로 등록했다
        mapper.registeredModuleIds shouldContain JsonUuidModule::class.qualifiedName
    }

    @Test
    fun `defaultJsonMapper는 lazy singleton이다`() {
        val mapper1 = Jackson.defaultJsonMapper
        val mapper2 = Jackson.defaultJsonMapper
        (mapper1 === mapper2).shouldBeTrue()
    }

    @Test
    fun `createDefaultJsonMapper는 호출 시마다 새 인스턴스를 반환한다`() {
        val mapper1 = Jackson.createDefaultJsonMapper()
        val mapper2 = Jackson.createDefaultJsonMapper()
        (mapper1 !== mapper2).shouldBeTrue()
    }

    @Test
    fun `createTypedJsonMapper - 허용 패키지 지정 시 정상 생성`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.")
        mapper.shouldNotBeNull()
        // 생성된 매퍼로 간단한 직렬화 확인
        val json = mapper.writeValueAsString(mapOf("key" to "value"))
        json.shouldNotBeNull()
        (json.contains("key")).shouldBeTrue()
    }

    @Test
    fun `createTypedJsonMapper - 빈 패키지 목록이면 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            Jackson.createTypedJsonMapper()
        }
    }

    @Test
    fun `createTypedJsonMapper - 여러 패키지 허용 가능`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.", "com.example.")
        mapper.shouldNotBeNull()
    }

    @Test
    fun `createTypedJsonMapper - allowed package payload round trips with property type info`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.jackson.")
        val original = TypedPayloadEnvelope(AllowedTypedPayload("safe"))

        val json = mapper.writeValueAsString(original)
        val restored = mapper.readValue(json, TypedPayloadEnvelope::class.java)

        json shouldContain "\"@class\""
        val payload = restored.payload.shouldBeInstanceOf<AllowedTypedPayload>()
        payload.value shouldBeEqualTo "safe"
    }

    @Test
    fun `createTypedJsonMapper - denied package payload is rejected`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.jackson.")
        val json = """
            {
              "payload": {
                "@class": "${DisallowedTypedPayload::class.qualifiedName}",
                "value": "blocked"
              }
            }
        """.trimIndent()

        assertFailsWith<InvalidTypeIdException> {
            mapper.readValue(json, TypedPayloadEnvelope::class.java)
        }
    }

    @Test
    fun `createTypedJsonMapper - denied root payload is rejected`() {
        val mapper = Jackson.createTypedJsonMapper("io.bluetape4k.jackson.")
        val json = """
            {
              "@class": "${DisallowedTypedPayload::class.qualifiedName}",
              "value": "blocked"
            }
        """.trimIndent()

        assertFailsWith<InvalidTypeIdException> {
            mapper.readValue(json, Any::class.java)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy typedJsonMapper는 명시적 migration failure로 차단한다`() {
        val exception = assertFailsWith<UnsupportedOperationException> {
            Jackson.typedJsonMapper
        }

        exception.message shouldContain "createTypedJsonMapper"
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy prettyTypedJsonWriter는 명시적 migration failure로 차단한다`() {
        val exception = assertFailsWith<UnsupportedOperationException> {
            Jackson.prettyTypedJsonWriter
        }

        exception.message shouldContain "createTypedJsonMapper"
    }

    @Test
    fun `createDefaultJsonMapper의 legacy type info 옵션은 명시적 migration failure로 차단한다`() {
        val exception = assertFailsWith<UnsupportedOperationException> {
            Jackson.createDefaultJsonMapper(needTypeInfo = true)
        }

        exception.message shouldContain "createTypedJsonMapper"
    }

    @Test
    fun `registeredModuleIdList - 등록된 모듈 ID를 List로 반환`() {
        val ids = Jackson.defaultJsonMapper.registeredModuleIdList()
        ids.shouldNotBeNull()
        (ids.isNotEmpty()).shouldBeTrue()
    }
}

internal data class TypedPayloadEnvelope(
    val payload: Any,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 8658783199380213352L
    }
}

internal data class AllowedTypedPayload(
    val value: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = -6573928863435706361L
    }
}
