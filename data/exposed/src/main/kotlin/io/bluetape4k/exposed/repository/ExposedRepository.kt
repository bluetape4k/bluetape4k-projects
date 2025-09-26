package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.core.HasIdentifier
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Exposed 를 사용하는 Repository 의 기본 인터페이스입니다.
 *
 * ```
 * class MyRepository: ExposedRepository<MyEntity, Long> {
 *    override val table = MyTable
 *    ...
 * }
 * ```
 */
interface ExposedRepository<T: HasIdentifier<ID>, ID: Any> {

    /**
     * Exposed의 IdTable을 반환합니다.
     * @return 엔티티에 해당하는 IdTable
     */
    val table: IdTable<ID>

    /**
     * 현재 트랜잭션을 반환합니다.
     * @return 현재 [Transaction]
     */
    fun currentTransaction(): Transaction =
        TransactionManager.current()

    /**
     * 현재 트랜잭션을 반환하거나, 없으면 null을 반환합니다.
     * @return 현재 [Transaction] 또는 null
     */
    fun currentTransactionOrNull(): Transaction? =
        TransactionManager.currentOrNull()

    /**
     * ResultRow를 엔티티로 변환합니다.
     * @receiver [ResultRow]
     * @return 엔티티 [T]
     */
    fun ResultRow.toEntity(): T

    /**
     * 전체 엔티티 개수를 반환합니다.
     * @return 엔티티 개수
     */
    fun count(): Long = table.selectAll().count()

    /**
     * @deprecated countBy()를 사용하세요.
     * 조건에 맞는 엔티티 개수를 반환합니다.
     */
    @Deprecated("Use countBy() instead", replaceWith = ReplaceWith("countBy(predicate)"))
    fun count(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    /**
     * @deprecated countBy()를 사용하세요.
     * 조건에 맞는 엔티티 개수를 반환합니다.
     */
    @Deprecated("Use countBy() instead", replaceWith = ReplaceWith("countBy(op)"))
    fun count(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    /**
     * 조건에 맞는 엔티티 개수를 반환합니다.
     * @param predicate 조건
     * @return 개수
     */
    fun countBy(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    /**
     * 조건에 맞는 엔티티 개수를 반환합니다.
     * @param op 조건
     * @return 개수
     */
    fun countBy(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    /**
     * 테이블이 비어있는지 확인합니다.
     * @return 비어있으면 true
     */
    fun isEmpty(): Boolean =
        table.selectAll().empty()

    /**
     * 쿼리 결과가 존재하는지 확인합니다.
     * @param query [AbstractQuery]
     * @return 존재하면 true
     */
    fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.v1.core.exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    /**
     * ID로 엔티티 존재 여부를 확인합니다.
     * @param id 엔티티 ID
     * @return 존재하면 true
     */
    fun existsById(id: ID): Boolean =
        !table.selectAll().where { table.id eq id }.empty()

    /**
     * ID로 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     * @param id 엔티티 ID
     * @return 엔티티 [T]
     */
    fun findById(id: ID): T =
        table.selectAll().where { table.id eq id }.single().toEntity()

    /**
     * ID로 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param id 엔티티 ID
     * @return 엔티티 [T] 또는 null
     */
    fun findByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    /**
     * 조건에 맞는 모든 엔티티를 조회합니다.
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @param predicate 조건
     * @return 엔티티 목록
     */
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): List<T> =
        table.selectAll()
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }
            .orderBy(table.id, sortOrder)
            .map { it.toEntity() }

    /**
     * 여러 조건을 and로 결합하여 엔티티를 조회합니다.
     * @param filters 조건 람다 가변 인자
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @return 엔티티 목록
     */
    fun findWithFilters(
        vararg filters: () -> Op<Boolean>,
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
    ): List<T> {
        val condition: Op<Boolean> = filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
            acc.and(filter.invoke())
        }
        return findAll(limit, offset, sortOrder) { condition }
    }

    /**
     * 조건에 맞는 첫 번째 엔티티를 조회합니다.
     * @param offset 시작 위치
     * @param predicate 조건
     * @return 엔티티 [T] 또는 null
     */
    fun findFirstOrNull(
        offset: Long? = null,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()?.toEntity()

    /**
     * 조건에 맞는 마지막 엔티티를 조회합니다.
     * @param offset 시작 위치
     * @param predicate 조건
     * @return 엔티티 [T] 또는 null
     */
    fun findLastOrNull(
        offset: Long? = null,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .orderBy(table.id, SortOrder.DESC)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .lastOrNull()?.toEntity()

    /**
     * 특정 컬럼 값으로 엔티티를 조회합니다.
     * @param field 컬럼
     * @param value 값
     * @return 엔티티 목록
     */
    fun <V> findByField(field: Column<V>, value: V): List<T> = table.selectAll()
        .where { field eq value }
        .map { it.toEntity() }

    /**
     * 엔티티를 삭제합니다.
     * @param entity 삭제할 엔티티
     * @return 삭제된 행 수
     */
    fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    /**
     * ID로 엔티티를 삭제합니다.
     * @param id 엔티티 ID
     * @return 삭제된 행 수
     */
    fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 삭제합니다.
     * @param limit 최대 삭제 개수
     * @param op 조건
     * @return 삭제된 행 수
     */
    fun deleteAll(limit: Int? = null, op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE }): Int =
        table.deleteWhere(limit = limit, op = op)

    /**
     * 엔티티를 무시하고 삭제합니다.
     * @param entity 삭제할 엔티티
     * @return 삭제된 행 수
     */
    fun deleteIgnore(entity: T): Int = table.deleteIgnoreWhere { table.id eq entity.id }

    /**
     * ID로 엔티티를 무시하고 삭제합니다.
     * @param id 엔티티 ID
     * @return 삭제된 행 수
     */
    fun deleteByIdIgnore(id: ID): Int = table.deleteIgnoreWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 무시하고 삭제합니다.
     * @param limit 최대 삭제 개수
     * @param op 조건
     * @return 삭제된 행 수
     */
    fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int = table.deleteIgnoreWhere(limit, op = op)

    /**
     * ID로 엔티티를 수정합니다.
     * @param id 엔티티 ID
     * @param limit 최대 수정 개수
     * @param updateStatement 수정 내용
     * @return 수정된 행 수
     */
    fun updateById(id: ID, limit: Int? = null, updateStatement: IdTable<ID>.(UpdateStatement) -> Unit): Int =
        table.update(where = { table.id eq id }, limit = limit, body = updateStatement)

    /**
     * 여러 엔티티를 일괄 삽입합니다.
     * @param entities 삽입할 엔티티 목록
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     * @return 삽입된 엔티티 목록
     */
    fun <E> batchInsert(
        entities: Iterable<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
        .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄 삽입합니다.
     * @param entities 삽입할 엔티티 시퀀스
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     * @return 삽입된 엔티티 목록
     */
    fun <E> batchInsert(
        entities: Sequence<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, insertStatement)
        .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄 수정합니다.
     * @param entities 수정할 엔티티 목록
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param updateStatement 수정 내용
     * @return 수정된 엔티티 목록
     */
    fun batchUpdate(
        entities: Iterable<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
        .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄 수정합니다.
     * @param entities 수정할 엔티티 시퀀스
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param updateStatement 수정 내용
     * @return 수정된 엔티티 목록
     */
    fun batchUpdate(
        entities: Sequence<T>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        updateStatement: BatchInsertStatement.(T) -> Unit,
    ): List<T> = table
        .batchInsert(entities, ignore, shouldReturnGeneratedValues, updateStatement)
        .map { it.toEntity() }
}
