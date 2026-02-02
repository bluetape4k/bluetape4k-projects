package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.collections.eclipse.fastList
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
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ConcurrentHashMap

class KsuidTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val TEST_COUNT = MAX_SEQUENCE * 4
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid`() {
        val ksuid = Ksuid.generate()
        log.debug { "Generated KSUID: $ksuid" }
        log.debug { "Decoded KSUID: ${Ksuid.prettyString(ksuid)}" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate multiple ksuids`() {
        val count = 100
        val ids = fastList(count) { Ksuid.generate() }

        ids.distinct().size shouldBeEqualTo count
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuids in multi threadings`() {
        val idMaps = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors)
            .roundsPerThread(TEST_COUNT / Runtimex.availableProcessors)
            .add {
                val ksuid = Ksuid.generate()
                idMaps.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid in virtual threads`() {
        val idMap = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .roundsPerTask(TEST_COUNT)
            .add {
                val ksuid = Ksuid.generate()
                idMap.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate ksuid in coroutines`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .roundsPerJob(TEST_COUNT)
            .add {
                val ksuid = Ksuid.generate()
                idMap.putIfAbsent(ksuid, 1).shouldBeNull()
            }
            .run()
    }
}
