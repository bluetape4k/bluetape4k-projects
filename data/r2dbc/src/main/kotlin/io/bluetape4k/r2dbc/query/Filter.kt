package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

/**
 * 쿼리 필터를 표현하는 sealed class입니다.
 *
 * WHERE 절의 조건들을 트리 구조로 표현할 수 있습니다.
 */
sealed class Filter: Serializable {
    /**
     * 리프 노드(Where 조건)의 개수를 반환합니다.
     */
    abstract fun countLeaves(): Int

    /**
     * 여러 필터를 논리 연산자로 그룹화합니다.
     *
     * @property operator 논리 연산자 (and, or)
     * @property filters 하위 필터 목록
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
     * 단일 WHERE 조건을 표현합니다.
     *
     * @property where WHERE 조건 문자열
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
