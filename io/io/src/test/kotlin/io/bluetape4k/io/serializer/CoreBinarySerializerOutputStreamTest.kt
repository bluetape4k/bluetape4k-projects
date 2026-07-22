package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
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
    fun `Kryo pooled output은 모든 failure 뒤 caller target에서 분리해 반환한다`() {
        val serializer = KryoBinarySerializer()
        val borrowed = KryoProvider.obtainOutput()
        val failures = listOf(
            IOException("target failure") to BinarySerializationException::class.java,
            CancellationException("cancelled") to CancellationException::class.java,
            AssertionError("fatal") to AssertionError::class.java,
        )

        mockkObject(KryoProvider)
        try {
            every { KryoProvider.obtainOutput() } returns borrowed
            every { KryoProvider.releaseOutput(borrowed) } returns Unit

            failures.forEach { (failure, expectedType) ->
                val actual = assertFailsWith<Throwable> {
                    serializer.serializeBinaryToStream(VALUE, RecordingOutputStream(failure))
                }

                expectedType.isInstance(actual).shouldBeTrue()
                if (failure !is IOException) {
                    actual shouldBeSameInstanceAs failure
                }
                borrowed.outputStream shouldBeEqualTo null
            }

            verify(exactly = failures.size) { KryoProvider.obtainOutput() }
            verify(exactly = failures.size) { KryoProvider.releaseOutput(borrowed) }
        } finally {
            unmockkObject(KryoProvider)
            borrowed.setOutputStream(null)
            KryoProvider.releaseOutput(borrowed)
        }
    }

    @Test
    fun `custom Kryo pool은 구별 가능한 allocating fallback wire format을 보존한다`() {
        val pool = object: Pool<Kryo>(true, false, 1) {
            override fun create(): Kryo = KryoProvider.createKryo().apply {
                isRegistrationRequired = true
                register(RegisteredPayload::class.java)
            }
        }
        val value = RegisteredPayload("custom-pool")
        val expected = KryoBinarySerializer(kryoPool = pool).serialize(value)
        val serializer = spyk(KryoBinarySerializer(kryoPool = pool))
        val target = RecordingOutputStream()
        every { serializer.serialize(value) } returns expected

        val written = serializer.serializeBinaryToStream(value, target)

        target.toByteArray() shouldBeEqualTo expected
        written shouldBeEqualTo expected.size
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
        verify(exactly = 1) { serializer.serialize(value) }
    }

    @Test
    fun `secure Kryo native stream은 미등록 값을 target mutation 전에 거부한다`() {
        val serializer = KryoBinarySerializer.secure(String::class.java)
        val target = RecordingOutputStream()

        assertFailsWith<BinarySerializationException> {
            serializer.serializeBinaryToStream(UnregisteredPayload("blocked"), target)
        }

        target.toByteArray() shouldBeEqualTo byteArrayOf()
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

        listOf<(OutputStream) -> Unit>(
            { it.write(0) },
            { it.write(byteArrayOf(1, 2, 3), 1, 1) },
        ).forEach { write ->
            val actual = assertFailsWith<IllegalStateException> {
                write(output)
            }

            actual.message shouldBeEqualTo "Serialized output exceeds Int.MAX_VALUE bytes."
            (actual.cause is ArithmeticException).shouldBeTrue()
            target.toByteArray() shouldBeEqualTo byteArrayOf()
        }
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

    private data class RegisteredPayload(val value: String)

    private data class UnregisteredPayload(val value: String)
}
