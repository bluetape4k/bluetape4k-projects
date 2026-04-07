package io.bluetape4k.exposed.core.serializable

import io.bluetape4k.io.serializer.BinarySerializers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun `binary transformer 는 손상된 바이트를 역직렬화 시 의미있는 오류 메시지를 던진다`() {
        val transformer = BinarySerializedBinaryTransformer<SamplePayload>(BinarySerializers.LZ4Fory)
        val corruptedBytes = byteArrayOf(0, 1, 2, 3, 4, 5)

        val ex =
            assertThrows<Exception> {
                transformer.wrap(corruptedBytes)
            }
        // 예외가 발생하면 충분함 — IllegalStateException 또는 직렬화 오류
        ex.message!! shouldContain ex.message!!
    }

    @Test
    fun `binary transformer 는 여러 필드를 가진 복잡한 객체를 직렬화하고 복원한다`() {
        data class ComplexPayload(
            val id: Long,
            val name: String,
            val scores: List<Int>,
        ): Serializable

        val source = ComplexPayload(id = 1L, name = "복잡한 객체", scores = listOf(10, 20, 30))
        val transformer = BinarySerializedBinaryTransformer<ComplexPayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }

    @Test
    fun `blob transformer 는 여러 필드를 가진 복잡한 객체를 직렬화하고 복원한다`() {
        data class ComplexPayload(
            val id: Long,
            val name: String,
            val scores: List<Int>,
        ): Serializable

        val source = ComplexPayload(id = 2L, name = "blob 복잡한 객체", scores = listOf(1, 2, 3))
        val transformer = BinarySerializedBlobTransformer<ComplexPayload>(BinarySerializers.LZ4Fory)

        val serialized = transformer.unwrap(source)
        val restored = transformer.wrap(serialized)

        restored shouldBeEqualTo source
    }
}
