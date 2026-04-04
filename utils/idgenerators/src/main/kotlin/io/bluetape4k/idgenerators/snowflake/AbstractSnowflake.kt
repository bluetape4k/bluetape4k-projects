package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.snowflake.sequencer.Sequencer
import io.bluetape4k.support.assertPositiveNumber


/**
 * [Snowflake]의 최상위 추상화 클래스
 *
 * ```kotlin
 * val snowflake = DefaultSnowflake()
 * val id: Long = snowflake.nextId()
 * // id > 0L
 * ```
 *
 * @property sequencer Snowflake Id 생성에 사용하는 Sequencer
 */
abstract class AbstractSnowflake(val sequencer: Sequencer): Snowflake {

    /**
     * Snowflake 알고리즘으로 Long 수형의 Id를 생성합니다.
     *
     * ```kotlin
     * val snowflake = DefaultSnowflake()
     * val id: Long = snowflake.nextId()
     * // id > 0L
     * ```
     */
    override fun nextId(): Long {
        return sequencer.nextSequence().value
    }

    /**
     * 지정한 개수만큼 Snowflake ID를 지연 생성합니다.
     *
     * ```kotlin
     * val snowflake = DefaultSnowflake()
     * val ids: List<Long> = snowflake.nextIds(5).toList()
     * // ids.size == 5
     * ```
     *
     * @param size 생성할 ID 수
     */
    override fun nextIds(size: Int): Sequence<Long> {
        size.assertPositiveNumber("size")
        return sequencer.nextSequences(size).map { it.value }
    }
}
