package io.bluetape4k.r2dbc.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.r2dbc.support.toParameter
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.Systemx
import io.r2dbc.spi.Parameters
import kotlin.reflect.KProperty

/**
 * QueryBuilder는 where 절과 파라미터가 선택적일 수 있는 동적 쿼리를 쉽게 작성하기 위한 간단한 SQL 쿼리 빌더입니다.
 *
 * 이 클래스는 형 안정성이 보장된 빌더가 아닙니다. 단순히 SQL 문의 조각을 어떤 순서로든 추가하고 문을 빌드합니다.
 * 이 클래스가 제공하는 주요한 기능은 where 절을 연산자로 결합할 수 있게 하고, 그룹이 비어있는 경우를 처리하는 것입니다.
 */
class QueryBuilder {

    companion object: KLogging()

    private val selects = LinkedHashSet<String>()
    private var selectCount: String? = null
    private val params = mutableMapOf<String, Any>()
    private var filters: Filter.Group = Filter.Group()
    private var groupBy: String? = null
    private var having: String? = null
    private var orderBy: String? = null
    private var limit: Int? = null
    private var offset: Int? = null

    fun select(table: String) = apply {
        selects.add(table)
    }

    fun selectCount(selectCount: String) = apply {
        this.selectCount = selectCount
    }

    fun parameter(name: String, value: Any) = apply {
        params[name] = value
    }

    fun parameter(name: String, value: Any?, type: Class<*>) = apply {
        params[name] = value.toParameter(type)
    }

    fun parameter(property: KProperty<*>, value: Any) = apply {
        params[property.name] = value
    }

    fun parameter(property: KProperty<*>, value: Any?, type: Class<*>) = apply {
        params[property.name] = value.toParameter(type)
    }

    fun parameterNull(name: String, type: Class<*>) = apply {
        params[name] = Parameters.`in`(type)
    }

    fun parameterNull(property: KProperty<*>, type: Class<*>) = apply {
        params[property.name] = Parameters.`in`(type)
    }

    fun groupBy(groupBy: String) = apply {
        this.groupBy = groupBy
    }

    fun groupBy(groupBy: KProperty<*>) = apply {
        this.groupBy = groupBy.name
    }


    fun having(having: String) = apply {
        this.having = having
    }

    fun orderBy(orderBy: String) = apply {
        this.orderBy = orderBy
    }

    fun limit(limit: Int) = apply {
        this.limit = limit
    }

    fun offset(offset: Int) = apply {
        this.offset = offset
    }

    fun whereGroup(operator: String = "and", block: FilterBuilder.() -> Unit) {
        operator.requireNotBlank("operator")
        require(filters.countLeaves() == 0) { "There must be only one root filters group" }

        filters = Filter.Group(operator.trim())
        block(FilterBuilder(filters))
    }

    inner class FilterBuilder(private val group: Filter.Group) {
        fun where(where: String) {
            // blank condition은 결국 `where` 절의 잘못된 SQL을 만들기 때문에 조기에 차단한다.
            require(where.isNotBlank()) { "where clause must not be blank." }
            group.filters.add(Filter.Where(where))
        }

        fun whereGroup(operator: String = "and", block: FilterBuilder.() -> Unit) {
            operator.requireNotBlank("operator")

            val inner = Filter.Group(operator.trim())
            group.filters.add(inner)
            block(FilterBuilder(inner))
        }
    }

    fun build(sb: StringBuilder = StringBuilder(), block: QueryBuilder.() -> Unit): Query {
        block(this)

        selects.joinTo(sb, Systemx.lineSeparator)
        if (filters.countLeaves() != 0) {
            if (selects.isNotEmpty()) sb.appendLine()
            sb.append("where ")
            appendConditions(sb, filters, true)
        }
        groupBy?.run { sb.appendLine().append("group by ").append(groupBy) }
        having?.run { sb.appendLine().append("having ").append(having) }
        orderBy?.run { sb.appendLine().append("order by ").append(orderBy) }
        limit?.run { sb.appendLine().append("limit ").append(limit) }
        offset?.run { sb.appendLine().append("offset ").append(offset) }

        return Query(sb, params).apply {
            log.debug { "query. $this" }
        }
    }

    fun buildCount(sb: StringBuilder = StringBuilder(), block: QueryBuilder.() -> Unit): Query {
        block(this)

        selectCount?.run { sb.append(selectCount) }
        if (filters.countLeaves() != 0) {
            if (selectCount != null) sb.appendLine()
            sb.append("where ")
            appendConditions(sb, filters, true)
        }
        return Query(sb, params).apply {
            log.debug { "count query. $this" }
        }
    }

    private fun appendConditions(sb: StringBuilder, conditions: Filter, root: Boolean) {
        when (conditions) {
            is Filter.Where -> sb.append(conditions.where)
            is Filter.Group -> appendConditions(conditions, root, sb)
        }
    }

    private fun appendConditions(conditions: Filter.Group, root: Boolean, sb: StringBuilder) {
        val filtered = conditions.filters.filter { it.countLeaves() != 0 }
        when (filtered.size) {
            0    -> Unit
            1    -> appendConditions(sb, filtered.first(), false)
            else -> {
                if (!root) sb.appendLine().append("(")
                filtered.forEachIndexed { index, cond ->
                    log.debug { "append condition group index=$index, condition=$cond" }
                    appendConditions(sb, cond, false)
                    if (index != filtered.indices.last) sb.append(" ${conditions.operator} ")
                }

                if (!root) sb.append(")")
            }
        }
    }
}
