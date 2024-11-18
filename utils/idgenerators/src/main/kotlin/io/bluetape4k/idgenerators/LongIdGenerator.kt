package io.bluetape4k.idgenerators

import io.bluetape4k.support.assertPositiveNumber

/**
 * [Long] 수형의 Unique Identifier를 생성하는 Generator
 */
interface LongIdGenerator: IdGenerator<Long> {

    override fun nextIdAsString(): String = nextId().toString(ALPHA_NUMERIC_BASE)

    override fun nextIdsAsString(size: Int): Sequence<String> {
        size.assertPositiveNumber("size")
        return nextIds(size).map { it.toString(ALPHA_NUMERIC_BASE) }
    }
}
