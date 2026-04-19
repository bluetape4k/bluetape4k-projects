package io.bluetape4k.support

import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Byte-specific 변환/슬라이스 테스트.
 *
 * 구조적(인덱싱·회전·역순·capacity) 동작은 [PrimitiveArraySupportTest]의 파라미터
 * 테스트에서 Int/Long/Float/Double 배열과 함께 일괄 검증합니다.
 */
class ByteArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        fun sampleByteArray(size: Int = 5): ByteArray {
            return ByteArray(size) { (it + 1).toByte() }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Int value to ByteArray vice versa`() {
        val value = Fakers.random.nextInt()
        val bytes = value.toByteArray()
        val converted = bytes.toInt()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Long value to ByteArray vice versa`() {
        val value = Fakers.random.nextLong()
        val bytes = value.toByteArray()
        val converted = bytes.toLong()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert UUID value to ByteArray vice versa`() {
        val value = UUID.randomUUID()
        val bytes = value.toByteArray()
        val converted = bytes.toUuid()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @Test
    fun `take and drop items from byte array`() {
        val bytes = sampleByteArray()

        bytes.take(3).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3)
        bytes.take(0).toByteArray() shouldBeEqualTo emptyByteArray
        bytes.take(5).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)
        bytes.take(10).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)

        bytes.drop(3).toByteArray() shouldBeEqualTo byteArrayOf(4, 5)
        bytes.drop(0).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)
        bytes.drop(5).toByteArray() shouldBeEqualTo emptyByteArray
        bytes.drop(10).toByteArray() shouldBeEqualTo emptyByteArray
    }
}
