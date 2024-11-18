package io.bluetape4k.idgenerators.snowflake.sequencer

import io.bluetape4k.idgenerators.snowflake.SnowflakeId

/**
 * Snowflake Id 생성 시 사용하는 Sequencer
 */
interface Sequencer {

    val machineId: Int

    fun nextSequence(): SnowflakeId

    fun nextSequences(size: Int): Sequence<SnowflakeId>
}
