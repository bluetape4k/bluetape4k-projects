package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.Pool
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CancellationException

class CoreBinarySerializerOutputStreamTest {

    @Test
    fun `JDK와 Kryo direct stream은 기존 wire format과 count를 보존한다`() {
        serializers().forEach { serializer ->
            val target = RecordingOutputStream()
            val expected = serializer.serialize(VALUE)

            val written = serializer.serializeBinaryToStream(VALUE, target)

            target.toByteArray() shouldBeEqualTo expected
            written shouldBeEqualTo expected.size
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `JDK와 native Kryo direct stream은 overridable serialize를 호출하지 않는다`() {
        val sentinel = AssertionError("serialize must not be called")
        val serializers = listOf(
            spyk(JdkBinarySerializer()),
            spyk(KryoBinarySerializer()),
        )

        serializers.forEach { serializer ->
            every { serializer.serialize(any()) } throws sentinel
            val target = RecordingOutputStream()

            serializer.serializeBinaryToStream(VALUE, target)

            target.toByteArray().isNotEmpty().shouldBeTrue()
        }
    }

    @Test
    fun `JDK와 Kryo direct stream은 target write failure를 backend 예외로 보존한다`() {
        serializers().forEach { serializer ->
            val writeFailure = IOException("target failure")
            val target = RecordingOutputStream(writeFailure)

            val actual = assertFailsWith<BinarySerializationException> {
                serializer.serializeBinaryToStream(VALUE, target)
            }

            generateSequence(actual as Throwable?) { it.cause }
                .any { it === writeFailure }
                .shouldBeTrue()
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `JDK와 Kryo direct stream은 cancellation identity를 복원한다`() {
        serializers().forEach { serializer ->
            val cancellation = CancellationException("cancelled")

            val actual = assertFailsWith<CancellationException> {
                serializer.serializeBinaryToStream(VALUE, RecordingOutputStream(cancellation))
            }

            actual shouldBeSameInstanceAs cancellation
        }
    }

    @Test
    fun `JDK와 Kryo direct stream은 fatal failure identity를 복원한다`() {
        serializers().forEach { serializer ->
            val fatal = AssertionError("fatal")

            val actual = assertFailsWith<AssertionError> {
                serializer.serializeBinaryToStream(VALUE, RecordingOutputStream(fatal))
            }

            actual shouldBeSameInstanceAs fatal
        }
    }

    @Test
    fun `JDK와 Kryo counting stream은 Int MAX 초과를 write 전에 거부한다`() {
        listOf(
            "io.bluetape4k.io.serializer.JdkCallerOwnedCountingOutputStream",
            "io.bluetape4k.io.serializer.KryoCallerOwnedCountingOutputStream",
        ).forEach(::verifyCountOverflow)
    }

    @Test
    fun `Kryo pooled output은 failure 뒤 반환되어 다음 호출에서 재사용할 수 있다`() {
        val serializer = KryoBinarySerializer()
        val writeFailure = IOException("target failure")

        assertFailsWith<BinarySerializationException> {
            serializer.serializeBinaryToStream(VALUE, RecordingOutputStream(writeFailure))
        }
        val target = RecordingOutputStream()

        val written = serializer.serializeBinaryToStream(VALUE, target)

        target.toByteArray() shouldBeEqualTo serializer.serialize(VALUE)
        written shouldBeEqualTo target.toByteArray().size
    }

    @Test
    fun `custom Kryo pool은 allocating fallback wire format을 보존한다`() {
        val pool = object: Pool<Kryo>(true, false, 1) {
            override fun create(): Kryo = KryoProvider.createKryo()
        }
        val serializer = KryoBinarySerializer(kryoPool = pool)
        val target = RecordingOutputStream()
        val expected = serializer.serialize(VALUE)

        val written = serializer.serializeBinaryToStream(VALUE, target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    private fun serializers(): List<BinarySerializer> = listOf(
        JdkBinarySerializer(),
        KryoBinarySerializer(),
        KryoBinarySerializer.fast(),
        KryoBinarySerializer.secure(String::class.java),
    )

    private fun verifyCountOverflow(className: String) {
        val target = RecordingOutputStream()
        val type = Class.forName(className)
        val constructor = type.getDeclaredConstructor(OutputStream::class.java).apply { isAccessible = true }
        val output = constructor.newInstance(target) as OutputStream
        type.getDeclaredField("written").apply {
            isAccessible = true
            setInt(output, Int.MAX_VALUE)
        }

        val actual = assertFailsWith<IllegalStateException> {
            output.write(0)
        }

        actual.message shouldBeEqualTo "Serialized output exceeds Int.MAX_VALUE bytes."
        (actual.cause is ArithmeticException).shouldBeTrue()
        target.toByteArray() shouldBeEqualTo byteArrayOf()
    }

    private class RecordingOutputStream(
        private val writeFailure: Throwable? = null,
    ): OutputStream() {
        private val output = ByteArrayOutputStream()

        var flushCount: Int = 0
            private set

        var closeCount: Int = 0
            private set

        override fun write(value: Int) {
            writeFailure?.let { throw it }
            output.write(value)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            writeFailure?.let { throw it }
            output.write(bytes, offset, length)
        }

        override fun flush() {
            flushCount++
        }

        override fun close() {
            closeCount++
        }

        fun toByteArray(): ByteArray = output.toByteArray()
    }

    private companion object {
        const val VALUE = "stream-value"
    }
}
