package io.bluetape4k.kafka.codec

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * [AbstractKafkaCodec.allowedTypePackages] 허용 목록 검증 테스트 (#296/#303).
 *
 * - emptySet() 기본값: 모든 클래스 허용 (하위 호환)
 * - 허용 패키지 지정 시: 목록 외 클래스 → IllegalArgumentException → deserialize null 반환 (poison-pill)
 * - 허용 패키지에 속하는 클래스: 정상 처리
 */
class KafkaCodecAllowlistTest {

    companion object: KLogging()

    private val topic = "test-topic"
    private val mapper: JsonMapper = Jackson.defaultJsonMapper

    private inner class TestJsonCodec(
        override val allowedTypePackages: Set<String> = emptySet(),
    ): AbstractKafkaCodec<Any?>() {

        override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray =
            graph?.let { mapper.writeValueAsBytes(it) } ?: emptyByteArray

        override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? {
            val clazz = getValueType(headers)
            return if (bytes.isEmpty()) null else mapper.readValue(bytes, clazz)
        }
    }

    data class SimpleMessage(val content: String)

    @Test
    fun `emptySet 기본값 - 모든 클래스 허용`() {
        val codec = TestJsonCodec()
        val headers = RecordHeaders()
        val data = SimpleMessage("hello")
        val bytes = codec.serialize(topic, headers, data)

        val result = codec.deserialize(topic, headers, bytes)
        result.shouldNotBeNull()
    }

    @Test
    fun `제한 코덱 - 허용 패키지 클래스는 정상 처리`() {
        val codec = TestJsonCodec(allowedTypePackages = setOf("io.bluetape4k.kafka.codec"))
        val headers = RecordHeaders()
        val data = SimpleMessage("world")
        val bytes = codec.serialize(topic, headers, data)

        // SimpleMessage는 io.bluetape4k.kafka.codec 패키지 → 허용됨
        val result = codec.deserialize(topic, headers, bytes)
        result.shouldNotBeNull()
    }

    @Test
    fun `제한 코덱 - 허용 패키지 외 클래스는 null 반환 (poison-pill)`() {
        val openCodec = TestJsonCodec()
        val restrictedCodec = TestJsonCodec(allowedTypePackages = setOf("io.bluetape4k.kafka.codec"))

        // openCodec으로 직렬화 → 헤더에 java.util.LinkedHashMap 타입 주입
        val headers = RecordHeaders()
        val data = mapOf("key" to "value")
        val bytes = openCodec.serialize(topic, headers, data)

        // restrictedCodec은 io.bluetape4k.kafka.codec만 허용 → java.util.* 차단 → null
        val result = restrictedCodec.deserialize(topic, headers, bytes)
        result.shouldBeNull()
    }

    @Test
    fun `패키지 prefix spoofing 차단 - 허용 패키지와 유사하지만 다른 패키지는 거부`() {
        // "io.bluetape4k.kafka.codec" 허용 시 "io.bluetape4k.kafka.codecEvil" 차단 검증
        val openCodec = TestJsonCodec()
        val restrictedCodec = TestJsonCodec(allowedTypePackages = setOf("io.bluetape4k.kafka.codec"))

        val headers = RecordHeaders()
        // SimpleMessage 직렬화 후 헤더 값을 spoofing된 패키지명으로 교체
        val data = SimpleMessage("spoof test")
        openCodec.serialize(topic, headers, data)

        // 헤더의 VALUE_TYPE_KEY 값을 허용 패키지와 유사한 악의적 패키지명으로 덮어씀
        val spoofedClass = "io.bluetape4k.kafka.codecEvil.MaliciousClass"
        headers.remove(AbstractKafkaCodec.VALUE_TYPE_KEY)
        headers.add(AbstractKafkaCodec.VALUE_TYPE_KEY, spoofedClass.toByteArray(Charsets.UTF_8))

        // startsWith("io.bluetape4k.kafka.codec") = true이지만 패키지 경계(".") 없어서 차단돼야 함
        val result = restrictedCodec.deserialize(topic, headers, """{"content":"spoof test"}""".toByteArray())
        result.shouldBeNull()
    }

    @Test
    fun `헤더에 클래스 정보 없으면 Any 타입으로 역직렬화 성공`() {
        val codec = TestJsonCodec()
        val headers: Headers? = null
        val bytes = """{"key":"value"}""".toByteArray()

        // 헤더 없음 → getValueType() returns Any::class.java → Jackson이 LinkedHashMap으로 역직렬화
        val result = codec.deserialize(topic, headers, bytes)
        result.shouldNotBeNull()
    }
}
