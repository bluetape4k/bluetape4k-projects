package io.bluetape4k.spring.data.exposed.jdbc.repository.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.ExposedEntityInformation
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.resolveColumnType
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.data.repository.query.RepositoryQuery
import java.sql.ResultSet

/**
 * [@Query][io.bluetape4k.spring.data.exposed.annotation.Query] 어노테이션으로 지정한 raw SQL을 실행합니다.
 * 위치 기반 파라미터(?1, ?2, ...)를 Prepared Statement 바인딩으로 안전하게 처리합니다.
 *
 * 결과 매핑: SELECT id 컬럼에서 ID를 읽어 EntityClass.findById로 로드합니다.
 */
class DeclaredExposedQuery<E : Entity<ID>, ID : Any>(
    private val queryMethod: ExposedQueryMethod,
    private val entityInformation: ExposedEntityInformation<E, ID>,
) : RepositoryQuery {

    companion object: KLogging()

    private data class BoundSql(
        val sql: String,
        val args: List<Pair<ColumnType<*>, Any?>>,
    )

    private val positionalPlaceholderRegex = Regex("\\?(\\d+)")
    private val entityClass: EntityClass<ID, E> = entityInformation.entityClass
    private val rawSql: String =
        queryMethod.getAnnotatedQuery()
            ?: error("@Query annotation is required for DeclaredExposedQuery")

    override fun getQueryMethod(): ExposedQueryMethod = queryMethod

    @Suppress("UNCHECKED_CAST")
    override fun execute(parameters: Array<out Any>): Any {
        val boundSql = bindParameters(rawSql, parameters)
        val tx = TransactionManager.current()
        tx.flushCache()

        return tx.exec(boundSql.sql, boundSql.args) { rs ->
            val results = mutableListOf<E>()
            while (rs.next()) {
                // id 컬럼에서 값을 읽어 EntityClass로 로드
                val idVal = readIdValue(rs) ?: continue
                val normalizedId = coerceIdValue(idVal)
                entityClass.findById(normalizedId)?.let { results.add(it) }
            }
            results
        } ?: emptyList<E>()
    }

    private fun bindParameters(
        sql: String,
        parameters: Array<out Any?>,
    ): BoundSql {
        val args = mutableListOf<Pair<ColumnType<*>, Any?>>()
        val normalizedSql =
            positionalPlaceholderRegex.replace(sql) { match ->
                val placeholderIndex = match.groupValues[1].toInt() - 1
                require(placeholderIndex in parameters.indices) {
                    "Query placeholder index out of bounds: ${match.value} for parameter size ${parameters.size}"
                }
                args += toSqlArg(parameters[placeholderIndex])
                "?"
            }
        return BoundSql(normalizedSql, args)
    }

    @OptIn(InternalApi::class)
    private fun toSqlArg(value: Any?): Pair<ColumnType<*>, Any?> {
        if (value == null) return TextColumnType() to null

        val columnType =
            runCatching {
                @Suppress("UNCHECKED_CAST")
                resolveColumnType(value::class as kotlin.reflect.KClass<Any>, defaultType = TextColumnType())
            }.getOrElse { TextColumnType() }

        val normalizedValue = if (columnType is TextColumnType && value !is String) value.toString() else value
        return columnType to normalizedValue
    }

    private fun readIdValue(rs: ResultSet): Any? {
        val idColumnName = entityInformation.table.id.name
        val byName = runCatching { rs.getObject(idColumnName) }.getOrNull()
        return byName ?: rs.getObject(1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun coerceIdValue(rawId: Any): ID {
        val idType = entityInformation.idType
        if (idType.isInstance(rawId)) {
            return rawId as ID
        }
        return when (idType) {
            Long::class.java -> if (rawId is Number) rawId.toLong() as ID else rawId as ID
            Int::class.java -> if (rawId is Number) rawId.toInt() as ID else rawId as ID
            Short::class.java -> if (rawId is Number) rawId.toShort() as ID else rawId as ID
            String::class.java -> rawId.toString() as ID
            else -> rawId as ID
        }
    }
}
