package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.getMachineId
import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.snowflake.MAX_SEQUENCE
import io.bluetape4k.idgenerators.snowflake.SnowflakeId
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.withLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.absoluteValue

internal class DefaultSequencer(machineId: Int = getMachineId(MAX_MACHINE_ID)): Sequencer {

    companion object: KLogging()

    override val machineId: Int = machineId.absoluteValue % MAX_MACHINE_ID

    @Volatile
    private var lastTimestamp: Long = -1L

    private val sequencer = atomic(0)
    private var sequence: Int by sequencer

    private val lock = ReentrantLock()

    /**
     * 현재의 Timestamp와 순 증가되는 sequence 값을 제공합니다.
     * 같은 Timestamp 에서 발급할 수 있는 sequence 값이 []MAX_SEQUENCE] 값을 초과하면,
     * Timestamp를 증가 시키고, sequence를 0으로 reset 합니다.
     *
     * MAX_SEQUENCE 값보더 더 많은 sequence를 생성하려면 최대 1 msec가 소요됩니다.
     */
    override fun nextSequence(): SnowflakeId {
        lock.withLock {
            return nextSequenceInternal()
        }
    }

    override fun nextSequences(size: Int): Sequence<SnowflakeId> =
        generateSequence { nextSequence() }.take(size)
//        sequence {
//            lock.withLock {
//                repeat(size) {
//                    yield(nextSequenceInternal())
//                }
//            }
//        }

    private fun nextSequenceInternal(): SnowflakeId {
        updateState()
        return SnowflakeId(lastTimestamp, machineId, sequence)
    }

    private fun updateState() {
        var currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp == lastTimestamp) {
            sequencer.incrementAndGet()
            // sequence 가 MAX_SEQUENCE 값보다 크거나 같다면, 다음 milliseconds까지 기다립니다.
            if (sequencer.value >= MAX_SEQUENCE) {
                while (currentTimestamp == lastTimestamp) {
                    currentTimestamp = System.currentTimeMillis()
                }
                sequence = 0
                lastTimestamp = currentTimestamp
            }
        } else {
            sequence = 0
            lastTimestamp = currentTimestamp
        }
    }
}
