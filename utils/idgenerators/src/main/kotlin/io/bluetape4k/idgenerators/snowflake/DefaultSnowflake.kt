package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.getMachineId
import io.bluetape4k.idgenerators.snowflake.sequencer.DefaultSequencer


/**
 * Twitter의 Snowflake 알고리즘을 기반으로 Long 수형의 Id 값을 생성합니다.
 *
 * ```
 * val snowflake = DefaultSnowflake()
 *
 * val id = snowflake.nextId()      // generate id
 * val ids = snowflake.nextIds(10)  // generate ten id
 * ```
 *
 * @see SnowflakeId
 */
class DefaultSnowflake(
    machineId: Int = getMachineId(MAX_MACHINE_ID),
): AbstractSnowflake(DefaultSequencer(machineId))
