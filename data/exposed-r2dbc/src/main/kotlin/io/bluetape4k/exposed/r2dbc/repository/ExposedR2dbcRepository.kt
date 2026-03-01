package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.exposed.core.HasIdentifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.UpsertBuilder
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.batchUpsert
import org.jetbrains.exposed.v1.r2dbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.update

/**
 * Exposed R2dbc를 사용하는 Repository 의 기본 인터페이스입니다.
 *
 * ```
 * class ActorR2dbcRepository: ExposedR2dbcRepository<ActorRecord, Long> {
 *    override val table = ActorTable
 *    ...
 * }
 * ```
 */
interface ExposedR2dbcRepository<T: HasIdentifier<ID>, ID: Any> {
    /**
     * 엔티티가 매핑되는 Exposed의 IdTable을 반환합니다.
     */
    val table: IdTable<ID>

    /**
     * 현재 R2DBC 트랜잭션을 반환합니다.
     */
    fun currentTransaction(): R2dbcTransaction =
        TransactionManager.current()

    /**
     * 현재 R2DBC 트랜잭션이 있으면 반환하고, 없으면 null을 반환합니다.
     */
    fun currentTransactionOrNull(): R2dbcTransaction? =
        TransactionManager.currentOrNull()

    /**
     * ResultRow를 엔티티로 변환합니다.
     */
    suspend fun ResultRow.toEntity(): T

    /**
     * 전체 엔티티의 개수를 반환합니다.
     */
    suspend fun count(): Long = table.selectAll().count()

    /**
     * 조건에 맞는 엔티티의 개수를 반환합니다.
     * @param predicate 조건을 반환하는 함수
     */
    suspend fun countBy(predicate: () -> Op<Boolean> = { Op.TRUE }): Long =
        table.selectAll().where(predicate).count()

    /**
     * Op 조건에 맞는 엔티티의 개수를 반환합니다.
     * @param op 조건
     */
    suspend fun countBy(op: Op<Boolean>): Long =
        table.selectAll().where(op).count()

    /**
     * 테이블이 비어있는지 여부를 반환합니다.
     */
    suspend fun isEmpty(): Boolean =
        table.selectAll().empty()

    /**
     * 테이블이 비어있지 않은지 여부를 반환합니다.
     */
    suspend fun isNotEmpty(): Boolean = !isEmpty()

    /**
     * 쿼리 결과가 존재하는지 확인합니다.
     * @param query AbstractQuery
     */
    suspend fun exists(query: AbstractQuery<*>): Boolean {
        val exists = org.jetbrains.exposed.v1.core.exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    /**
     * ID로 엔티티가 존재하는지 확인합니다.
     * @param id 엔티티의 ID
     */
    suspend fun existsById(id: ID): Boolean =
        !table.selectAll().where { table.id eq id }.empty()

    /**
     * 조건에 맞는 엔티티가 존재하는지 확인합니다.
     * @param predicate 조건
     */
    suspend fun existsBy(predicate: () -> Op<Boolean>): Boolean =
        !table.selectAll().where(predicate).empty()

    /**
     * ID로 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     * @param id 엔티티의 ID
     */
    suspend fun findById(id: ID): T =
        table.selectAll().where { table.id eq id }.single().toEntity()

    /**
     * ID로 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param id 엔티티의 ID
     */
    suspend fun findByIdOrNull(id: ID): T? =
        table.selectAll().where { table.id eq id }.singleOrNull()?.toEntity()

    /**
     * 조건에 맞는 모든 엔티티를 조회합니다.
     * @param limit 조회할 최대 개수
     * @param offset 조회 시작 위치
     * @param sortOrder 정렬 순서
     * @param predicate 조건
     */
    fun findAll(
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): Flow<T> =
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
     * @param filters 조건 함수 가변 인자
     * @param limit 조회할 최대 개수
     * @param offset 조회 시작 위치
     * @param sortOrder 정렬 순서
     */
    fun findWithFilters(
        vararg filters: () -> Op<Boolean>,
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
    ): Flow<T> {
        val condition: Op<Boolean> = filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
            acc.and(filter.invoke())
        }
        return findAll(limit, offset, sortOrder) { condition }
    }

    /**
     * 여러 조건을 and로 결합하여 엔티티를 조회합니다. [findWithFilters]의 alias입니다.
     * @param filters 조건 함수 가변 인자
     * @param limit 조회할 최대 개수
     * @param offset 조회 시작 위치
     * @param sortOrder 정렬 순서
     */
    fun findBy(
        vararg filters: () -> Op<Boolean>,
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
    ): Flow<T> = findWithFilters(
        *filters,
        limit = limit,
        offset = offset,
        sortOrder = sortOrder,
    )

    /**
     * 조건에 맞는 첫 번째 엔티티를 조회합니다.
     * @param offset 조회 시작 위치
     * @param predicate 조건
     */
    suspend fun findFirstOrNull(
        offset: Long? = null,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): T? =
        table.selectAll()
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }
            .firstOrNull()
            ?.toEntity()

    /**
     * 조건에 맞는 마지막 엔티티를 조회합니다.
     * @param offset 조회 시작 위치
     * @param predicate 조건
     */
    suspend fun findLastOrNull(
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
            .firstOrNull()
            ?.toEntity()

    /**
     * 특정 컬럼 값으로 엔티티를 조회합니다.
     * @param field 컬럼
     * @param value 값
     */
    fun <V> findByField(field: Column<V>, value: V): Flow<T> =
        table.selectAll()
            .where { field eq value }
            .map { it.toEntity() }

    /**
     * 특정 컬럼 값으로 첫 번째 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param field 컬럼
     * @param value 값
     */
    suspend fun <V> findByFieldOrNull(field: Column<V>, value: V): T? =
        table.selectAll()
            .where { field eq value }
            .firstOrNull()
            ?.toEntity()

    /**
     * 여러 ID로 엔티티를 일괄 조회합니다.
     * @param ids 조회할 ID 컬렉션
     */
    fun findAllByIds(ids: Iterable<ID>): Flow<T> =
        table.selectAll()
            .where { table.id inList ids }
            .map { it.toEntity() }

    /**
     * 엔티티를 삭제합니다.
     * @param entity 삭제할 엔티티
     */
    suspend fun delete(entity: T): Int =
        table.deleteWhere { table.id eq entity.id }

    /**
     * ID로 엔티티를 삭제합니다.
     * @param id 삭제할 엔티티의 ID
     */
    suspend fun deleteById(id: ID): Int =
        table.deleteWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 삭제합니다.
     * @param limit 삭제할 최대 개수
     * @param op 조건
     */
    suspend fun deleteAll(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int =
        table.deleteWhere(limit = limit, op = op)

    /**
     * 엔티티를 무시하고 삭제합니다.
     * @param entity 삭제할 엔티티
     */
    suspend fun deleteIgnore(entity: T): Int =
        table.deleteIgnoreWhere { table.id eq entity.id }

    /**
     * ID로 엔티티를 무시하고 삭제합니다.
     * @param id 삭제할 엔티티의 ID
     */
    suspend fun deleteByIdIgnore(id: ID): Int =
        table.deleteIgnoreWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 무시하고 삭제합니다.
     * @param limit 삭제할 최대 개수
     * @param op 조건
     */
    suspend fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int =
        table.deleteIgnoreWhere(limit, op = op)

    /**
     * 여러 ID로 엔티티를 일괄 삭제합니다.
     * @param ids 삭제할 ID 컬렉션
     */
    suspend fun deleteAllByIds(ids: Iterable<ID>): Int =
        table.deleteWhere { table.id inList ids }

    /**
     * ID로 엔티티를 수정합니다.
     * @param id 수정할 엔티티의 ID
     * @param limit 수정할 최대 개수
     * @param updateStatement 수정 내용
     */
    suspend fun updateById(
        id: ID,
        limit: Int? = null,
        updateStatement: IdTable<ID>.(UpdateStatement) -> Unit,
    ): Int =
        table.update(
            where = { table.id eq id },
            limit = limit,
            body = updateStatement
        )

    /**
     * 조건에 맞는 모든 엔티티를 수정합니다.
     * @param predicate 조건
     * @param limit 수정할 최대 개수
     * @param updateStatement 수정 내용
     */
    suspend fun updateAll(
        predicate: () -> Op<Boolean> = { Op.TRUE },
        limit: Int? = null,
        updateStatement: IdTable<ID>.(UpdateStatement) -> Unit,
    ): Int =
        table.update(where = predicate, limit = limit, body = updateStatement)

    /**
     * 여러 엔티티를 일괄로 삽입합니다.
     * @param entities 삽입할 엔티티 목록
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     */
    suspend fun <E> batchInsert(
        entities: Iterable<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchInsert(
                data = entities,
                ignore = ignore,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = insertStatement
            )
            .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄로 삽입합니다.
     * @param entities 삽입할 엔티티 시퀀스
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     */
    suspend fun <E> batchInsert(
        entities: Sequence<E>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchInsert(
                data = entities,
                ignore = ignore,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = insertStatement
            )
            .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄 Upsert 합니다.
     *
     * See [Batch Insert](https://github.com/JetBrains/Exposed/wiki/DSL#batch-insert) for more details.
     *
     * @param entities Upsert 할 엔티티 컬렉션
     * @param keys (optional) Columns to include in the condition that determines a unique constraint match. If no columns are provided,
     *             primary keys will be used. If the table does not have any primary keys, the first unique index will be attempted.
     * @param onUpdate Lambda block with an [UpdateStatement] as its argument, allowing values to be assigned to the UPDATE clause.
     *  To specify manually that the insert value should be used when updating a column, for example within an expression
     *  or function, invoke `insertValue()` with the desired column as the function argument.
     *  If left null, all columns will be updated with the values provided for the insert.
     * @param onUpdateExclude List of specific columns to exclude from updating. If left null, all columns will be updated with the values provided for the insert.
     * @param shouldReturnGeneratedValues Specifies whether newly generated values (for example, auto-incremented IDs) should be returned.
     * @return Upsert 된 엔티티 목록
     */
    suspend fun <E: Any> batchUpsert(
        entities: Iterable<E>,
        vararg keys: Column<*>,
        onUpdate: (UpsertBuilder.(UpdateStatement) -> Unit)? = null,
        onUpdateExclude: List<Column<*>>? = null,
        where: (() -> Op<Boolean>)? = null,
        shouldReturnGeneratedValues: Boolean = true,
        body: BatchUpsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchUpsert(
                data = entities,
                keys = keys,
                onUpdate = onUpdate,
                onUpdateExclude = onUpdateExclude,
                where = where,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = body,
            )
            .map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄 Upsert 합니다.
     *
     * See [Batch Insert](https://github.com/JetBrains/Exposed/wiki/DSL#batch-insert) for more details.
     *
     * @param entities Upsert 할 엔티티 시퀀스
     * @param keys (optional) Columns to include in the condition that determines a unique constraint match. If no columns are provided,
     *             primary keys will be used. If the table does not have any primary keys, the first unique index will be attempted.
     * @param onUpdate Lambda block with an [UpdateStatement] as its argument, allowing values to be assigned to the UPDATE clause.
     *  To specify manually that the insert value should be used when updating a column, for example within an expression
     *  or function, invoke `insertValue()` with the desired column as the function argument.
     *  If left null, all columns will be updated with the values provided for the insert.
     * @param onUpdateExclude List of specific columns to exclude from updating. If left null, all columns will be updated with the values provided for the insert.
     * @param shouldReturnGeneratedValues Specifies whether newly generated values (for example, auto-incremented IDs) should be returned.
     * @return Upsert 된 엔티티 목록
     */
    suspend fun <E: Any> batchUpsert(
        entities: Sequence<E>,
        vararg keys: Column<*>,
        onUpdate: (UpsertBuilder.(UpdateStatement) -> Unit)? = null,
        onUpdateExclude: List<Column<*>>? = null,
        where: (() -> Op<Boolean>)? = null,
        shouldReturnGeneratedValues: Boolean = true,
        body: BatchUpsertStatement.(E) -> Unit,
    ): List<T> =
        table
            .batchUpsert(
                data = entities,
                keys = keys,
                onUpdate = onUpdate,
                onUpdateExclude = onUpdateExclude,
                where = where,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = body,
            )
            .map { it.toEntity() }

    /**
     * 페이징하여 엔티티를 조회합니다.
     * @param pageNumber 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 크기
     * @param sortOrder 정렬 순서
     * @param predicate 조건
     * @return 페이징 결과 [ExposedPage]
     */
    suspend fun findPage(
        pageNumber: Int,
        pageSize: Int,
        sortOrder: SortOrder = SortOrder.ASC,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): ExposedPage<T> {
        val totalCount = countBy(predicate)
        val content = findAll(
            limit = pageSize,
            offset = (pageNumber.toLong() * pageSize),
            sortOrder = sortOrder,
            predicate = predicate,
        ).toList()
        return ExposedPage(
            content = content,
            totalCount = totalCount,
            pageNumber = pageNumber,
            pageSize = pageSize,
        )
    }
}
