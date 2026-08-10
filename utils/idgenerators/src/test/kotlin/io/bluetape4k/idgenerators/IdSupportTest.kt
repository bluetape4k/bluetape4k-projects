package io.bluetape4k.idgenerators

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.utils.node.MacAddressNodeIdentifier
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class IdSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `create machine id by network address`() {
        val machineId = getMachineId()
        log.debug { "machineId=$machineId" }

        machineId shouldBeInRange (0 until MAX_MACHINE_ID)
    }

    @Test
    fun `machine id support parses signed and unsigned values`() {
        "ZZ".parseAsInt() shouldBeEqualTo 1295
        "ffffffff".parseAsUInt(16) shouldBeEqualTo -1
        "10".parseAsLong(16) shouldBeEqualTo 16L
        "ffffffffffffffff".parseAsULong(16) shouldBeEqualTo -1L
    }

    @Test
    fun `long id generator exposes radix 36 string sequences`() {
        val generator = object: LongIdGenerator {
            private var current = 35L

            override fun nextId(): Long = current++
        }

        generator.nextIdAsString() shouldBeEqualTo "z"
        generator.nextIdsAsString(2).toList() shouldBeEqualTo listOf("10", "11")
    }

    @Test
    fun `mac address node identifier accepts six byte addresses`() {
        val identifier = MacAddressNodeIdentifier(byteArrayOf(0, 1, 2, 3, 4, 5))

        identifier.get() shouldBeEqualTo 0x000102030405L

        assertFailsWith<IllegalArgumentException> {
            MacAddressNodeIdentifier(byteArrayOf(1, 2, 3))
        }
    }
}
