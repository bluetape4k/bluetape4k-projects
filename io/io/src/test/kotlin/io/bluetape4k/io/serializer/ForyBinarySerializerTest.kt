package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class ForyBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()

    override val serializer: BinarySerializer = ForyBinarySerializer()

    @Test
    fun `default Fory stream bytes equal serialize bytes`() {
        assertStreamParity(ForyBinarySerializer(), "default-fory-stream")
    }

    @Test
    fun `fast Fory stream bytes equal serialize bytes`() {
        assertStreamParity(ForyBinarySerializer.fast(), "fast-fory-stream")
    }

    private fun assertStreamParity(serializer: ForyBinarySerializer, value: String) {
        val target = ByteArrayOutputStream()
        val expected = serializer.serialize(value)

        val written = serializer.serializeBinaryToStream(value, target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
    }

}
