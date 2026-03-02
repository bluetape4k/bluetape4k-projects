package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

/**
 * WHERE 절 필터 트리를 표현하는 sealed 타입입니다.
 *
 * ## 동작/계약
 * - `Group`과 `Where` 노드로 필터 트리를 구성합니다.
 * - [countLeaves]는 실제 조건 노드(`Where`) 개수를 재귀 계산합니다.
 * - 필터 객체는 SQL 생성 시점에 해석되며 이 타입 자체는 DB I/O를 수행하지 않습니다.
 *
 * ```kotlin
 * val filter = Filter.Group("and", mutableListOf(Filter.Where("id = :id"), Filter.Where("active = true")))
 * val leaves = filter.countLeaves()
 * // leaves == 2
 * ```
 */
sealed class Filter: Serializable {
    /**
     * 현재 트리에서 `Where` 리프 노드 개수를 반환합니다.
     *
     * ## 동작/계약
     * - `Group`은 하위 필터를 순회해 합계를 계산합니다.
     * - `Where`는 항상 `1`을 반환합니다.
     * - 트리 크기에 비례해 O(n) 시간으로 동작합니다.
     *
     * ```kotlin
     * val leaves = Filter.Where("id = :id").countLeaves()
     * // leaves == 1
     * ```
     */
    abstract fun countLeaves(): Int

    /**
     * 하위 필터를 논리 연산자로 묶는 그룹 노드입니다.
     *
     * ## 동작/계약
     * - [operator]는 SQL 조합 시 사용될 연산자 문자열(`and`, `or`)입니다.
     * - [filters]는 가변 리스트이므로 호출자가 노드를 추가/삭제할 수 있습니다.
     * - 비어 있는 [filters]의 `countLeaves()` 결과는 `0`입니다.
     *
     * ```kotlin
     * val group = Filter.Group("or", mutableListOf(Filter.Where("name like :name")))
     * val count = group.countLeaves()
     * // count == 1
     * ```
     */
    data class Group(
        val operator: String = "and",
        val filters: MutableList<Filter> = mutableListOf(),
    ): Filter() {
        override fun countLeaves(): Int {
            fun countLeaves(conditions: Group): Int =
                conditions.filters.fold(0) { count, filter -> count + filter.countLeaves() }
            return countLeaves(this)
        }

        override fun toString(): String =
            ToStringBuilder(this)
                .add("operator", operator)
                .add("filters", filters.joinToString())
                .toString()
    }

    /**
     * 단일 WHERE 조건 문자열을 담는 리프 노드입니다.
     *
     * ## 동작/계약
     * - [where] 문자열을 그대로 보관합니다.
     * - `countLeaves()`는 항상 `1`을 반환합니다.
     * - 조건 문자열 문법 검증은 SQL 실행 계층에서 처리됩니다.
     *
     * ```kotlin
     * val where = Filter.Where("id = :id")
     * val count = where.countLeaves()
     * // count == 1
     * ```
     */
    data class Where(
        val where: String,
    ): Filter() {
        override fun countLeaves(): Int = 1

        override fun toString(): String =
            ToStringBuilder(this)
                .add("where", where)
                .toString()
    }
}
