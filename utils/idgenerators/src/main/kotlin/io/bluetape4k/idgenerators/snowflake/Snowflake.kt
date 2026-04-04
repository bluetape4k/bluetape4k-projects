package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.ALPHA_NUMERIC_BASE
import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.idgenerators.parseAsLong
import io.bluetape4k.support.assertNotBlank


/**
 * Twitter Snowflake 알고리즘을 이용하여 Time 기반의 Long 수형의 Unique Id 를 생성합니다.
 *
 * ```kotlin
 * val snowflake: Snowflake = DefaultSnowflake()
 * val id: Long = snowflake.nextId()
 * val idStr: String = snowflake.nextIdAsString()
 * val parsed: SnowflakeId = snowflake.parse(id)
 * // parsed.timestamp > 0L
 * ```
 */
interface Snowflake: IdGenerator<Long> {

    /**
     * Snowflake ID를 36진수 문자열로 반환합니다.
     *
     * ```kotlin
     * val snowflake: Snowflake = DefaultSnowflake()
     * val idStr: String = snowflake.nextIdAsString()
     * // idStr.isNotBlank() == true
     * ```
     */
    override fun nextIdAsString(): String = nextId().toString(ALPHA_NUMERIC_BASE)

    /**
     * Long 형 Snowflake ID를 파싱하여 [SnowflakeId]로 변환합니다.
     *
     * ```kotlin
     * val snowflake: Snowflake = DefaultSnowflake()
     * val id: Long = snowflake.nextId()
     * val parsed: SnowflakeId = snowflake.parse(id)
     * // parsed.machineId >= 0
     * ```
     */
    fun parse(id: Long): SnowflakeId = parseSnowflakeId(id)

    /**
     * 36진수 문자열 형태의 Snowflake ID를 파싱하여 [SnowflakeId]로 변환합니다.
     *
     * ```kotlin
     * val snowflake: Snowflake = DefaultSnowflake()
     * val idStr: String = snowflake.nextIdAsString()
     * val parsed: SnowflakeId = snowflake.parse(idStr)
     * // parsed.sequence >= 0
     * ```
     */
    fun parse(idString: String): SnowflakeId {
        idString.assertNotBlank("idString")

        val id = idString.parseAsLong()
        return parseSnowflakeId(id)
    }
}
