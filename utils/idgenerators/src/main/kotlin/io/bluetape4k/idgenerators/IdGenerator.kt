package io.bluetape4k.idgenerators

import io.bluetape4k.support.assertPositiveNumber

/**
 * Identifier 생성기
 *
 * @param ID Id 수형
 */
interface IdGenerator<ID> {

    /**
     * identifier 를 생성합니다.
     */
    fun nextId(): ID

    /**
     * identifier 를 생성하고, 문자열로 변환합니다.
     */
    fun nextIdAsString(): String

    /**
     * [size] 만큼 identifier 를 생성합니다.
     *
     * @param size 생성할 identifier 수
     * @return 생성된 identifier 시퀀스
     */
    fun nextIds(size: Int): Sequence<ID> {
        size.assertPositiveNumber("size")
        return generateSequence { nextId() }.take(size)
    }

    /**
     * [size] 만큼 identifier 를 생성하고, 문자열로 변환합니다.
     *
     * @param size 생성할 identifier 수
     * @return 생성된 identifier의 문자열의 시퀀스
     */
    fun nextIdsAsString(size: Int): Sequence<String> {
        size.assertPositiveNumber("size")
        return generateSequence { nextIdAsString() }.take(size)
    }
}
