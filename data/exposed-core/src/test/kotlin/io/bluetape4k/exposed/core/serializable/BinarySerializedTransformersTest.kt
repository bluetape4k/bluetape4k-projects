package io.bluetape4k.exposed.core.serializable

import io.bluetape4k.io.serializer.BinarySerializers
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.Serializable

class BinarySerializedTransformersTest {

    private data class SamplePayload(
        val name: String,
        val age: Int,
    ): Serializable

    @Test
    fun `binary serializer transformer 는 객체를 직렬화 역직렬화한다`() {
        val source = SamplePayload("alice", 23)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob serializer transformer 는 객체를 직렬화 역직렬화한다`() {
        val source = SamplePayload("bob", 41)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }
}
