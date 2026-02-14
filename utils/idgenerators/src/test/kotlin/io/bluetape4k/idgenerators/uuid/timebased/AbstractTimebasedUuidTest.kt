package io.bluetape4k.idgenerators.uuid.timebased

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.stream.toFastList
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.hashids.Hashids
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toLongArray
import io.bluetape4k.support.toUUID
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertTrue

abstract class AbstractTimebasedUuidTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private val TEST_COUNT = 1024 * Runtime.getRuntime().availableProcessors()
        private val TEST_LIST = fastList(TEST_COUNT) { it }
    }

    protected abstract val uuidGenerator: IdGenerator<UUID>

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

        val uuids = uuidGenerator.nextIds(TEST_COUNT).toFastList()
        val sorted = uuids.sorted()

        sorted.forEachIndexed { index, uuid ->
            uuid shouldBeEqualTo sorted[index]
        }

        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids as parallel`() {
        val uuids = TEST_LIST.parallelStream()
            .map { uuidGenerator.nextId() }
            .toFastList()
            .sorted()

        // 중복 발행은 없어야 한다
        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(TEST_COUNT)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in virtual threads`() {
        val idMap = ConcurrentHashMap<UUID, Int>()

        StructuredTaskScopeTester()
            .rounds(TEST_COUNT * 2 * Runtimex.availableProcessors)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in suspend jobs`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<UUID, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(TEST_COUNT * 2 * Runtimex.availableProcessors)
            .add {
                val id = uuidGenerator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert timebased uuids to hashids`() {
        val hashids = Hashids()

        val uuids = TEST_LIST.parallelStream().map { uuidGenerator.nextId() }.toFastList()
        val encodeds = uuids.map { hashids.encode(*it.toLongArray()) }

        val decodeds = encodeds.map { hashids.decode(it).toUUID() }
        decodeds shouldContainSame uuids
    }
}
