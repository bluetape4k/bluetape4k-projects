package io.bluetape4k.r2dbc.query

import io.bluetape4k.ToStringBuilder
import java.io.Serializable

sealed class Filter: Serializable {

    abstract fun countLeaves(): Int

    class Group(
        val operator: String = "and",
        val filters: MutableList<Filter> = mutableListOf(),
    ): Filter() {

        override fun countLeaves(): Int {
            fun countLeaves(conditions: Group): Int =
                conditions.filters.fold(0) { count, filter -> count + filter.countLeaves() }
            return countLeaves(this)
        }

        override fun toString(): String {
            return ToStringBuilder(this)
                .add("operator", operator)
                .add("filters", filters.joinToString())
                .toString()
        }
    }

    class Where(val where: String): Filter() {
        override fun countLeaves(): Int = 1

        override fun toString(): String {
            return ToStringBuilder(this)
                .add("where", where)
                .toString()
        }
    }
}
