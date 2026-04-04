package io.bluetape4k.spring.data.exposed.jdbc.repository.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedEntityInformation
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toExposedOrderBy
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.RepositoryQuery
import org.springframework.data.repository.query.parser.PartTree

/**
 * 메서드명 기반 PartTree 쿼리를 Exposed DAO로 실행합니다.
 *
 * ```kotlin
 * // ExposedQueryLookupStrategy가 내부적으로 생성합니다.
 * // 아래 메서드명은 자동으로 PartTreeExposedQuery로 처리됩니다.
 * interface UserRepository : ExposedJdbcRepository<User, Long> {
 *     fun findByName(name: String): List<User>
 *     fun findByAgeGreaterThan(age: Int): List<User>
 *     fun findTop3ByNameContaining(keyword: String): List<User>
 *     fun countByAgeGreaterThanEqual(age: Int): Long
 *     fun existsByName(name: String): Boolean
 * }
 * ```
 */
class PartTreeExposedQuery<E : Entity<ID>, ID : Any>(
    private val queryMethod: ExposedQueryMethod,
    private val entityInformation: ExposedEntityInformation<E, ID>,
) : RepositoryQuery {

    companion object: KLogging()

    private val entityClass: EntityClass<ID, E> = entityInformation.entityClass
    private val partTree: PartTree = PartTree(queryMethod.name, queryMethod.entityInformation.javaType)

    override fun getQueryMethod(): ExposedQueryMethod = queryMethod

    @Suppress("UNCHECKED_CAST")
    override fun execute(parameters: Array<out Any>): Any? {
        val provider = ParameterMetadataProvider.of(queryMethod.parameters, parameters as Array<Any?>)
        val op = ExposedQueryCreator(partTree, provider.accessor, entityInformation.table).createQuery()

        val pageable = parameters.firstInstanceOrNull<Pageable>() ?: Pageable.unpaged()
        val sort = partTree.sort
            .and(pageable.sort)
            .and(parameters.firstInstanceOrNull<Sort>() ?: Sort.unsorted())

        return when {
            partTree.isDelete -> executeDelete(op)
            partTree.isCountProjection -> entityClass.find { op }.count()
            partTree.isExistsProjection -> !entityClass.find { op }.empty()
            partTree.isLimiting -> executeLimiting(op, partTree.maxResults, sort)
            isPageQuery() -> executePageQuery(op, pageable, sort)
            isSliceQuery() -> executeSliceQuery(op, pageable, sort)
            isSingleResult() -> entityClass.find { op }.let { query ->
                if (sort.isSorted) {
                    query.orderBy(*sort.toExposedOrderBy(entityInformation.table))
                }
                query.firstOrNull()
            }
            else -> entityClass.find { op }.let { query ->
                if (sort.isSorted) {
                    query.orderBy(*sort.toExposedOrderBy(entityInformation.table))
                }
                query.toList()
            }
        }
    }

    private fun executeDelete(op: Op<Boolean>): Long {
        val entities = entityClass.find { op }.toList()
        entities.forEach { it.delete() }
        return entities.size.toLong()
    }

    private fun executeLimiting(op: Op<Boolean>, maxResults: Int?, sort: Sort): Any? {
        val query = entityClass.find { op }
        if (sort.isSorted) {
            query.orderBy(*sort.toExposedOrderBy(entityInformation.table))
        }
        val limited = if (maxResults != null) query.limit(maxResults) else query
        return if (isSingleResult()) limited.firstOrNull() else limited.toList()
    }

    private fun executePageQuery(op: Op<Boolean>, pageable: Pageable, sort: Sort): Page<E> {
        val total = entityClass.find { op }.count()
        val query = entityClass.find { op }
        if (sort.isSorted) {
            query.orderBy(*sort.toExposedOrderBy(entityInformation.table))
        }
        val content = query.limit(pageable.pageSize).offset(pageable.offset).toList()
        return PageImpl(content, pageable, total)
    }

    private fun executeSliceQuery(op: Op<Boolean>, pageable: Pageable, sort: Sort): Any {
        val query = entityClass.find { op }
        if (sort.isSorted) {
            query.orderBy(*sort.toExposedOrderBy(entityInformation.table))
        }
        val fetchSize = pageable.pageSize + 1
        val content = query.limit(fetchSize).offset(pageable.offset).toList()
        val hasNext = content.size > pageable.pageSize
        return SliceImpl(if (hasNext) content.dropLast(1) else content, pageable, hasNext)
    }

    private fun isPageQuery(): Boolean =
        Page::class.java.isAssignableFrom(queryMethod.returnedObjectType)

    private fun isSliceQuery(): Boolean =
        org.springframework.data.domain.Slice::class.java.isAssignableFrom(queryMethod.returnedObjectType)

    private fun isSingleResult(): Boolean =
        !queryMethod.isCollectionQuery &&
            !queryMethod.isStreamQuery &&
            !queryMethod.isPageQuery &&
            !queryMethod.isSliceQuery

    private inline fun <reified T : Any> Array<out Any>.firstInstanceOrNull(): T? =
        firstOrNull { it is T } as? T
}
