package io.bluetape4k.spring.data.exposed.jdbc.repository.support

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.springframework.data.domain.Sort

private val log = KotlinLogging.logger {}

/**
 * Spring Data [Sort]를 Exposed [SortOrder] 쌍 배열로 변환합니다.
 */
fun Sort.toExposedOrderBy(table: Table): Array<Pair<Expression<*>, SortOrder>> {
    val result = mutableListOf<Pair<Expression<*>, SortOrder>>()
    for (order in this) {
        val col: Column<*> = table.columns.firstOrNull { col ->
            col.name.equals(order.property, ignoreCase = true) ||
                    col.name.equals(toSnakeCase(order.property), ignoreCase = true)
        } ?: run {
            log.warn { "Sort property '${order.property}' not found in table '${table.tableName}', skipped." }
            null
        } ?: continue

        val sortOrder = if (order.isAscending) SortOrder.ASC else SortOrder.DESC
        result.add(col to sortOrder)
    }
    return result.toTypedArray()
}

/** camelCase → snake_case 변환. 모듈 내 공용 유틸리티. */
internal fun toSnakeCase(camelCase: String): String =
    camelCase.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

/** snake_case → camelCase 변환. 컬럼명을 Java 필드명으로 매핑할 때 사용. */
internal fun toCamelCase(snakeCase: String): String =
    snakeCase.replace(Regex("_([a-zA-Z])")) { it.groupValues[1].uppercase() }
