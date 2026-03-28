package io.bluetape4k.spring.data.exposed.jdbc.repository.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toSnakeCase
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.notLike
import org.jetbrains.exposed.v1.core.or
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.ParameterAccessor
import org.springframework.data.repository.query.parser.AbstractQueryCreator
import org.springframework.data.repository.query.parser.Part
import org.springframework.data.repository.query.parser.PartTree

/**
 * Spring Data [PartTree]를 Exposed [Op]<[Boolean]> 조건으로 변환합니다.
 *
 * Spring Data 4.x [AbstractQueryCreator] API:
 * - Constructor: `(PartTree, ParameterAccessor)`
 * - `create(Part, Iterator<Any>)`: 단일 Part → 조건 생성
 * - `and(Part, S, Iterator<Any>)`: Part → 조건 생성 후 base와 AND 결합 (3-param)
 * - `or(S, S)`: 두 조건을 OR 결합
 * - `complete(S?, Sort)`: 최종 결과 반환
 */
class ExposedQueryCreator(
    tree: PartTree,
    accessor: ParameterAccessor,
    private val table: Table,
): AbstractQueryCreator<Op<Boolean>, Op<Boolean>>(tree, accessor) {

    companion object: KLogging() {
        /** LIKE 패턴에서 와일드카드 문자(`%`, `_`)를 이스케이프합니다. */
        fun escapeLikeWildcards(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
    }

    override fun create(part: Part, iterator: Iterator<Any>): Op<Boolean> =
        buildCondition(part, iterator)

    override fun and(part: Part, base: Op<Boolean>, iterator: Iterator<Any>): Op<Boolean> =
        base.and(buildCondition(part, iterator))

    override fun or(base: Op<Boolean>, criteria: Op<Boolean>): Op<Boolean> =
        base.or(criteria)

    override fun complete(criteria: Op<Boolean>?, _sort: Sort): Op<Boolean> =
        criteria ?: Op.TRUE

    @Suppress("UNCHECKED_CAST")
    private fun buildCondition(part: Part, iterator: Iterator<Any>): Op<Boolean> {
        val colName = toColumnName(part.property.segment)
        val column = table.columns.firstOrNull { it.name.equals(colName, ignoreCase = true) }
            ?: error("Column '$colName' not found in table '${table.tableName}'")

        return when (part.type) {
            Part.Type.SIMPLE_PROPERTY               ->
                (column as Column<Any>).eq(iterator.next())

            Part.Type.NEGATING_SIMPLE_PROPERTY      ->
                (column as Column<Any>).neq(iterator.next())

            Part.Type.GREATER_THAN, Part.Type.AFTER ->
                (column as Column<Comparable<Any>>).greater(iterator.next() as Comparable<Any>)

            Part.Type.GREATER_THAN_EQUAL            ->
                (column as Column<Comparable<Any>>).greaterEq(iterator.next() as Comparable<Any>)

            Part.Type.LESS_THAN, Part.Type.BEFORE   ->
                (column as Column<Comparable<Any>>).less(iterator.next() as Comparable<Any>)

            Part.Type.LESS_THAN_EQUAL               ->
                (column as Column<Comparable<Any>>).lessEq(iterator.next() as Comparable<Any>)

            Part.Type.BETWEEN                       -> {
                val first = iterator.next() as Comparable<Any>
                val second = iterator.next() as Comparable<Any>
                (column as Column<Comparable<Any>>).between(first, second)
            }

            Part.Type.IS_NULL                       -> column.isNull()
            Part.Type.IS_NOT_NULL                   -> column.isNotNull()

            Part.Type.LIKE                          ->
                (column as Column<String>).like(iterator.next() as String)

            Part.Type.NOT_LIKE                      ->
                (column as Column<String>).notLike(iterator.next() as String)

            Part.Type.STARTING_WITH                 ->
                (column as Column<String>).like("${escapeLikeWildcards(iterator.next() as String)}%")

            Part.Type.ENDING_WITH                   ->
                (column as Column<String>).like("%${escapeLikeWildcards(iterator.next() as String)}")

            Part.Type.CONTAINING                    ->
                (column as Column<String>).like("%${escapeLikeWildcards(iterator.next() as String)}%")

            Part.Type.NOT_CONTAINING                ->
                (column as Column<String>).notLike("%${escapeLikeWildcards(iterator.next() as String)}%")

            Part.Type.IN                            ->
                (column as Column<Any>).inList(iterator.next() as Collection<Any>)

            Part.Type.NOT_IN                        ->
                (column as Column<Any>).notInList(iterator.next() as Collection<Any>)

            Part.Type.TRUE                          ->
                (column as Column<Boolean>).eq(true)

            Part.Type.FALSE                         ->
                (column as Column<Boolean>).eq(false)

            Part.Type.EXISTS                        -> column.isNotNull()

            else                                    -> error("Unsupported Part.Type: ${part.type}")
        }
    }

    private fun toColumnName(propertyName: String): String = toSnakeCase(propertyName)

}
