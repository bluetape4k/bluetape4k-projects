package io.bluetape4k.jackson

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.jackson.binary.CborJacksonSerializer
import io.bluetape4k.jackson.binary.IonJacksonSerializer
import io.bluetape4k.jackson.binary.SmileJacksonSerializer
import io.bluetape4k.jackson.text.CsvJacksonSerializer
import io.bluetape4k.jackson.text.PropsJacksonSerializer
import io.bluetape4k.jackson.text.TomlJacksonSerializer
import io.bluetape4k.jackson.text.YamlJacksonSerializer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.ByteBuffer

class JacksonFormatSerializerByteBufferTest {

    @TestFactory
    fun `inherited formats preserve ByteArray wire and cross-read ByteBuffer paths`() =
        formatCases().map { case ->
            dynamicTest(case.name) {
                val oldWire = case.serializer.serialize(case.value)
                val target = ByteBuffer.allocate(oldWire.size)

                case.serializer.serializeTo(case.value, target) shouldBeEqualTo oldWire.size
                val newWire = target.array()
                newWire.contentEquals(oldWire).shouldBeTrue()

                @Suppress("UNCHECKED_CAST")
                val targetClass = case.targetClass as Class<Any>
                case.serializer.deserializeFrom(ByteBuffer.wrap(oldWire), targetClass) shouldBeEqualTo
                    case.serializer.deserialize(oldWire, targetClass)
                case.serializer.deserialize(newWire, targetClass) shouldBeEqualTo case.value
            }
        }

    private fun formatCases(): List<FormatCase> = listOf(
        FormatCase("YAML", YamlJacksonSerializer()),
        FormatCase("Properties", PropsJacksonSerializer()),
        FormatCase("CSV", CsvJacksonSerializer(), listOf("format"), List::class.java),
        FormatCase("TOML", TomlJacksonSerializer()),
        FormatCase("CBOR", CborJacksonSerializer()),
        FormatCase("Ion", IonJacksonSerializer()),
        FormatCase("Smile", SmileJacksonSerializer()),
    )
}

private data class FormatCase(
    val name: String,
    val serializer: JacksonSerializer,
    val value: Any = FormatItem(17, "format"),
    val targetClass: Class<*> = FormatItem::class.java,
)

private data class FormatItem(
    val id: Int = 0,
    val name: String = "",
)
