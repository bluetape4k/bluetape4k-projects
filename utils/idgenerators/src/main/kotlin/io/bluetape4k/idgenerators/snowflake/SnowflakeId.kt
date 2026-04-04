package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.ALPHA_NUMERIC_BASE
import io.bluetape4k.support.publicLazy
import java.io.Serializable

/**
 * Snowflake id
 *
 * ```kotlin
 * val snowflake = DefaultSnowflake()
 * val id: Long = snowflake.nextId()
 * val snowflakeId: SnowflakeId = snowflake.parse(id)
 * // snowflakeId.timestamp > 0L
 * // snowflakeId.machineId >= 0
 * // snowflakeId.sequence >= 0
 * ```
 *
 * @property timestamp  생성한 시각의 Timestamp (밀리초)
 * @property machineId  머신 ID
 * @property sequence   시퀀스 값
 */
data class SnowflakeId(
    val timestamp: Long,
    val machineId: Int,
    val sequence: Int,
): Serializable {

    /**
     * Snowflake Id 값 (Long 형)
     *
     * ```kotlin
     * val sid = SnowflakeId(System.currentTimeMillis(), 1, 0)
     * val v: Long = sid.value
     * // v > 0L
     * ```
     */
    val value: Long by publicLazy { makeId(timestamp, machineId, sequence) }

    /**
     * Snowflake Id 값을 36진수로 표현한 문자열
     *
     * ```kotlin
     * val sid = SnowflakeId(System.currentTimeMillis(), 1, 0)
     * val s: String = sid.valueAsString
     * // s.isNotBlank() == true
     * ```
     */
    val valueAsString: String by publicLazy { value.toString(ALPHA_NUMERIC_BASE) }
}
