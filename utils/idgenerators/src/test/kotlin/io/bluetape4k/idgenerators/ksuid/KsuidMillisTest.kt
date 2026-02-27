package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.idgenerators.snowflake.MAX_SEQUENCE
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ConcurrentHashMap

class KsuidMillisTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val TEST_COUNT = MAX_SEQUENCE * 4
    }

    @Test
    fun `KsuidMillis should decode to 20 bytes and payload should be 12 bytes`() {
        repeat(TEST_COUNT) {
            val ksuid = KsuidMillis.generate()
            ksuid.length shouldBeEqualTo KsuidMillis.MAX_ENCODED_LEN

            val decoded = BytesBase62.decode(ksuid, expectedBytes = KsuidMillis.TOTAL_BYTES)
            decoded.size shouldBeEqualTo KsuidMillis.TOTAL_BYTES

            val payloadHex = decoded.copyOfRange(KsuidMillis.TIMESTAMP_LEN, KsuidMillis.TOTAL_BYTES).encodeHexString()
            payloadHex.length shouldBeEqualTo KsuidMillis.PAYLOAD_LEN * 2
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid`() {
        val ksuid = KsuidMillis.generate()

        log.debug { "Generated KSUID: $ksuid" }
        log.debug { "Decoded KSUID: ${KsuidMillis.prettyString(ksuid)}" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate multiple ksuids`() {
        val count = 100
        val ids = List(count) { KsuidMillis.generate() }

        ids.distinct().size shouldBeEqualTo count
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuids in multi threadings`() {
        val idMaps = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(TEST_COUNT)
            .add {
                val ksuid = KsuidMillis.generate()
                idMaps.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid in virtual threads`() {
        val idMap = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .rounds(TEST_COUNT)
            .add {
                val ksuid = KsuidMillis.generate()
                idMap.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid in coroutines`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .rounds(TEST_COUNT)
            .add {
                val ksuid = KsuidMillis.generate()
                idMap.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }
}
