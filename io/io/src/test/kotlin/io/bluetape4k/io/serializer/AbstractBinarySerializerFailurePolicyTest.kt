package io.bluetape4k.io.serializer

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AbstractBinarySerializerFailurePolicyTest {

    private val serializer = object: AbstractBinarySerializer() {
        override fun doSerialize(graph: Any): ByteArray {
            throw IllegalStateException("serialize-failed")
        }

        override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
            throw IllegalStateException("deserialize-failed")
        }
    }

    @Test
    fun `직렬화 실패 시 BinarySerializationException 을 던진다`() {
        val exception = assertFailsWith<BinarySerializationException> {
            serializer.serialize("fail")
        }

        (exception.cause is IllegalStateException) shouldBeEqualTo true
    }

    @Test
    fun `역직렬화 실패 시 BinarySerializationException 을 던진다`() {
        val exception = assertFailsWith<BinarySerializationException> {
            serializer.deserialize<String>(byteArrayOf(1, 2, 3))
        }

        (exception.cause is IllegalStateException) shouldBeEqualTo true
    }

    @Test
    fun `null 또는 empty 입력 정책은 유지한다`() {
        serializer.serialize(null).shouldBeEmpty()
        serializer.deserialize<String>(null) shouldBeEqualTo null
        serializer.deserialize<String>(byteArrayOf()) shouldBeEqualTo null
    }
}
