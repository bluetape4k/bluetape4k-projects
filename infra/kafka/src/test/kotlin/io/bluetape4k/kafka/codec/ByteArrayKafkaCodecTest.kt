package io.bluetape4k.kafka.codec

import io.bluetape4k.kafka.AbstractKafkaTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.*

/**
 * [ByteArrayKafkaCodec]에 대한 테스트 클래스입니다.
 */
class ByteArrayKafkaCodecTest: AbstractKafkaTest() {
    companion object: KLoggingChannel()

    private val codec = ByteArrayKafkaCodec()

    @Test
    fun `바이트 배열 직렬화는 동일한 배열을 반환`() {
        val original = "Hello, Kafka!".toByteArray()
        val bytes = codec.serialize(TEST_TOPIC_NAME, original)

        bytes.shouldNotBeNull()
        bytes shouldBeEqualTo original
    }

    @Test
    fun `바이트 배열 역직렬화는 동일한 배열을 반환`() {
        val original = "Test data".toByteArray()
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, original)

        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo original
    }

    @Test
    fun `빈 바이트 배열 직렬화`() {
        val emptyArray = ByteArray(0)
        val bytes = codec.serialize(TEST_TOPIC_NAME, emptyArray)

        bytes.shouldNotBeNull()
        bytes shouldBeEqualTo emptyArray
    }

    @Test
    fun `빈 바이트 배열 역직렬화`() {
        val emptyArray = ByteArray(0)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, emptyArray)

        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo emptyArray
    }

    @Test
    fun `큰 바이트 배열 직렬화`() {
        val largeArray = randomString().toByteArray()
        val bytes = codec.serialize(TEST_TOPIC_NAME, largeArray)

        bytes.shouldNotBeNull()
        bytes shouldBeEqualTo largeArray
    }

    @Test
    fun `null 값 직렬화는 null을 반환`() {
        val bytes = codec.serialize(TEST_TOPIC_NAME, null, null as ByteArray?)
        bytes.shouldBeNull()
    }

    @Test
    fun `이진 데이터 직렬화 및 역직렬화`() {
        val binaryData = ByteArray(256) { it.toByte() }
        val bytes = codec.serialize(TEST_TOPIC_NAME, binaryData)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo binaryData
    }

    @Test
    fun `랜덤 바이트 데이터 직렬화`() {
        val random = Random()
        val randomData = ByteArray(1024)
        random.nextBytes(randomData)

        val bytes = codec.serialize(TEST_TOPIC_NAME, randomData)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, bytes)

        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo randomData
    }

    @Test
    fun `직렬화 후 역직렬화는 원본 데이터를 보존`() {
        val originalText = "Binary data: \u0000\u0001\u0002\u0003\u00FF"
        val original = originalText.toByteArray(Charsets.ISO_8859_1)

        val serialized = codec.serialize(TEST_TOPIC_NAME, original)
        val deserialized = codec.deserialize(TEST_TOPIC_NAME, serialized)

        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo original
    }
}
