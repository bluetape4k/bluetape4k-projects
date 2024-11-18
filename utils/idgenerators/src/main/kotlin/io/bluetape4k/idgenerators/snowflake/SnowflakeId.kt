package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.ALPHA_NUMERIC_BASE
import io.bluetape4k.support.publicLazy
import java.io.Serializable

/**
 * Snowflake id
 *
 * @property timestamp  생상한 시각의 Timestamp
 * @property machineId  머신 ID
 * @property sequence   시퀀스 값
 */
data class SnowflakeId(
    val timestamp: Long,
    val machineId: Int,
    val sequence: Int,
): Serializable {

    /**
     * Snowflake Id 값
     */
    val value: Long by publicLazy { makeId(timestamp, machineId, sequence) }

    /**
     * Snowflake Id 값을 36진수로 표현한 문자열
     */
    val valueAsString: String by publicLazy { value.toString(ALPHA_NUMERIC_BASE) }
}
