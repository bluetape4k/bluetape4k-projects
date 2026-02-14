package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class KryoBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()

    override val serializer: BinarySerializer = KryoBinarySerializer()

    @Test
    fun `bufferSize 가 1이어도 직렬화 역직렬화가 가능하다`() {
        val serializer = KryoBinarySerializer(bufferSize = 1)
        val expected = "small-buffer-kryo"

        val bytes = serializer.serialize(expected)
        val actual = serializer.deserialize<String>(bytes)

        actual shouldBeEqualTo expected
    }

    @Test
    fun `bufferSize 는 0 이하를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            KryoBinarySerializer(0)
        }
        assertFailsWith<IllegalArgumentException> {
            KryoBinarySerializer(-1)
        }
    }
}
