package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.snowflake.MAX_MACHINE_ID
import io.bluetape4k.idgenerators.snowflake.MAX_SEQUENCE
import io.bluetape4k.idgenerators.snowflake.SnowflakeId
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * MachineId 구분 없이 Sequence를 생성합니다.
 *
 * 같은 Timestamp 에서 발급할 수 있는 sequence 값이 [MAX_SEQUENCE] 값을 초과하면,
 * Machine Id를 증가시키고, MachineId 가 [MAX_MACHINE_ID] 보다 커지면
 * 시스템 시각이 lastTimestamp보다 커지기를 기다렸다가 machineId와 sequence를 reset한 값을 제공합니다.
 *
 * MAX_MACHINE_ID * MAX_SEQUENCE 값보더 더 많은 sequence를 생성하려면 최대 1 msec가 소요됩니다.
 */
class GlobalSequencer: Sequencer {

    companion object: KLogging()

    @Volatile
    private var lastTimestamp: Long = -1L

    private val machineIdSequencer = atomic(0)
    override var machineId: Int by machineIdSequencer

    private val sequencer = atomic(0)
    private var sequence by sequencer

    private val lock = ReentrantLock()

    /**
     * 현재의 Timestamp와 순 증가되는 sequence 값을 제공합니다.
     *
     * 같은 Timestamp 에서 발급할 수 있는 sequence 값이 [MAX_SEQUENCE] 값을 초과하면,
     * Machine Id를 증가시키고, MachineId 가 [MAX_MACHINE_ID] 보다 커지면
     * 시스템 시각이 lastTimestamp보다 커지기를 기다렸다가 machineId와 sequence를 reset한 값을 제공합니다.
     *
     * MAX_MACHINE_ID * MAX_SEQUENCE 값보더 더 많은 sequence를 생성하려면 최대 1 msec가 소요됩니다.
     */
    override fun nextSequence(): SnowflakeId =
        lock.withLock {
            nextSequenceInternal()
        }

    override fun nextSequences(size: Int): Sequence<SnowflakeId> =
        generateSequence { nextSequence() }.take(size)

    private fun nextSequenceInternal(): SnowflakeId {
        updateState()
        return SnowflakeId(lastTimestamp, machineId, sequence)
    }

    private fun updateState() {
        var currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp == lastTimestamp) {
            sequencer.incrementAndGet()
            if (sequence >= MAX_SEQUENCE) {
                machineIdSequencer.incrementAndGet()
                // sequence 가 MAX_SEQUENCE 값보다 크거나 같다면, 다음 milliseconds까지 기다립니다.
                if (sequence >= MAX_SEQUENCE) {
                    while (currentTimestamp == lastTimestamp) {
                        currentTimestamp = System.currentTimeMillis()
                    }
                    machineId = 0
                    lastTimestamp = currentTimestamp
                }
                sequence = 0
            }
        } else {
            // Reset sequence and machine id
            sequence = 0
            machineId = 0
            lastTimestamp = currentTimestamp
        }
    }
}
