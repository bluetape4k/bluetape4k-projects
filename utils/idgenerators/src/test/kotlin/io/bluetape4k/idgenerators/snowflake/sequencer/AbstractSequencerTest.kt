package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.snowflake.MAX_SEQUENCE
import io.bluetape4k.idgenerators.snowflake.SnowflakeId
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.IntStream
import kotlin.math.absoluteValue

abstract class AbstractSequencerTest {

    companion object: KLoggingChannel() {
        private const val TEST_SIZE: Int = MAX_SEQUENCE * 2
        private const val REPEAT_SIZE = 3
    }

    protected abstract val sequencer: Sequencer

    private val comparator = Comparator { id1: SnowflakeId, id2: SnowflakeId ->
        var comparison = (id1.timestamp - id2.timestamp).toInt()
        if (comparison == 0) {
            comparison = id1.machineId - id2.machineId
        }
        comparison
    }

    @BeforeAll
    fun setup() {
        // Warm up sequencer
        repeat(TEST_SIZE) {
            sequencer.nextSequence()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate sequence`() {
        val ids = List(TEST_SIZE) { sequencer.nextSequence() }

        ids shouldHaveSize TEST_SIZE
        ids.distinct() shouldBeEqualTo ids
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate sequence as parallel`() {
        val ids = IntStream.range(0, TEST_SIZE)
            .parallel()
            .mapToObj { sequencer.nextSequence() }
            .toList()

        //ids.count { it.machineId > 0 } shouldBeGreaterThan 0
        ids.distinct().size shouldBeEqualTo ids.size

        val resetIds = ids.filter { it.sequence == 0 }.sortedWith(comparator)
        resetIds.size shouldBeLessThan ids.size
    }

    @ParameterizedTest(name = "create Snowflake instance with machineId {0}")
    @ValueSource(ints = [-1000, -100, -1, 0, 1, 100, 1000])
    fun `create Snowflake instance with invalid machineId`(machineId: Int) {
        val sequencer = DefaultSequencer(machineId)
        sequencer.machineId shouldBeEqualTo (machineId.absoluteValue % MAX_MACHINE_ID)
    }

    @Test
    fun `generate sequence in multi-threading`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        MultithreadingTester()
            .workers(4 * Runtimex.availableProcessors)
            .rounds(MAX_SEQUENCE * 2)
            .add {
                val id = sequencer.nextSequence()
                idMap.putIfAbsent(id.value, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `generate sequence in virtual threads`() {
        val idMap = ConcurrentHashMap<Long, Int>()

        StructuredTaskScopeTester()
            .rounds(MAX_SEQUENCE * 2 * 4 * Runtimex.availableProcessors)
            .add {
                val id = sequencer.nextSequence()
                idMap.putIfAbsent(id.value, 1).shouldBeNull()
            }
            .run()
    }

    @Test
    fun `generate sequence in multi job`() = runSuspendDefault {
        val idMap = ConcurrentHashMap<Long, Int>()

        SuspendedJobTester()
            .rounds(MAX_SEQUENCE * 2 * Runtimex.availableProcessors)
            .add {
                val id = sequencer.nextSequence()
                idMap.putIfAbsent(id.value, 1).shouldBeNull()
            }
            .run()
    }
}
