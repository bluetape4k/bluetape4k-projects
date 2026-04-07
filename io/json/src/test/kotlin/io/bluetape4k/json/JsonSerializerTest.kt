package io.bluetape4k.json

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [JsonSerializer] 인터페이스의 기본 메서드 동작을 검증합니다.
 *
 * 구체적인 JSON 파싱 로직은 구현체(Jackson, Fastjson2 등)에 위임하므로,
 * 이 테스트에서는 인터페이스가 제공하는 기본 메서드의 null/빈값 처리 계약을 검증합니다.
 */
class JsonSerializerTest {
    companion object: KLogging()

    /**
     * 테스트용 최소 구현체.
     * - [serialize]: null → 빈 배열, 그 외 → UTF-8 JSON 바이트 (간단한 toString)
     * - [deserialize]: null/빈배열 → null, 그 외 → 바이트를 String으로 복원
     */
    private val serializer: JsonSerializer =
        object: JsonSerializer {
            override fun serialize(graph: Any?): ByteArray {
                if (graph == null) return ByteArray(0)
                return "\"$graph\"".toByteArray(Charsets.UTF_8)
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T: Any> deserialize(
                bytes: ByteArray?,
                clazz: Class<T>,
            ): T? {
                if (bytes == null || bytes.isEmpty()) return null
                val text = bytes.toString(Charsets.UTF_8)
                // 따옴표 제거
                return text.trim('"') as T
            }
        }

    // ─── serializeAsString ───────────────────────────────────────────────────

    @Test
    fun `null 객체를 문자열 직렬화하면 빈 문자열 반환`() {
        val result = serializer.serializeAsString(null)
        result.shouldBeEmpty()
    }

    @Test
    fun `객체를 문자열 직렬화하면 비어 있지 않은 문자열 반환`() {
        val result = serializer.serializeAsString("hello")
        result.shouldNotBeEmpty()
    }

    @Test
    fun `문자열 직렬화 후 역직렬화 시 원래 값 복원`() {
        val original = "bluetape4k"
        val jsonText = serializer.serializeAsString(original)
        jsonText.shouldNotBeEmpty()

        val restored = serializer.deserializeFromString(jsonText, String::class.java)
        restored.shouldNotBeNull() shouldBeEqualTo original
    }

    // ─── deserializeFromString ───────────────────────────────────────────────

    @Test
    fun `null 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString(null, String::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `비어 있지 않은 문자열 역직렬화 시 결과 반환`() {
        val result = serializer.deserializeFromString("\"hello\"", String::class.java)
        result.shouldNotBeNull()
    }

    // ─── reified 확장 함수 ─────────────────────────────────────────────────────

    @Test
    fun `reified deserialize - null 바이트 배열 시 null 반환`() {
        val result: String? = serializer.deserialize(null as ByteArray?)
        result.shouldBeNull()
    }

    @Test
    fun `reified deserialize - 유효한 바이트 배열 역직렬화`() {
        val bytes = serializer.serialize("world")
        val result: String? = serializer.deserialize(bytes)
        result.shouldNotBeNull() shouldBeEqualTo "world"
    }

    @Test
    fun `reified deserializeFromString - null 문자열 시 null 반환`() {
        val result: String? = serializer.deserializeFromString(null as String?)
        result.shouldBeNull()
    }

    @Test
    fun `reified deserializeFromString - 유효한 문자열 역직렬화`() {
        val jsonText = serializer.serializeAsString("kotlin")
        val result: String? = serializer.deserializeFromString(jsonText)
        result.shouldNotBeNull() shouldBeEqualTo "kotlin"
    }

    // ─── serialize → deserialize 왕복 ────────────────────────────────────────

    @Test
    fun `바이트 배열 직렬화-역직렬화 왕복 시 원래 값 복원`() {
        val original = "roundtrip"
        val bytes = serializer.serialize(original)
        bytes.shouldNotBeEmpty()

        val restored = serializer.deserialize(bytes, String::class.java)
        restored.shouldNotBeNull() shouldBeEqualTo original
    }

    @Test
    fun `null 직렬화 결과는 빈 배열이며 역직렬화 시 null 반환`() {
        val bytes = serializer.serialize(null)
        bytes.shouldBeEmpty()

        val result = serializer.deserialize(bytes, String::class.java)
        result.shouldBeNull()
    }
}
