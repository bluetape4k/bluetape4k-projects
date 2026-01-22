package io.bluetape4k.idgenerators

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.Test

class IdSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `create machine id by network address`() {
        val machineId = getMachineId()
        log.debug { "machineId=$machineId" }

        machineId shouldBeInRange (0 until MAX_MACHINE_ID)
    }
}
