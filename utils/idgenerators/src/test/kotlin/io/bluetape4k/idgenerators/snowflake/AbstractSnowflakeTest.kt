package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.collections.asParallelStream
import io.bluetape4k.idgenerators.getMachineId
import io.bluetape4k.idgenerators.parseAsLong
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractSnowflakeTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val TEST_COUNT = MAX_SEQUENCE * 4
        private val TEST_LIST = List(TEST_COUNT) { it }
    }

    abstract val snowflake: Snowflake

    @Test
    fun `create machine id`() {
        val machineId = getMachineId(MAX_MACHINE_ID)
        machineId shouldBeInRange (0 until MAX_MACHINE_ID)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake id`() {
        snowflake.nextId()

        val ids = List(3) { snowflake.nextId() }

        ids[1] shouldBeGreaterThan ids[0]
        ids[2] shouldBeGreaterThan ids[1]

        ids.forEach {
            log.trace { "id=$it, ${snowflake.parse(it)}" }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake ids`() {
        val ids = snowflake.nextIds(TEST_COUNT).toList()
        ids shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake as parallel`() {
        val ids = TEST_LIST.asParallelStream()
            .map { snowflake.nextId() }
            .toList()
            .sorted()

        ids.distinct() shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake id as string`() {
        val id1 = snowflake.nextIdAsString()
        val id2 = snowflake.nextIdAsString()
        val id3 = snowflake.nextIdAsString()

        log.trace {
            """
            |
            |id1 = $id1
            |id2 = $id2
            |id3 = $id3
            """.trimMargin()
        }

        (id2 > id1).shouldBeTrue()
        (id3 > id2).shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake ids as string`() {
        val ids = snowflake.nextIdsAsString(TEST_COUNT).toList()
        ids.distinct() shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate snowflake id as String as parallel`() {
        val ids = TEST_LIST
            .asParallelStream()
            .map { snowflake.nextIdAsString() }
            .toList()
            .sorted()

        ids.distinct() shouldBeEqualTo ids
    }

    @Test
    fun `generate snowflake id in multi threads`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors)
            .rounds(TEST_COUNT / Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `generate snowflake id in virtual threads`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        StructuredTaskScopeTester()
            .rounds(TEST_COUNT)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @Test
    fun `generate snowflake id in coroutines`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<Long, Int>()

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors)
            .rounds(TEST_COUNT)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @Test
    fun `make and parse snowflake id`() {
        repeat(TEST_COUNT) {
            val id = snowflake.nextId()
            val parsedId = parseSnowflakeId(id)
            parsedId.value shouldBeEqualTo id
        }
    }

    @Test
    fun `make and parse snowflake id as string`() {
        repeat(TEST_COUNT) {
            val idString = snowflake.nextIdAsString()
            val parsedId = parseSnowflakeId(idString.parseAsLong(Character.MAX_RADIX))

            parsedId.valueAsString shouldBeEqualTo idString
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id`() {
        snowflake.nextId()  // for warmup

        val id1 = snowflake.nextId()
        val id2 = snowflake.nextId()

        val snowflakeId1 = snowflake.parse(id1)
        val snowflakeId2 = snowflake.parse(id2)

        Thread.sleep(1L)
        val id3 = snowflake.nextId()
        val snowflakeId3 = snowflake.parse(id3)

        snowflakeId2.timestamp shouldBeGreaterOrEqualTo snowflakeId1.timestamp
        snowflakeId3.timestamp shouldBeGreaterThan snowflakeId2.timestamp

        snowflakeId1.value shouldBeEqualTo id1
        snowflakeId2.value shouldBeEqualTo id2
        snowflakeId3.value shouldBeEqualTo id3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as sequence`() {
        val ids = snowflake.nextIds(TEST_COUNT).toList()
        val snowflakeIds = ids.map { snowflake.parse(it) }

        snowflakeIds.map { it.value } shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as parallel`() {
        val ids = snowflake.nextIds(TEST_COUNT).toList()
        val snowflakeIds = ids.asParallelStream()
            .map { snowflake.parse(it) }
            .toList()

        snowflakeIds.map { it.value } shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id as string`() {
        val id1 = snowflake.nextIdAsString()
        val id2 = snowflake.nextIdAsString()
        Thread.sleep(1L)
        val id3 = snowflake.nextIdAsString()
        val id4 = snowflake.nextIdAsString()

        val snowflakeId1 = snowflake.parse(id1)
        val snowflakeId2 = snowflake.parse(id2)
        val snowflakeId3 = snowflake.parse(id3)
        val snowflakeId4 = snowflake.parse(id4)

        snowflakeId1.valueAsString shouldBeEqualTo id1
        snowflakeId2.valueAsString shouldBeEqualTo id2
        snowflakeId3.valueAsString shouldBeEqualTo id3
        snowflakeId4.valueAsString shouldBeEqualTo id4

        log.trace { "snowflakeId1=$snowflakeId1" }
        log.trace { "snowflakeId2=$snowflakeId2" }
        log.trace { "snowflakeId3=$snowflakeId3" }
        log.trace { "snowflakeId4=$snowflakeId4" }

        snowflakeId1.value shouldBeEqualTo id1.parseAsLong()

        snowflakeId2.timestamp shouldBeGreaterOrEqualTo snowflakeId1.timestamp
        snowflakeId3.timestamp shouldBeGreaterThan snowflakeId2.timestamp

        snowflakeId4.timestamp shouldBeGreaterOrEqualTo snowflakeId3.timestamp
        snowflakeId4.timestamp shouldBeGreaterThan snowflakeId2.timestamp
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as String`() {
        val ids = snowflake.nextIdsAsString(TEST_COUNT).toList()
        val snowflakeIds = ids.map { snowflake.parse(it).value }

        snowflakeIds.distinct() shouldBeEqualTo ids.map { it.parseAsLong() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id as String as parallel`() {
        val ids = snowflake.nextIdsAsString(TEST_COUNT).toList()
        val snowflakeIds = ids
            .asParallelStream()
            .map { snowflake.parse(it).value }
            .toList()
            .sorted()

        snowflakeIds.distinct() shouldBeEqualTo ids.map { it.parseAsLong() }
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id in virtual threads`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as sequence in suspended jobs`() = runTest {
        val idMap = ConcurrentHashMap<Long, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id in multi threading`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id in virtual threading`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id in suspend jobs`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<Long, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id as base62 in multi threading`() {
        val idMap = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id as base62 in virtual threads`() {
        val idMap = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake id as base62 in suspended jobs`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .add {
                val id = snowflake.nextId().encodeBase62()
                log.trace { "base62=$id" }
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }


    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as sequence in multi threading`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as sequence in virtual threads`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse snowflake ids as sequence in suspend jobs`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<Long, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .add {
                val ids = snowflake.nextIds(10)
                ids.forEach { id ->
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }
            }
            .run()
    }
}
