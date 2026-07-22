package io.bluetape4k.jackson3

import com.fasterxml.jackson.annotation.JsonInclude
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.jackson3.binary.CborJacksonSerializer
import io.bluetape4k.jackson3.binary.IonJacksonSerializer
import io.bluetape4k.jackson3.binary.SmileJacksonSerializer
import io.bluetape4k.jackson3.text.CsvJacksonSerializer
import io.bluetape4k.jackson3.text.PropsJacksonSerializer
import io.bluetape4k.jackson3.text.TomlJacksonSerializer
import io.bluetape4k.jackson3.text.YamlJacksonSerializer
import io.bluetape4k.json.JsonSerializationException
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException

class JacksonSerializerOutputStreamTest {

    @Test
    fun `default와 custom mapper stream은 ByteArray wire와 count 및 null policy를 보존한다`() {
        val serializers = listOf(
            JacksonSerializer(),
            JacksonSerializer(
                Jackson.createDefaultJsonMapper()
                    .rebuild()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build()
            ),
        )
        val value = CollectionItem(21, "stream")

        serializers.forEach { serializer ->
            val target = RecordingOutputStream()
            val expected = serializer.serialize(value)

            serializer.serializeJsonToStream(value, target) shouldBeEqualTo expected.size
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
    fun `direct stream은 overridable serialize fallback을 호출하지 않는다`() {
        val value = CollectionItem(22, "direct")
        val baseline = JacksonSerializer()
        val serializer = spyk(JacksonSerializer())
        val sentinel = AssertionError("serialize fallback must not run")
        every { serializer.serialize(any()) } throws sentinel
        val target = RecordingOutputStream()

        serializer.serializeJsonToStream(value, target) shouldBeEqualTo baseline.serialize(value).size
        target.toByteArray() shouldBeEqualTo baseline.serialize(value)
        verify(exactly = 0) { serializer.serialize(any()) }
    }

    @Test
    fun `custom mapper module naming inclusion과 pretty policy를 direct stream에도 적용한다`() {
        val mapper = Jackson.createDefaultJsonMapper()
            .rebuild()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .changeDefaultPropertyInclusion {
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)
            }
            .addModule(SimpleModule().addSerializer(String::class.java, Jackson3UppercaseStringSerializer))
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
        val serializer = JacksonSerializer(mapper)
        val value = Jackson3ConfiguredValue(displayName = "alpha", optionalValue = null)
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
    fun `target failure는 ByteBuffer direct 분류를 보존하고 serializer는 재사용할 수 있다`() {
        val serializer = JacksonSerializer()
        val value = CollectionItem(23, "failure")
        val failures = listOf(
            IOException("target failure"),
            CancellationException("cancelled"),
        )

        failures.forEach { failure ->
            val target = RecordingOutputStream(failure)
            val actual = assertFailsWith<JsonSerializationException> {
                serializer.serializeJsonToStream(value, target)
            }

            generateSequence(actual as Throwable?) { it.cause }
                .any { it === failure }
                .shouldBeTrue()

            target.writeFailure = null
            val expected = serializer.serialize(value)
            serializer.serializeJsonToStream(value, target) shouldBeEqualTo expected.size
            target.toByteArray() shouldBeEqualTo expected
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0
        }

        val fatal = AssertionError("fatal")
        val fatalTarget = RecordingOutputStream(fatal)
        val actual = assertFailsWith<AssertionError> {
            serializer.serializeJsonToStream(value, fatalTarget)
        }
        actual shouldBeSameInstanceAs fatal
        fatalTarget.flushCount shouldBeEqualTo 0
        fatalTarget.closeCount shouldBeEqualTo 0

        fatalTarget.writeFailure = null
        val expected = serializer.serialize(value)
        serializer.serializeJsonToStream(value, fatalTarget) shouldBeEqualTo expected.size
        fatalTarget.toByteArray() shouldBeEqualTo expected
        fatalTarget.flushCount shouldBeEqualTo 0
        fatalTarget.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `annotation polymorphism과 unsolicited type metadata 정책을 보존한다`() {
        val serializer = JacksonSerializer()
        val professor: Person = Professor("stream-professor", 42, "coroutines")
        val professorTarget = RecordingOutputStream()
        val professorWire = serializer.serialize(professor)

        serializer.serializeJsonToStream(professor, professorTarget) shouldBeEqualTo professorWire.size
        professorTarget.toByteArray() shouldBeEqualTo professorWire
        serializer.deserialize(professorTarget.toByteArray(), Person::class.java) shouldBeEqualTo professor
        serializer.deserializeFrom(boundedDirectSource(professorWire), Person::class.java) shouldBeEqualTo professor

        val metadata = mapOf("@class" to "java.lang.ProcessBuilder", "value" to "blocked")
        val metadataTarget = RecordingOutputStream()
        val metadataWire = serializer.serialize(metadata)
        serializer.serializeJsonToStream(metadata, metadataTarget) shouldBeEqualTo metadataWire.size
        metadataTarget.toByteArray() shouldBeEqualTo metadataWire
        serializer.deserialize(metadataTarget.toByteArray(), Any::class.java)
            .shouldBeInstanceOf<Map<*, *>>() shouldBeEqualTo metadata
        serializer.deserializeFrom(boundedDirectSource(metadataWire), Any::class.java)
            .shouldBeInstanceOf<Map<*, *>>() shouldBeEqualTo metadata

        val malicious = """{"@class":"java.lang.ProcessBuilder","name":"blocked","age":1}"""
        val arrayFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserialize(malicious.toByteArray(), Person::class.java)
        }
        val directFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(boundedDirectSource(malicious.toByteArray()), Person::class.java)
        }

        directFailure.javaClass shouldBeEqualTo arrayFailure.javaClass
        directFailure.cause?.javaClass shouldBeEqualTo arrayFailure.cause?.javaClass
    }

    @Test
    fun `corrupt JSON은 ByteArray와 bounded direct decode failure 분류가 같다`() {
        val serializer = JacksonSerializer()
        val corrupt = "{not-json".encodeToByteArray()
        val arrayFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserialize(corrupt, CollectionItem::class.java)
        }
        val directFailure = assertFailsWith<JsonSerializationException> {
            serializer.deserializeFrom(boundedDirectSource(corrupt), CollectionItem::class.java)
        }

        directFailure.javaClass shouldBeEqualTo arrayFailure.javaClass
        directFailure.cause?.javaClass shouldBeEqualTo arrayFailure.cause?.javaClass
    }

    @TestFactory
    fun `inherited formats preserve their ByteArray wire on streams`() =
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
    fun `counting stream은 single과 bulk Int MAX 초과를 target mutation 전에 거부한다`() {
        val type = Class.forName("io.bluetape4k.jackson3.Jackson3CallerOwnedCountingOutputStream")
        val target = RecordingOutputStream()
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
            val actual = assertFailsWith<IllegalStateException> { write(output) }

            actual.message shouldBeEqualTo "Serialized output exceeds Int.MAX_VALUE bytes."
            (actual.cause is ArithmeticException).shouldBeTrue()
            target.toByteArray() shouldBeEqualTo byteArrayOf()
        }
    }

    private fun formatCases(): List<StreamFormatCase> = listOf(
        StreamFormatCase("YAML", YamlJacksonSerializer()),
        StreamFormatCase("Properties", PropsJacksonSerializer()),
        StreamFormatCase("CSV", CsvJacksonSerializer(), listOf("stream")),
        StreamFormatCase("TOML", TomlJacksonSerializer()),
        StreamFormatCase("CBOR", CborJacksonSerializer()),
        StreamFormatCase("Ion", IonJacksonSerializer()),
        StreamFormatCase("Smile", SmileJacksonSerializer()),
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
}

private data class StreamFormatCase(
    val name: String,
    val serializer: JacksonSerializer,
    val value: Any = CollectionItem(24, "format"),
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private object Jackson3UppercaseStringSerializer: ValueSerializer<String>() {
    override fun serialize(value: String, generator: JsonGenerator, context: SerializationContext) {
        generator.writeString(value.uppercase())
    }
}

private data class Jackson3ConfiguredValue(
    val displayName: String,
    val optionalValue: String?,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
