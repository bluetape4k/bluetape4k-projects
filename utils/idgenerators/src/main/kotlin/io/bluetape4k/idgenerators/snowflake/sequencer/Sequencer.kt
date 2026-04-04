package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.snowflake.SnowflakeId

/**
 * Snowflake Id 생성 시 사용하는 Sequencer
 *
 * ```kotlin
 * val sequencer: Sequencer = DefaultSequencer(machineId = 1)
 * val sid: SnowflakeId = sequencer.nextSequence()
 * // sid.machineId == 1
 * val sids: List<SnowflakeId> = sequencer.nextSequences(3).toList()
 * // sids.size == 3
 * ```
 */
interface Sequencer {

    /**
     * 이 Sequencer가 사용하는 머신 ID
     *
     * ```kotlin
     * val sequencer: Sequencer = DefaultSequencer(machineId = 5)
     * // sequencer.machineId == 5
     * ```
     */
    val machineId: Int

    /**
     * 다음 [SnowflakeId]를 생성합니다.
     *
     * ```kotlin
     * val sequencer: Sequencer = DefaultSequencer()
     * val sid: SnowflakeId = sequencer.nextSequence()
     * // sid.value > 0L
     * ```
     */
    fun nextSequence(): SnowflakeId

    /**
     * 지정한 개수만큼 [SnowflakeId]를 지연 생성합니다.
     *
     * ```kotlin
     * val sequencer: Sequencer = DefaultSequencer()
     * val sids: List<SnowflakeId> = sequencer.nextSequences(5).toList()
     * // sids.size == 5
     * ```
     *
     * @param size 생성할 SnowflakeId 수
     */
    fun nextSequences(size: Int): Sequence<SnowflakeId>
}
