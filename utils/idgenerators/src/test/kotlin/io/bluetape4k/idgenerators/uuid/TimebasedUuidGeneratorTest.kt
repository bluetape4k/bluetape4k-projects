package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.idgenerators.hashids.Hashids
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toLongArray
import io.bluetape4k.support.toUUID
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.RepeatedTest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertTrue

class TimebasedUuidGeneratorTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val TEST_COUNT = 512 * Runtime.getRuntime().availableProcessors()
        private val TEST_LIST = List(TEST_COUNT) { it }
    }

    private val uuidGenerator = TimebasedUuidGenerator()

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuid`() {
        val u1 = uuidGenerator.nextId()
        val u2 = uuidGenerator.nextId()
        val u3 = uuidGenerator.nextId()

        listOf(u1, u2, u3).forEach {
            log.debug { "uuid=$it" }
        }

        assertTrue { u2 > u1 }
        assertTrue { u3 > u2 }

        // u1.version() shouldBeEqualTo 6   // Time based
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuid with size`() {

        val uuids = uuidGenerator.nextIds(TEST_COUNT).toList()
        val sorted = uuids.sorted().toList()

        sorted.forEachIndexed { index, uuid ->
            uuid shouldBeEqualTo sorted[index]
        }

        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids as parallel`() {
        val uuids = TEST_LIST.parallelStream()
            .map { uuidGenerator.nextId() }
            .toList()
            .sorted()

        // 중복 발행은 없어야 한다
        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultithreadingTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextUUID()
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
                val id = uuidGenerator.nextUUID()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi jobs`() = runTest {
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultijobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextUUID()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert timebased uuids to hashids`() {
        val hashids = Hashids()

        val uuids = TEST_LIST.parallelStream().map { uuidGenerator.nextUUID() }.toList()
        val encodeds = uuids.map { hashids.encode(*it.toLongArray()) }

        val decodeds = encodeds.map { hashids.decode(it).toUUID() }
        decodeds shouldContainSame uuids
    }
}
