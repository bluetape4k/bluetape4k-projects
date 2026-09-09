package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class NamebasedUuidGeneratorTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val STRESS_OPERATIONS = 10_000
        private const val STRESS_WORKERS = 8
        private const val STRESS_ROUNDS_PER_WORKER = STRESS_OPERATIONS / STRESS_WORKERS
        private val STRESS_TIMEOUT = 30.seconds
    }

    // private val randomUuid = TimebasedUuidGenerator()
    private val uuidGenerator = Uuid.V5

    @RepeatedTest(REPEAT_SIZE)
    fun `generate random uuid`() {
        val uuid1 = uuidGenerator.nextId()
        val uuid2 = uuidGenerator.nextId()

        log.trace { "uuid1=$uuid1" }
        log.trace { "uuid2=$uuid2" }
        uuid2 shouldNotBeEqualTo uuid1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate random uuid as string`() {
        val uuid1 = uuidGenerator.nextIdAsString()
        val uuid2 = uuidGenerator.nextIdAsString()

        log.trace { "uuid1=$uuid1" }
        log.trace { "uuid2=$uuid2" }
        uuid2 shouldNotBeEqualTo uuid1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultithreadingTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_ROUNDS_PER_WORKER)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in virtual threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        StructuredTaskScopeTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_OPERATIONS)
            .withTimeout(STRESS_TIMEOUT)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi jobs`() = runSuspendDefault(timeout = STRESS_TIMEOUT) {
        val idMap = ConcurrentHashMap<UUID, Int>()

        SuspendedJobTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_OPERATIONS)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }
}
