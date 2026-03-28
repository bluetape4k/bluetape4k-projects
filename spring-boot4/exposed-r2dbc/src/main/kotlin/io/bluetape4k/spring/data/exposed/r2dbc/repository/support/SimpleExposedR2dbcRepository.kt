package io.bluetape4k.spring.data.exposed.r2dbc.repository.support

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.data.exposed.jdbc.repository.support.toExposedOrderBy
import io.bluetape4k.spring.data.exposed.r2dbc.repository.ExposedR2dbcRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Exposed suspend CRUD/paging/streaming Repository의 기본 구현체입니다.
 *
 * Reflection 없이 Repository가 제공한 매핑 함수([toDomain], [toPersistValues])를 사용해
 * [IdTable] DSL 쿼리를 실행합니다.
 */
@Repository
@Suppress("UNCHECKED_CAST")
class SimpleExposedR2dbcRepository<R : Any, ID : Any>(
    override val table: IdTable<ID>,
    private val toDomainMapper: (ResultRow) -> R,
    private val persistValuesProvider: (R) -> Map<Column<*>, Any?>,
    private val idExtractor: (R) -> ID?,
) : ExposedR2dbcRepository<R, ID> {

    companion object : KLoggingChannel()

    override fun extractId(entity: R): ID? = idExtractor(entity)

    override fun toDomain(row: ResultRow): R = toDomainMapper(row)

    override fun toPersistValues(domain: R): Map<Column<*>, Any?> = persistValuesProvider(domain)

    override suspend fun <S : R> save(entity: S): S {
        val persisted = inTransaction { persist(entity) }
        return (persisted as? S) ?: entity
    }

    override fun <S : R> saveAll(entities: Iterable<S>): Flow<S> = flow {
        val results = mutableListOf<S>()
        inTransaction {
            for (entity in entities) {
                results.add(persist(entity) as S)
            }
        }
        emitAll(results.asFlow())
    }

    override fun <S : R> saveAll(entityStream: Flow<S>): Flow<S> = flow {
        val results = mutableListOf<S>()
        inTransaction {
            entityStream.collect { entity ->
                results.add(persist(entity) as S)
            }
        }
        emitAll(results.asFlow())
    }

    override suspend fun findById(id: ID): R? = findByIdOrNull(id)

    override suspend fun findByIdOrNull(id: ID): R? =
        inTransaction { findRowById(id)?.let(::toDomain) }

    override suspend fun existsById(id: ID): Boolean = inTransaction { findRowById(id) != null }

    override suspend fun findAllAsList(): List<R> = inTransaction {
        val rows = mutableListOf<ResultRow>()
        table.selectAll().collect { rows.add(it) }
        rows.map(::toDomain)
    }

    /**
     * 모든 row를 channelFlow + suspendTransaction으로 진짜 lazy streaming합니다.
     * eager materialization 없이 백프레셔와 함께 처리됩니다.
     */
    override fun findAll(): Flow<R> = streamAll()

    override fun streamAll(database: R2dbcDatabase?): Flow<R> = channelFlow {
        suspendTransaction(database) {
            table.selectAll().collect { row ->
                send(toDomain(row))
            }
        }
    }

    override fun findAllById(ids: Iterable<ID>): Flow<R> = flow {
        val idList = ids.toList()
        if (idList.isEmpty()) return@flow

        val rows = mutableListOf<ResultRow>()
        inTransaction {
            table.selectAll()
                .where { table.id inList idList }
                .collect { rows.add(it) }
        }
        emitAll(rows.map(::toDomain).asFlow())
    }

    override fun findAllById(ids: Flow<ID>): Flow<R> = flow {
        emitAll(findAllById(ids.toList() as Iterable<ID>))
    }

    override fun findAll(op: () -> Op<Boolean>): Flow<R> = channelFlow {
        suspendTransaction {
            table.selectAll().where { op() }.collect { row ->
                send(toDomain(row))
            }
        }
    }

    override suspend fun count(): Long = inTransaction { table.selectAll().count() }

    override suspend fun deleteById(id: ID): Unit = inTransaction {
        table.deleteWhere { table.id eq id }
    }

    override suspend fun delete(entity: R) {
        extractId(entity)?.let { deleteById(it) }
    }

    override suspend fun deleteAllById(ids: Iterable<ID>) {
        val idList = ids.toList()
        if (idList.isNotEmpty()) {
            inTransaction {
                table.deleteWhere { table.id inList idList }
            }
        }
    }

    override suspend fun deleteAll(entities: Iterable<R>) {
        deleteAllById(entities.mapNotNull { extractId(it) })
    }

    override suspend fun <S : R> deleteAll(entityStream: Flow<S>) {
        inTransaction {
            val ids = mutableListOf<ID>()
            entityStream.collect { entity ->
                extractId(entity)?.let { ids.add(it) }
            }
            if (ids.isNotEmpty()) {
                table.deleteWhere { table.id inList ids }
            }
        }
    }

    override suspend fun deleteAll(): Unit = inTransaction {
        table.deleteAll()
    }

    override suspend fun findAll(pageable: Pageable): Page<R> = inTransaction {
        // COUNT 쿼리는 ORDER BY 없이 별도로 실행 (S-2)
        val total = table.selectAll().count()

        val query = table.selectAll()
        if (pageable.sort.isSorted) {
            query.orderBy(*pageable.sort.toExposedOrderBy(table))
        }

        if (pageable.isUnpaged) {
            val rows = mutableListOf<ResultRow>()
            query.collect { rows.add(it) }
            PageImpl(rows.map(::toDomain), pageable, total)
        } else {
            val rows = mutableListOf<ResultRow>()
            query
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .collect { rows.add(it) }
            PageImpl(rows.map(::toDomain), pageable, total)
        }
    }

    override suspend fun count(op: () -> Op<Boolean>): Long = inTransaction {
        table.selectAll()
            .where { op() }
            .count()
    }

    override suspend fun exists(op: () -> Op<Boolean>): Boolean = inTransaction {
        !table.selectAll()
            .where { op() }
            .empty()
    }

    private suspend fun findRowById(id: ID): ResultRow? {
        var result: ResultRow? = null
        table.selectAll()
            .where { table.id eq id }
            .limit(1)
            .collect { result = it }
        return result
    }

    /**
     * 엔티티를 저장합니다.
     * - [extractId]가 null → INSERT (auto-generated ID)
     * - [extractId]가 non-null → UPDATE 시도. 0 rows affected 이면 INSERT
     *
     * UPDATE 성공 시 추가 SELECT 없이 입력 entity를 그대로 반환합니다. (P-2)
     * INSERT 후에는 DB에서 할당된 ID를 반영하기 위해 re-fetch합니다.
     */
    private suspend fun persist(entity: R): R {
        val idValue = extractId(entity)
        if (idValue != null) {
            val updatedRows = table.update({ table.id eq idValue }) { stmt ->
                writePersistValues(stmt, entity)
            }
            if (updatedRows > 0) {
                return entity  // UPDATE 성공: 추가 SELECT 불필요
            }
        }
        val insertedId = table.insertAndGetId { stmt ->
            writePersistValues(stmt, entity)
        }.value
        return findRowById(insertedId)?.let(::toDomain) ?: entity
    }

    private suspend inline fun <T> inTransaction(crossinline block: suspend R2dbcTransaction.() -> T): T =
        suspendTransaction { block() }

    private fun writePersistValues(statement: UpdateBuilder<*>, entity: R) {
        toPersistValues(entity).forEach { (column, value) ->
            require(column.table == table && column != table.id) {
                "Persist column '${column.name}' must belong to table '${table.tableName}' and must not be id column"
            }
            statement[column as Column<Any?>] = value
        }
    }
}
