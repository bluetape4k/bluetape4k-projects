package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.snowflake.sequencer.Sequencer
import io.bluetape4k.support.assertPositiveNumber


/**
 * [Snowflake]의 최상위 추상화 클래스
 *
 * @property sequencer Snowflake Id 생성에 사용하는 Sequencer
 */
abstract class AbstractSnowflake(val sequencer: Sequencer): Snowflake {

    /**
     * Snowflake 알고리즘으로 Long 수형의 Id를 생성합니다.
     */
    override fun nextId(): Long {
        return sequencer.nextSequence().value
    }

    override fun nextIds(size: Int): Sequence<Long> {
        size.assertPositiveNumber("size")
        return sequencer.nextSequences(size).map { it.value }
    }
}
