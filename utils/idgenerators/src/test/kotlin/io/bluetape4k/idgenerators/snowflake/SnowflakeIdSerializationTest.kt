package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

class SnowflakeIdSerializationTest {

    @Test
    fun `SnowflakeId는 기존 계산 UID와 Java serialization round trip을 유지한다`() {
        val id = SnowflakeId(timestamp = 1_700_000_000_000L, machineId = 7, sequence = 3)

        deserialize<SnowflakeId>(serialize(id)).apply {
            timestamp shouldBeEqualTo id.timestamp
            machineId shouldBeEqualTo id.machineId
            sequence shouldBeEqualTo id.sequence
            value shouldBeEqualTo id.value
        }
        SnowflakeId::class.java.getDeclaredField("serialVersionUID").apply { isAccessible = true }
            .getLong(null) shouldBeEqualTo -4955654157101446216L
        ObjectStreamClass.lookup(SnowflakeId::class.java).serialVersionUID shouldBeEqualTo -4955654157101446216L
    }

    private fun serialize(value: Any): ByteArray = ByteArrayOutputStream().use { bytes ->
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        bytes.toByteArray()
    }

    private inline fun <reified T> deserialize(bytes: ByteArray): T =
        ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
}
