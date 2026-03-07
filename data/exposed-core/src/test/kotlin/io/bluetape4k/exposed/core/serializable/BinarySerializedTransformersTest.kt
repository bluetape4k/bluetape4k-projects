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

    // --- BinarySerializedBinaryTransformer ---

    @Test
    fun `binary transformer 는 LZ4Fory 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("alice", 23)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `binary transformer 는 Kryo 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("charlie", 35)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.Kryo)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `binary transformer 는 LZ4Kryo 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("dave", 28)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.LZ4Kryo)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `binary transformer 는 Jdk 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("eve", 19)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.Jdk)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `binary transformer 는 ZstdFory 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("frank", 52)
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.ZstdFory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    // --- BinarySerializedBlobTransformer ---

    @Test
    fun `blob transformer 는 LZ4Fory 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("bob", 41)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob transformer 는 Kryo 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("grace", 30)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.Kryo)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob transformer 는 LZ4Kryo 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("henry", 45)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.LZ4Kryo)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob transformer 는 Jdk 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("iris", 27)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.Jdk)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob transformer 는 ZstdFory 직렬화로 원본을 복원한다`() {
        val source = SamplePayload("jack", 38)
        val transformer = BinarySerializedBlobTransformer<SamplePayload>(BinarySerializers.ZstdFory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }
}
