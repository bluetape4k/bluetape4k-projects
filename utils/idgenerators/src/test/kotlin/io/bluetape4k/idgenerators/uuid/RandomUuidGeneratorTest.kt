package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RandomUuidGeneratorTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private val TEST_COUNT = 512 * Runtime.getRuntime().availableProcessors()
        private val TEST_LIST = List(TEST_COUNT) { it }
    }

    private val uuidGenerator = RandomUuidGenerator()

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
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in virtual threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        StructuredTaskScopeTester()
            .roundsPerTask(TEST_COUNT * 2 * Runtimex.availableProcessors)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi jobs`() = runTest {
        val idMap = ConcurrentHashMap<UUID, Int>()

        SuspendedJobTester()
            .roundsPerJob(TEST_COUNT * 2 * Runtimex.availableProcessors)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }
}
