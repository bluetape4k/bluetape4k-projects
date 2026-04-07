package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requirePositiveNumber
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
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.batchUpsert
import org.jetbrains.exposed.v1.r2dbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Exposed R2DBC를 사용하는 Repository의 기본 인터페이스입니다.
 *
 * `ID` 타입의 기본키를 가지는 [IdTable] 테이블에서 [E] 엔티티를 조회/저장/삭제하는
 * 공통 CRUD 연산을 기본 구현과 함께 제공합니다. 모든 단건 조회/변경 연산은
 * `suspend` 함수로, 다건 조회 연산은 [kotlinx.coroutines.flow.Flow]로 제공됩니다.
 *
 * @param ID 기본키 타입 (예: [Long], [Int], [java.util.UUID])
 * @param E 조회 결과로 매핑될 엔티티(레코드) 타입
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 1. 테이블 정의
 * object ActorTable : LongIdTable("actors") {
 *     val firstName = varchar("first_name", 50)
 *     val lastName  = varchar("last_name",  50)
 * }
 *
 * // 2. 레코드(DTO) 타입 정의
 * data class ActorRecord(
 *     val id: Long = 0L,
 *     val firstName: String,
 *     val lastName: String,
 * )
 *
 * // 3. Repository 구현
 * class ActorRepository : LongR2dbcRepository<ActorRecord> {
 *     override val table = ActorTable
 *
 *     override suspend fun ResultRow.toEntity() = ActorRecord(
 *         id        = this[ActorTable.id].value,
 *         firstName = this[ActorTable.firstName],
 *         lastName  = this[ActorTable.lastName],
 *     )
 *
 *     suspend fun save(record: ActorRecord): ActorRecord {
 *         val id = ActorTable.insertAndGetId {
 *             it[firstName] = record.firstName
 *             it[lastName]  = record.lastName
 *         }
 *         return record.copy(id = id.value)
 *     }
 * }
 *
 * // 4. 사용
 * suspendTransaction {
 *     val repo = ActorRepository()
 *     val saved = repo.save(ActorRecord(firstName = "Johnny", lastName = "Depp"))
 *
 *     val found = repo.findById(saved.id)
 *     val all   = repo.findAll(limit = 10) { ActorTable.lastName eq "Depp" }.toList()
 *     val page  = repo.findPage(pageNumber = 0, pageSize = 20)
 * }
 * ```
 */
interface R2dbcRepository<ID: Any, E: Any> {
    /**
     * 엔티티가 매핑되는 Exposed의 IdTable을 반환합니다.
     */
    val table: IdTable<ID>

    /**
     * 엔티티의 Identifier 를 제공합니다.
     */
    fun extractId(entity: E): ID

    /**
     * ResultRow를 엔티티로 변환합니다.
     */
    suspend fun ResultRow.toEntity(): E

    /**
     * 전체 엔티티의 개수를 반환합니다.
     */
    suspend fun count(): Long = table.selectAll().count()

    /**
     * 조건에 맞는 엔티티의 개수를 반환합니다.
     * @param predicate 조건을 반환하는 함수
     */
    suspend fun countBy(predicate: () -> Op<Boolean> = { Op.TRUE }): Long = table.selectAll().where(predicate).count()

    /**
     * Op 조건에 맞는 엔티티의 개수를 반환합니다.
     * @param op 조건
     */
    suspend fun countBy(op: Op<Boolean>): Long = table.selectAll().where(op).count()

    /**
     * 테이블이 비어있는지 여부를 반환합니다.
     */
    suspend fun isEmpty(): Boolean = table.selectAll().empty()

    /**
     * 테이블이 비어있지 않은지 여부를 반환합니다.
     */
    suspend fun isNotEmpty(): Boolean = !isEmpty()

    /**
     * 쿼리 결과가 존재하는지 확인합니다.
     * @param query AbstractQuery
     */
    suspend fun exists(query: AbstractQuery<*>): Boolean {
        val exists =
            org.jetbrains.exposed.v1.core
                .exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    /**
     * ID로 엔티티가 존재하는지 확인합니다.
     * @param id 엔티티의 ID
     */
    suspend fun existsById(id: ID): Boolean = !table.selectAll().where { table.id eq id }.empty()

    /**
     * 조건에 맞는 엔티티가 존재하는지 확인합니다.
     * @param predicate 조건
     */
    suspend fun existsBy(predicate: () -> Op<Boolean>): Boolean = !table.selectAll().where(predicate).empty()

    /**
     * ID로 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     * @param id 엔티티의 ID
     */
    suspend fun findById(id: ID): E =
        table
            .selectAll()
            .where { table.id eq id }
            .single()
            .toEntity()

    /**
     * ID로 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param id 엔티티의 ID
     */
    suspend fun findByIdOrNull(id: ID): E? =
        table
            .selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

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
    ): Flow<E> =
        table
            .selectAll()
            .where(predicate)
            .apply {
                limit?.run { limit(limit) }
                offset?.run { offset(offset) }
            }.orderBy(table.id, sortOrder)
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
    ): Flow<E> {
        val condition: Op<Boolean> =
            filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
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
    ): Flow<E> =
        findWithFilters(
            *filters,
            limit = limit,
            offset = offset,
            sortOrder = sortOrder
        )

    /**
     * 조건에 맞는 첫 번째 엔티티를 조회합니다.
     * @param offset 조회 시작 위치
     * @param predicate 조건
     */
    suspend fun findFirstOrNull(
        offset: Long? = null,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): E? =
        table
            .selectAll()
            .where(predicate)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }.firstOrNull()
            ?.toEntity()

    /**
     * 조건에 맞는 마지막 엔티티를 조회합니다.
     * @param offset 조회 시작 위치
     * @param predicate 조건
     */
    suspend fun findLastOrNull(
        offset: Long? = null,
        predicate: () -> Op<Boolean> = { Op.TRUE },
    ): E? =
        table
            .selectAll()
            .where(predicate)
            .orderBy(table.id, SortOrder.DESC)
            .limit(1)
            .apply {
                offset?.run { offset(offset) }
            }.firstOrNull()
            ?.toEntity()

    /**
     * 특정 컬럼 값으로 엔티티를 조회합니다.
     * @param field 컬럼
     * @param value 값
     */
    fun <V> findByField(
        field: Column<V>,
        value: V,
    ): Flow<E> =
        table
            .selectAll()
            .where { field eq value }
            .map { it.toEntity() }

    /**
     * 특정 컬럼 값으로 첫 번째 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param field 컬럼
     * @param value 값
     */
    suspend fun <V> findByFieldOrNull(
        field: Column<V>,
        value: V,
    ): E? =
        table
            .selectAll()
            .where { field eq value }
            .firstOrNull()
            ?.toEntity()

    /**
     * 여러 ID로 엔티티를 일괄 조회합니다.
     *
     * **주의**: `ids` 개수가 많을 경우 DB별 `IN` 절 크기 제한을 초과할 수 있습니다.
     * 대용량 ID 목록은 청크 단위로 나눠 호출하세요.
     *
     * @param ids 조회할 ID 컬렉션
     */
    fun findAllByIds(ids: Iterable<ID>): Flow<E> =
        table
            .selectAll()
            .where { table.id inList ids }
            .map { it.toEntity() }

    /**
     * 엔티티를 삭제합니다.
     */
    suspend fun delete(entity: E): Int = deleteById(extractId(entity))

    /**
     * ID로 엔티티를 삭제합니다.
     * @param id 삭제할 엔티티의 ID
     */
    suspend fun deleteById(id: ID): Int = table.deleteWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 삭제합니다.
     * @param limit 삭제할 최대 개수
     * @param op 조건
     */
    suspend fun deleteAll(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int = table.deleteWhere(limit = limit, op = op)

    /**
     * ID로 엔티티를 무시하고 삭제합니다.
     * @param id 삭제할 엔티티의 ID
     */
    suspend fun deleteByIdIgnore(id: ID): Int = table.deleteIgnoreWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 무시하고 삭제합니다.
     * @param limit 삭제할 최대 개수
     * @param op 조건
     */
    suspend fun deleteAllIgnore(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int = table.deleteIgnoreWhere(limit, op = op)

    /**
     * 여러 ID로 엔티티를 일괄 삭제합니다.
     *
     * **주의**: `ids` 개수가 많을 경우 DB별 `IN` 절 크기 제한을 초과할 수 있습니다.
     * 대용량 ID 목록은 청크 단위로 나눠 호출하세요.
     *
     * @param ids 삭제할 ID 컬렉션
     */
    suspend fun deleteAllByIds(ids: Iterable<ID>): Int = table.deleteWhere { table.id inList ids }

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
    ): Int = table.update(where = predicate, limit = limit, body = updateStatement)

    /**
     * 여러 엔티티를 일괄로 삽입합니다.
     * @param entities 삽입할 엔티티 목록
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     */
    suspend fun <D> batchInsert(
        entities: Iterable<D>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(D) -> Unit,
    ): List<E> =
        table
            .batchInsert(
                data = entities,
                ignore = ignore,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = insertStatement
            ).map { it.toEntity() }

    /**
     * 여러 엔티티를 일괄로 삽입합니다.
     * @param entities 삽입할 엔티티 시퀀스
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     */
    suspend fun <D> batchInsert(
        entities: Sequence<D>,
        ignore: Boolean = false,
        shouldReturnGeneratedValues: Boolean = true,
        insertStatement: BatchInsertStatement.(D) -> Unit,
    ): List<E> =
        table
            .batchInsert(
                data = entities,
                ignore = ignore,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = insertStatement
            ).map { it.toEntity() }

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
    suspend fun <D: Any> batchUpsert(
        entities: Iterable<D>,
        vararg keys: Column<*>,
        onUpdate: (UpsertBuilder.(UpdateStatement) -> Unit)? = null,
        onUpdateExclude: List<Column<*>>? = null,
        where: (() -> Op<Boolean>)? = null,
        shouldReturnGeneratedValues: Boolean = true,
        body: BatchUpsertStatement.(D) -> Unit,
    ): List<E> =
        table
            .batchUpsert(
                data = entities,
                keys = keys,
                onUpdate = onUpdate,
                onUpdateExclude = onUpdateExclude,
                where = where,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = body
            ).map { it.toEntity() }

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
    suspend fun <D: Any> batchUpsert(
        entities: Sequence<D>,
        vararg keys: Column<*>,
        onUpdate: (UpsertBuilder.(UpdateStatement) -> Unit)? = null,
        onUpdateExclude: List<Column<*>>? = null,
        where: (() -> Op<Boolean>)? = null,
        shouldReturnGeneratedValues: Boolean = true,
        body: BatchUpsertStatement.(D) -> Unit,
    ): List<E> =
        table
            .batchUpsert(
                data = entities,
                keys = keys,
                onUpdate = onUpdate,
                onUpdateExclude = onUpdateExclude,
                where = where,
                shouldReturnGeneratedValues = shouldReturnGeneratedValues,
                body = body
            ).map { it.toEntity() }

    /**
     * 페이징하여 엔티티를 조회합니다.
     *
     * **주의**: `totalCount`와 `content`는 별도 쿼리로 조회되므로 원자적으로 일관성이 보장되지 않습니다.
     * 두 쿼리 사이에 다른 트랜잭션이 행을 삽입/삭제하면 값이 불일치할 수 있습니다.
     * 엄격한 일관성이 필요한 경우 더 높은 격리 수준을 사용하세요.
     *
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
    ): ExposedPage<E> {
        pageNumber.requireGe(0, "pageNumber")
        pageSize.requirePositiveNumber("pageSize")
        val totalCount = countBy(predicate)
        val content =
            findAll(
                limit = pageSize,
                offset = (pageNumber.toLong() * pageSize),
                sortOrder = sortOrder,
                predicate = predicate
            ).toList()
        return ExposedPage(
            content = content,
            totalCount = totalCount,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }
}

/**
 * [Int] 기본키를 사용하는 [R2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param E 엔티티 타입
 */
interface IntR2dbcRepository<E: Any>: R2dbcRepository<Int, E>

/**
 * [Long] 기본키를 사용하는 [R2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param E 엔티티 타입
 */
interface LongR2dbcRepository<E: Any>: R2dbcRepository<Long, E>

/**
 * Kotlin [kotlin.uuid.Uuid] 기본키를 사용하는 [R2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param E 엔티티 타입
 */
@OptIn(ExperimentalUuidApi::class)
interface UuidR2dbcRepository<E: Any>: R2dbcRepository<Uuid, E>

/**
 * [java.util.UUID] 기본키를 사용하는 [R2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param E 엔티티 타입
 */
interface UUIDR2dbcRepository<E: Any>: R2dbcRepository<UUID, E>

/**
 * [String] 기본키를 사용하는 [R2dbcRepository]의 편의 타입 별칭입니다.
 *
 * @param E 엔티티 타입
 */
interface StringR2dbcRepository<E: Any>: R2dbcRepository<String, E>
