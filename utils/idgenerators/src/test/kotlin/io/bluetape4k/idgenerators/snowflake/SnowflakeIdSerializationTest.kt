package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.lookup
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class SnowflakeIdSerializationTest {

    companion object: KLogging()

    @Test
    fun `SnowflakeId는 기존 계산 UID와 Java serialization round trip을 유지한다`() {
        val id = SnowflakeId(timestamp = 1_700_000_000_000L, machineId = 7, sequence = 3)

        deserialize<SnowflakeId>(serialize(id)).shouldNotBeNull()
            .apply {
                timestamp shouldBeEqualTo id.timestamp
                machineId shouldBeEqualTo id.machineId
                sequence shouldBeEqualTo id.sequence
                value shouldBeEqualTo id.value
            }
        SnowflakeId::class.java.getDeclaredField("serialVersionUID").apply { isAccessible = true }
            .getLong(null) shouldBeEqualTo -4955654157101446216L

        // ObjectStreamClass.lookup(SnowflakeId::class.java).serialVersionUID shouldBeEqualTo -4955654157101446216L
        SnowflakeId::class.lookup().serialVersionUID shouldBeEqualTo -4955654157101446216L
    }

    private fun serialize(value: Any): ByteArray =
        BinarySerializers.FastFory.serialize(value)

    private inline fun <reified T: Any> deserialize(bytes: ByteArray): T? =
        BinarySerializers.FastFory.deserialize(bytes)
}
