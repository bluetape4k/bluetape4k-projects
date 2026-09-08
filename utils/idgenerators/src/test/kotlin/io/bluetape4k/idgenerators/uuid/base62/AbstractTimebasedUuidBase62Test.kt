package io.bluetape4k.idgenerators.uuid.base62

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.codec.decodeBase62AsUuid
import io.bluetape4k.codec.encodeBase62
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
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

abstract class AbstractTimebasedUuidBase62Test {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val STRESS_OPERATIONS = 10_000
        private const val STRESS_WORKERS = 8
        private const val STRESS_ROUNDS_PER_WORKER = STRESS_OPERATIONS / STRESS_WORKERS
        private val STRESS_TIMEOUT = 30.seconds
        private val TEST_LIST = List(STRESS_OPERATIONS) { it }
    }

    protected abstract val uuidGenerator: IdGenerator<UUID>

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuid`() {
        val u1 = uuidGenerator.nextIdAsString()
        val u2 = uuidGenerator.nextIdAsString()
        val u3 = uuidGenerator.nextIdAsString()

        listOf(u1, u2, u3).forEach {
            log.debug { "uuid=$it" }
        }

        (u2 > u1).shouldBeTrue()
        (u3 > u2).shouldBeTrue()

        // u1.version() shouldBeEqualTo 6   // Time based
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuid with size`() {

        val uuids = uuidGenerator.nextIdsAsString(STRESS_OPERATIONS).toList()
        val sorted = uuids.sorted()

        sorted.forEachIndexed { index, uuid ->
            uuid shouldBeEqualTo sorted[index]
        }

        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids as parallel`() {
        val uuids = TEST_LIST.parallelStream()
            .map { uuidGenerator.nextIdAsString() }
            .toList()
            .sorted()

        // 중복 발행은 없어야 한다
        uuids.distinct().size shouldBeEqualTo uuids.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in multi threads`() {
        val idMap = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_ROUNDS_PER_WORKER)
            .add {
                val id = uuidGenerator.nextIdAsString()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in virtual threads`() {
        val idMap = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_OPERATIONS)
            .withTimeout(STRESS_TIMEOUT)
            .add {
                val id = uuidGenerator.nextIdAsString()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate timebased uuids in suspend jobs`() = runSuspendDefault(timeout = STRESS_TIMEOUT) {
        val idMap = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .workers(STRESS_WORKERS)
            .rounds(STRESS_OPERATIONS)
            .add {
                val id = uuidGenerator.nextIdAsString()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()

        idMap.size shouldBeEqualTo STRESS_OPERATIONS
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert timebased uuids to hashids`() {
        val hashids = Hashids()

        val uuids = TEST_LIST.parallelStream().map { uuidGenerator.nextIdAsString() }.toList()
        val encodeds = uuids.map { hashids.encode(*it.decodeBase62AsUuid().toLongArray()) }

        val decodeds = encodeds.map { hashids.decode(it).toUUID().encodeBase62() }
        decodeds shouldContainSame uuids
    }
}
