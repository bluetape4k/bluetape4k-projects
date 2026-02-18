package io.bluetape4k.kafka.codec

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * [StringKafkaCodec]에 대한 테스트 클래스입니다.
 */
class StringKafkaCodecTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    private val codec = StringKafkaCodec()

    @Test
    fun `기본 문자열 직렬화 및 역직렬화`() {
        val original = "Hello, Kafka!"
        val bytes = codec.serialize(TEST_TOPIC_NAME, original)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo original
    }

    @ParameterizedTest
    @ValueSource(strings = ["a", "Hello World", "한글 테스트", "日本語テスト", "🚀🎉💻"])
    fun `다양한 문자열 직렬화 및 역직렬화`(input: String) {
        val bytes = codec.serialize(TEST_TOPIC_NAME, input)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo input
    }

    @Test
    fun `빈 문자열 직렬화 및 역직렬화는 null 반환`() {
        val input = ""
        val bytes = codec.serialize(TEST_TOPIC_NAME, input)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        // 빈 문자열은 역직렬화 시 null로 반환됨 (StringKafkaCodec의 동작)
        deserialized.shouldBeNull()
    }

    @Test
    fun `null 값 직렬화는 null을 반환`() {
        val bytes = codec.serialize(TEST_TOPIC_NAME, null, null as String?)
        bytes.shouldBeNull()
    }

    @Test
    fun `빈 바이트 배열 역직렬화는 null을 반환`() {
        val result = codec.deserialize(TEST_TOPIC_NAME, ByteArray(0))
        result.shouldBeNull()
    }

    @Test
    fun `긴 문자열 직렬화 및 역직렬화`() {
        val longString = randomString()
        val bytes = codec.serialize(TEST_TOPIC_NAME, longString)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo longString
    }

    @Test
    fun `멀티라인 문자열 직렬화 및 역직렬화`() {
        val multilineString =
            """
            첫 번째 줄
            두 번째 줄
            세 번째 줄
            
            공백 줄 포함
            """.trimIndent()

        val bytes = codec.serialize(TEST_TOPIC_NAME, multilineString)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo multilineString
    }

    @Test
    fun `특수 문자가 포함된 문자열 직렬화`() {
        val specialString = "Special chars: \t\n\r!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val bytes = codec.serialize(TEST_TOPIC_NAME, specialString)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo specialString
    }

    @Test
    fun `UTF-8 인코딩으로 다국어 문자열 처리`() {
        val utf8String = "English: Hello, 한국어: 안녕하세요, 日本語: こんにちは, 中文: 你好, العربية: مرحبا"
        val bytes = codec.serialize(TEST_TOPIC_NAME, utf8String)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized shouldBeEqualTo utf8String
    }
}
