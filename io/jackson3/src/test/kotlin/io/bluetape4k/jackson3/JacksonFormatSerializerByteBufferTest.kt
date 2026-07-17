package io.bluetape4k.jackson3

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.jackson3.binary.CborJacksonSerializer
import io.bluetape4k.jackson3.binary.IonJacksonSerializer
import io.bluetape4k.jackson3.binary.SmileJacksonSerializer
import io.bluetape4k.jackson3.text.CsvJacksonSerializer
import io.bluetape4k.jackson3.text.PropsJacksonSerializer
import io.bluetape4k.jackson3.text.TomlJacksonSerializer
import io.bluetape4k.jackson3.text.YamlJacksonSerializer
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
