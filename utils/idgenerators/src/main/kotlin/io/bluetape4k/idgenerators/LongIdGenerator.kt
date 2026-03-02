package io.bluetape4k.idgenerators

import io.bluetape4k.support.assertPositiveNumber

/**
 * `Long` 식별자를 36진수 문자열로도 제공하는 생성기 계약입니다.
 *
 * ## 동작/계약
 * - 문자열 변환은 [ALPHA_NUMERIC_BASE](`Character.MAX_RADIX`)를 사용합니다.
 * - [nextIdsAsString]의 `size`는 `assertPositiveNumber`로 검증하며, `-ea`에서 위반 시 [AssertionError]가 발생합니다.
 * - 기본 구현은 [nextId], [nextIds] 동작을 그대로 재사용합니다.
 *
 * ```kotlin
 * val id = generator.nextIdAsString()
 * // id.isNotBlank() == true
 * val ids = generator.nextIdsAsString(2).toList()
 * // ids.size == 2
 * ```
 */
interface LongIdGenerator: IdGenerator<Long> {

    override fun nextIdAsString(): String = nextId().toString(ALPHA_NUMERIC_BASE)

    override fun nextIdsAsString(size: Int): Sequence<String> {
        size.assertPositiveNumber("size")
        return nextIds(size).map { it.toString(ALPHA_NUMERIC_BASE) }
    }
}
