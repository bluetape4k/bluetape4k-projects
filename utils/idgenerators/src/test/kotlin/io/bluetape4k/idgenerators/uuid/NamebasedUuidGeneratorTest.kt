package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NamebasedUuidGeneratorTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val TEST_COUNT = 512 * Runtime.getRuntime().availableProcessors()
        private val TEST_LIST = List(TEST_COUNT) { it }
    }

    // private val randomUuid = TimebasedUuidGenerator()
    private val uuidGenerator = NamebasedUuidGenerator()

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

        VirtualthreadTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi job`() = runTest {
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultijobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }
}
