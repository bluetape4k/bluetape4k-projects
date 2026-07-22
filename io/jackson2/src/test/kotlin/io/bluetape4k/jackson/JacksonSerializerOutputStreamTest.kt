package io.bluetape4k.jackson

import com.example.disallowed.DisallowedTypedPayload
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer as JacksonValueSerializer
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.jackson.binary.CborJacksonSerializer
import io.bluetape4k.jackson.binary.IonJacksonSerializer
import io.bluetape4k.jackson.binary.SmileJacksonSerializer
import io.bluetape4k.jackson.text.CsvJacksonSerializer
import io.bluetape4k.jackson.text.PropsJacksonSerializer
import io.bluetape4k.jackson.text.TomlJacksonSerializer
import io.bluetape4k.jackson.text.YamlJacksonSerializer
import io.bluetape4k.json.JsonSerializationException
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException

class JacksonSerializerOutputStreamTest {

    @Test
    fun `default와 custom mapper는 기존 wire와 exact count와 null policy를 보존한다`() {
        serializers().forEach { serializer ->
            val expected = serializer.serialize(VALUE)
            val target = RecordingOutputStream()

            serializer.serializeJsonToStream(VALUE, target) shouldBeEqualTo expected.size
            target.toByteArray() shouldBeEqualTo expected
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0

            val nullTarget = RecordingOutputStream()
            serializer.serializeJsonToStream(null, nullTarget) shouldBeEqualTo 0
            nullTarget.toByteArray() shouldBeEqualTo byteArrayOf()
            nullTarget.flushCount shouldBeEqualTo 0
            nullTarget.closeCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `direct stream은 overridable serialize sentinel을 우회한다`() {
        val expected = JacksonSerializer().serialize(VALUE)
        val serializer = spyk(JacksonSerializer())
        every { serializer.serialize(any()) } returns byteArrayOf(0x13, 0x37)
        val target = RecordingOutputStream()

        serializer.serializeJsonToStream(VALUE, target) shouldBeEqualTo expected.size

        target.toByteArray() shouldBeEqualTo expected
        verify(exactly = 0) { serializer.serialize(any()) }
    }

    @Test
    fun `custom mapper module naming inclusion과 pretty policy를 direct stream에서도 적용한다`() {
        val mapper = Jackson.createDefaultJsonMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(
                SimpleModule().addSerializer(String::class.java, UppercaseStringSerializer),
            )
        val serializer = JacksonSerializer(mapper)
        val value = ConfiguredValue(displayName = "alpha", optionalValue = null)
        val expected = serializer.serialize(value)
        val target = RecordingOutputStream()

        serializer.serializeJsonToStream(value, target) shouldBeEqualTo expected.size

        target.toByteArray() shouldBeEqualTo expected
        val json = target.toByteArray().decodeToString()
        json shouldContain "\n"
        json shouldContain "\"display_name\""
        json shouldContain "\"ALPHA\""
        (!json.contains("optional_value")).shouldBeTrue()
    }

    @Test
    fun `typed mapper allowlist policy는 direct stream wire에서도 유지된다`() {
        val serializer = JacksonSerializer(Jackson.createTypedJsonMapper("io.bluetape4k.jackson."))
        val value = TypedPayloadEnvelope(AllowedTypedPayload("safe"))
        val expected = serializer.serialize(value)
        val target = RecordingOutputStream()

        serializer.serializeJsonToStream(value, target) shouldBeEqualTo expected.size
        target.toByteArray() shouldBeEqualTo expected
        serializer.deserialize(target.toByteArray(), TypedPayloadEnvelope::class.java)
            ?.payload.shouldBeInstanceOf<AllowedTypedPayload>()
            .value shouldBeEqualTo "safe"
        serializer.deserializeFrom(boundedDirectSource(target.toByteArray()), TypedPayloadEnvelope::class.java)
            ?.payload.shouldBeInstanceOf<AllowedTypedPayload>()
            .value shouldBeEqualTo "safe"

        val denied = """{"payload":{"@class":"${DisallowedTypedPayload::class.qualifiedName}","value":"blocked"}}"""
        val arrayFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserialize(denied.toByteArray(), TypedPayloadEnvelope::class.java)
        }
        val directFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(boundedDirectSource(denied.toByteArray()), TypedPayloadEnvelope::class.java)
        }

        directFailure.javaClass shouldBeEqualTo arrayFailure.javaClass
        directFailure.cause?.javaClass shouldBeEqualTo arrayFailure.cause?.javaClass
    }

    @Test
    fun `corrupt JSON은 ByteArray와 bounded direct decode failure 분류가 같다`() {
        val serializer = JacksonSerializer()
        val corrupt = "{not-json".encodeToByteArray()
        val arrayFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserialize(corrupt, StreamValue::class.java)
        }
        val directFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(boundedDirectSource(corrupt), StreamValue::class.java)
        }

        directFailure.javaClass shouldBeEqualTo arrayFailure.javaClass
        directFailure.cause?.javaClass shouldBeEqualTo arrayFailure.cause?.javaClass
    }

    @Test
    fun `IOException과 cancellation은 JsonSerializationException cause로 분류하고 target은 재사용 가능하다`() {
        listOf(
            IOException("target failure"),
            CancellationException("cancelled"),
        ).forEach { failure ->
            val serializer = JacksonSerializer()
            val target = RecordingOutputStream(failure)

            val actual = assertFailsWith<JsonSerializationException> {
                serializer.serializeJsonToStream(VALUE, target)
            }

            generateSequence(actual as Throwable?) { it.cause }
                .any { it === failure }
                .shouldBeTrue()
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0

            target.writeFailure = null
            val expected = serializer.serialize(VALUE)
            serializer.serializeJsonToStream(VALUE, target) shouldBeEqualTo expected.size
            target.toByteArray() shouldBeEqualTo expected
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `fatal target failure는 identity를 보존하고 target lifecycle을 소유하지 않는다`() {
        val fatal = JacksonStreamFatalError()
        val target = RecordingOutputStream(fatal)
        val serializer = JacksonSerializer()

        val actual = assertFailsWith<JacksonStreamFatalError> {
            serializer.serializeJsonToStream(VALUE, target)
        }

        actual shouldBeSameInstanceAs fatal
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0

        target.writeFailure = null
        val expected = serializer.serialize(VALUE)
        serializer.serializeJsonToStream(VALUE, target) shouldBeEqualTo expected.size
        target.toByteArray() shouldBeEqualTo expected
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @TestFactory
    fun `inherited formats preserve ByteArray wire count null과 caller lifecycle`() =
        formatCases().map { case ->
            dynamicTest(case.name) {
                val expected = case.serializer.serialize(case.value)
                val target = RecordingOutputStream()

                case.serializer.serializeJsonToStream(case.value, target) shouldBeEqualTo expected.size
                target.toByteArray() shouldBeEqualTo expected
                target.flushCount shouldBeEqualTo 0
                target.closeCount shouldBeEqualTo 0

                case.serializer.serializeJsonToStream(null, target) shouldBeEqualTo 0
                target.toByteArray() shouldBeEqualTo expected
                target.flushCount shouldBeEqualTo 0
                target.closeCount shouldBeEqualTo 0
            }
        }

    @Test
    fun `counting stream은 두 write overload에서 Int MAX 초과를 write 전에 거부한다`() {
        val target = RecordingOutputStream()
        val type = Class.forName("io.bluetape4k.jackson.JacksonCallerOwnedCountingOutputStream")
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

    private fun serializers(): List<JacksonSerializer> = listOf(
        JacksonSerializer(),
        JacksonSerializer(
            Jackson.createDefaultJsonMapper()
                .enable(SerializationFeature.INDENT_OUTPUT),
        ),
    )

    private fun boundedDirectSource(bytes: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(bytes.size + 4).apply {
            put(byteArrayOf(0x51, 0x52))
            put(bytes)
            put(byteArrayOf(0x53, 0x54))
            position(2)
            limit(2 + bytes.size)
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        }

    private fun formatCases(): List<StreamFormatCase> = listOf(
        StreamFormatCase("YAML", YamlJacksonSerializer()),
        StreamFormatCase("Properties", PropsJacksonSerializer()),
        StreamFormatCase("CSV", CsvJacksonSerializer(), listOf("format")),
        StreamFormatCase("TOML", TomlJacksonSerializer()),
        StreamFormatCase("CBOR", CborJacksonSerializer()),
        StreamFormatCase("Ion", IonJacksonSerializer()),
        StreamFormatCase("Smile", SmileJacksonSerializer()),
    )

    private class RecordingOutputStream(
        var writeFailure: Throwable? = null,
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
        val VALUE = StreamValue(17, "stream-value")
    }
}

private object UppercaseStringSerializer: JacksonValueSerializer<String>() {
    override fun serialize(value: String, generator: JsonGenerator, serializers: SerializerProvider) {
        generator.writeString(value.uppercase())
    }
}

private data class StreamValue(
    val id: Int,
    val name: String,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private data class ConfiguredValue(
    val displayName: String,
    val optionalValue: String?,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private data class StreamFormatCase(
    val name: String,
    val serializer: JacksonSerializer,
    val value: Any = StreamValue(23, "format"),
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private class JacksonStreamFatalError: Error()
