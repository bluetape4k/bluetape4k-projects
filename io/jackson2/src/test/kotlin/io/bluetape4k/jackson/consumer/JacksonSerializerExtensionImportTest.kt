package io.bluetape4k.jackson.consumer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.jackson.deserialize
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize as deserializeRaw
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class JacksonSerializerExtensionImportTest {

    @Test
    fun `external caller can import concrete and raw ByteBuffer extensions together`() {
        val serializer = JacksonSerializer()
        val expected = listOf(ConsumerItem(1, "external"))
        val wire = serializer.serialize(expected)

        serializer.deserialize<List<ConsumerItem>>(ByteBuffer.wrap(wire)) shouldBeEqualTo expected

        val contract: JsonSerializer = serializer
        val raw: Any? = contract.deserializeRaw<List<ConsumerItem>>(ByteBuffer.wrap(wire))
        (raw as List<*>).first().shouldBeInstanceOf<Map<*, *>>()
    }
}

private data class ConsumerItem(
    val id: Int,
    val name: String,
)
