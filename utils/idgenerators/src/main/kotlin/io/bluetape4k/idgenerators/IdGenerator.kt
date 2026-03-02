package io.bluetape4k.idgenerators

import io.bluetape4k.support.assertPositiveNumber

/**
 * 식별자를 생성하는 공통 계약입니다.
 *
 * ## 동작/계약
 * - [nextIds], [nextIdsAsString]은 `size`가 1 이상이어야 하며, `assert` 활성화(`-ea`) 환경에서 위반 시 [AssertionError]가 발생합니다.
 * - 기본 구현은 `Sequence` 기반 지연 생성이므로 요청한 개수만큼 순차 계산됩니다.
 * - 생성 전략(랜덤, 시간 기반, 노드 기반)은 구현체가 결정합니다.
 *
 * ```kotlin
 * val ids = generator.nextIds(3).toList()
 * // ids.size == 3
 * val strings = generator.nextIdsAsString(2).toList()
 * // strings.size == 2
 * ```
 */
interface IdGenerator<ID> {

    /**
     * 다음 식별자 1개를 생성합니다.
     *
     * ## 동작/계약
     * - 호출할 때마다 새 식별자를 반환하는 것이 일반 계약입니다.
     * - 충돌 가능성과 정렬 특성은 구현체 알고리즘을 따릅니다.
     *
     * ```kotlin
     * val id = generator.nextId()
     * // id == [새로 생성된 식별자]
     * ```
     */
    fun nextId(): ID

    /**
     * 다음 식별자를 문자열로 생성합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [nextId]를 변환하거나 문자열 전용 생성 경로를 사용할 수 있습니다.
     * - 문자열 인코딩 방식(Base62 등)은 구현체가 결정합니다.
     *
     * ```kotlin
     * val id = generator.nextIdAsString()
     * // id.isNotBlank() == true
     * ```
     */
    fun nextIdAsString(): String

    /**
     * 지정한 개수만큼 식별자를 지연 생성합니다.
     *
     * ## 동작/계약
     * - `size <= 0`이면 `assertPositiveNumber` 검증에 실패하며 `-ea`에서 [AssertionError]가 발생합니다.
     * - 매 원소 생성 시 [nextId]가 호출됩니다.
     *
     * @param size 생성할 identifier 수
     * @throws AssertionError `-ea`에서 `size`가 1 미만인 경우
     *
     * ```kotlin
     * val ids = generator.nextIds(2).toList()
     * // ids.size == 2
     * ```
     */
    fun nextIds(size: Int): Sequence<ID> {
        size.assertPositiveNumber("size")
        return generateSequence { nextId() }.take(size)
    }

    /**
     * 지정한 개수만큼 문자열 식별자를 지연 생성합니다.
     *
     * ## 동작/계약
     * - `size <= 0`이면 `assertPositiveNumber` 검증에 실패하며 `-ea`에서 [AssertionError]가 발생합니다.
     * - 매 원소 생성 시 [nextIdAsString]이 호출됩니다.
     *
     * @param size 생성할 identifier 수
     * @throws AssertionError `-ea`에서 `size`가 1 미만인 경우
     *
     * ```kotlin
     * val ids = generator.nextIdsAsString(2).toList()
     * // ids.size == 2
     * ```
     */
    fun nextIdsAsString(size: Int): Sequence<String> {
        size.assertPositiveNumber("size")
        return generateSequence { nextIdAsString() }.take(size)
    }
}
