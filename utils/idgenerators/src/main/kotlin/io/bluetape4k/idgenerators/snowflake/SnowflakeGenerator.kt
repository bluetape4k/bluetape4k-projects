package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.logging.KLogging

/**
 * [Snowflake]를 [IdGenerator]로 사용하는 어댑터 클래스.
 *
 * 기본 전략은 [Snowflakers.Default]입니다.
 * [UuidGenerator], [KsuidGenerator]와 동일한 어댑터 패턴입니다.
 *
 * ## 사용 예
 * ```kotlin
 * // 기본 (DefaultSnowflake)
 * val gen = SnowflakeGenerator()
 * val id: Long = gen.nextId()
 *
 * // GlobalSnowflake로 교체
 * val globalGen = SnowflakeGenerator(Snowflakers.Global)
 * val id2: Long = globalGen.nextId()
 *
 * // 특정 machineId로 생성
 * val customGen = SnowflakeGenerator(Snowflakers.default(machineId = 5))
 * ```
 *
 * @param snowflake 사용할 Snowflake 구현체. 기본값은 [Snowflakers.Default]
 */
class SnowflakeGenerator(
    private val snowflake: Snowflake = Snowflakers.Default,
) : IdGenerator<Long> by snowflake {
    companion object : KLogging()

    /**
     * Snowflake ID를 파싱합니다.
     *
     * ```kotlin
     * val gen = SnowflakeGenerator()
     * val id: Long = gen.nextId()
     * val parsed: SnowflakeId = gen.parse(id)
     * // parsed.timestamp > 0L
     * ```
     *
     * @see Snowflake.parse
     */
    fun parse(id: Long): SnowflakeId = snowflake.parse(id)

    /**
     * 문자열 형태의 Snowflake ID를 파싱합니다.
     *
     * ```kotlin
     * val gen = SnowflakeGenerator()
     * val idStr: String = gen.nextIdAsString()
     * val parsed: SnowflakeId = gen.parse(idStr)
     * // parsed.sequence >= 0
     * ```
     *
     * @see Snowflake.parse
     */
    fun parse(idString: String): SnowflakeId = snowflake.parse(idString)
}
