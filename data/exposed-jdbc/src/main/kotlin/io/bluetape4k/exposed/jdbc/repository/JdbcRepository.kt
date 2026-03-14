package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.core.ExposedPage
import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.UpsertBuilder
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Exposed JDBC를 사용하는 Repository의 기본 인터페이스입니다.
 *
 * `ID` 타입의 기본키를 가지는 [T] 테이블에서 [E] 엔티티를 조회/저장/삭제하는
 * 공통 CRUD 연산을 기본 구현과 함께 제공합니다.
 * 구현체는 [table]과 [ResultRow.toEntity]만 정의하면 됩니다.
 *
 * @param ID 기본키 타입 (예: [Long], [Int], [java.util.UUID])
 * @param T Exposed [IdTable] 구현체 (예: [LongIdTable], [IntIdTable])
 * @param E 조회 결과로 매핑될 엔티티(레코드) 타입
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 1. 테이블 정의
 * object ActorTable : LongIdTable("actors") {
 *     val firstName = varchar("first_name", 50)
 *     val lastName  = varchar("last_name",  50)
 *     val birthday  = date("birthday").nullable()
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
 * class ActorRepository : LongJdbcRepository<ActorTable, ActorRecord> {
 *     override val table = ActorTable
 *
 *     override fun ResultRow.toEntity() = ActorRecord(
 *         id        = this[ActorTable.id].value,
 *         firstName = this[ActorTable.firstName],
 *         lastName  = this[ActorTable.lastName],
 *     )
 *
 *     fun save(record: ActorRecord): ActorRecord {
 *         val id = ActorTable.insertAndGetId {
 *             it[firstName] = record.firstName
 *             it[lastName]  = record.lastName
 *         }
 *         return record.copy(id = id.value)
 *     }
 * }
 *
 * // 4. 사용
 * transaction {
 *     val repo = ActorRepository()
 *     val saved = repo.save(ActorRecord(firstName = "Johnny", lastName = "Depp"))
 *
 *     val found = repo.findById(saved.id)
 *     val all   = repo.findAll(limit = 10) { ActorTable.lastName eq "Depp" }
 *     val page  = repo.findPage(pageNumber = 0, pageSize = 20)
 * }
 * ```
 */
interface JdbcRepository<ID : Any, T : IdTable<ID>, E : Any> {
    /**
     * Exposed의 IdTable을 반환합니다.
     * @return 엔티티에 해당하는 IdTable
     */
    val table: T

    /**
     * ResultRow를 엔티티로 변환합니다.
     * @receiver [ResultRow]
     * @return 엔티티 [T]
     */
    fun ResultRow.toEntity(): E

    /**
     * 전체 엔티티 개수를 반환합니다.
     * @return 엔티티 개수
     */
    fun count(): Long = table.selectAll().count()

    /**
     * 조건에 맞는 엔티티 개수를 반환합니다.
     * @param predicate 조건
     * @return 개수
     */
    fun countBy(predicate: () -> Op<Boolean> = { Op.TRUE }): Long = table.selectAll().where(predicate).count()

    /**
     * 조건에 맞는 엔티티 개수를 반환합니다.
     * @param op 조건
     * @return 개수
     */
    fun countBy(op: Op<Boolean>): Long = table.selectAll().where(op).count()

    /**
     * 테이블이 비어있는지 확인합니다.
     * @return 비어있으면 true
     */
    fun isEmpty(): Boolean = table.selectAll().empty()

    /**
     * 테이블이 비어있지 않은지 확인합니다.
     * @return 비어있지 않으면 true
     */
    fun isNotEmpty(): Boolean = !isEmpty()

    /**
     * 쿼리 결과가 존재하는지 확인합니다.
     * @param query [AbstractQuery]
     * @return 존재하면 true
     */
    fun exists(query: AbstractQuery<*>): Boolean {
        val exists =
            org.jetbrains.exposed.v1.core
                .exists(query)
        return table.select(exists).firstOrNull()?.getOrNull(exists) ?: false
    }

    /**
     * ID로 엔티티 존재 여부를 확인합니다.
     * @param id 엔티티 ID
     * @return 존재하면 true
     */
    fun existsById(id: ID): Boolean = !table.selectAll().where { table.id eq id }.empty()

    /**
     * 조건에 맞는 엔티티가 존재하는지 확인합니다.
     * @param predicate 조건
     * @return 존재하면 true
     */
    fun existsBy(predicate: () -> Op<Boolean>): Boolean = !table.selectAll().where(predicate).empty()

    /**
     * ID로 엔티티를 조회합니다. 없으면 예외를 발생시킵니다.
     *
     * @param id 엔티티 ID
     * @return 조회된 엔티티 [E]
     * @throws NoSuchElementException 해당 ID의 엔티티가 존재하지 않을 때
     * @throws IllegalArgumentException 해당 ID의 엔티티가 둘 이상일 때
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     val actor = repo.findById(1L)            // 없으면 예외
     *     val actorOrNull = repo.findByIdOrNull(1L) // 없으면 null
     * }
     * ```
     */
    fun findById(id: ID): E =
        table
            .selectAll()
            .where { table.id eq id }
            .single()
            .toEntity()

    /**
     * ID로 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param id 엔티티 ID
     * @return 엔티티 [E] 또는 null
     */
    fun findByIdOrNull(id: ID): E? =
        table
            .selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()

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
    ): List<E> =
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
    ): List<E> {
        val condition: Op<Boolean> =
            filters.fold(Op.TRUE as Op<Boolean>) { acc, filter ->
                acc.and(filter.invoke())
            }
        return findAll(limit, offset, sortOrder) { condition }
    }

    /**
     * 여러 조건을 and로 결합하여 엔티티를 조회합니다.
     * @param filters 조건 람다 가변 인자
     * @param limit 최대 개수
     * @param offset 시작 위치
     * @param sortOrder 정렬 순서
     * @return 엔티티 목록
     */
    fun findBy(
        vararg filters: () -> Op<Boolean>,
        limit: Int? = null,
        offset: Long? = null,
        sortOrder: SortOrder = SortOrder.ASC,
    ): List<E> =
        findWithFilters(
            *filters,
            limit = limit,
            offset = offset,
            sortOrder = sortOrder
        )

    /**
     * 조건에 맞는 첫 번째 엔티티를 조회합니다.
     * @param offset 시작 위치
     * @param predicate 조건
     * @return 엔티티 [E] 또는 null
     */
    fun findFirstOrNull(
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
     * @param offset 시작 위치
     * @param predicate 조건
     * @return 엔티티 [E] 또는 null
     */
    fun findLastOrNull(
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
     * @return 엔티티 목록
     */
    fun <V> findByField(
        field: Column<V>,
        value: V,
    ): List<E> =
        table
            .selectAll()
            .where { field eq value }
            .map { it.toEntity() }

    /**
     * 특정 컬럼 값으로 첫 번째 엔티티를 조회합니다. 없으면 null을 반환합니다.
     * @param field 컬럼
     * @param value 값
     * @return 엔티티 [E] 또는 null
     */
    fun <V> findByFieldOrNull(
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
     * @return 엔티티 목록
     */
    fun findAllByIds(ids: Iterable<ID>): List<E> =
        table
            .selectAll()
            .where { table.id inList ids }
            .map { it.toEntity() }

    /**
     * ID로 엔티티를 삭제합니다.
     * @param id 엔티티 ID
     * @return 삭제된 행 수
     */
    fun deleteById(id: ID): Int = table.deleteWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 삭제합니다.
     * @param limit 최대 삭제 개수
     * @param op 조건
     * @return 삭제된 행 수
     */
    fun deleteAll(
        limit: Int? = null,
        op: (IdTable<ID>).() -> Op<Boolean> = { Op.TRUE },
    ): Int = table.deleteWhere(limit = limit, op = op)

    /**
     * 해당 id를 가진 레코드를 삭제합니다. 예외는 무시합니다.
     *
     * @param id 엔티티 ID
     * @return 삭제된 행 수
     */
    fun deleteByIdIgnore(id: ID): Int = table.deleteIgnoreWhere { table.id eq id }

    /**
     * 조건에 맞는 모든 엔티티를 삭제합니다. 삭제 실패는 무시합니다.
     *
     * @param limit 최대 삭제 개수
     * @param op 조건
     * @return 삭제된 행 수
     */
    fun deleteAllIgnore(
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
     * @return 삭제된 행 수
     */
    fun deleteAllByIds(ids: Iterable<ID>): Int = table.deleteWhere { table.id inList ids }

    /**
     * ID로 엔티티를 수정합니다.
     * @param id 엔티티 ID
     * @param limit 최대 수정 개수
     * @param updateStatement 수정 내용
     * @return 수정된 행 수
     */
    fun updateById(
        id: ID,
        limit: Int? = null,
        updateStatement: IdTable<ID>.(UpdateStatement) -> Unit,
    ): Int = table.update(where = { table.id eq id }, limit = limit, body = updateStatement)

    /**
     * 조건에 맞는 모든 엔티티를 수정합니다.
     * @param predicate 조건
     * @param limit 최대 수정 개수
     * @param updateStatement 수정 내용
     * @return 수정된 행 수
     */
    fun updateAll(
        predicate: () -> Op<Boolean> = { Op.TRUE },
        limit: Int? = null,
        updateStatement: IdTable<ID>.(UpdateStatement) -> Unit,
    ): Int = table.update(where = predicate, limit = limit, body = updateStatement)

    /**
     * 여러 엔티티를 일괄 삽입합니다.
     * @param entities 삽입할 엔티티 목록
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     * @return 삽입된 엔티티 목록
     */
    fun <D> batchInsert(
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
     * 여러 엔티티를 일괄 삽입합니다.
     * @param entities 삽입할 엔티티 시퀀스
     * @param ignore 중복 무시 여부
     * @param shouldReturnGeneratedValues 생성된 값 반환 여부
     * @param insertStatement 삽입 내용
     * @return 삽입된 엔티티 목록
     */
    fun <D> batchInsert(
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
     * @param keys (선택) 유니크 제약 매칭에 사용할 컬럼 목록. 미지정 시 기본키를 사용하며, 기본키가 없을 경우 첫 번째 유니크 인덱스를 사용합니다.
     * @param onUpdate [UpdateStatement]를 인자로 받는 람다. UPDATE 절에 할당할 값을 지정합니다.
     *  컬럼에 INSERT 값을 그대로 사용하려면 `insertValue()`를 호출하세요.
     *  null 이면 INSERT 값으로 모든 컬럼을 업데이트합니다.
     * @param onUpdateExclude UPDATE에서 제외할 컬럼 목록. null 이면 INSERT 값으로 모든 컬럼을 업데이트합니다.
     * @param shouldReturnGeneratedValues 새로 생성된 값(자동 증가 ID 등) 반환 여부
     * @return Upsert 된 엔티티 목록
     */
    fun <D : Any> batchUpsert(
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
     * @param keys (선택) 유니크 제약 매칭에 사용할 컬럼 목록. 미지정 시 기본키를 사용하며, 기본키가 없을 경우 첫 번째 유니크 인덱스를 사용합니다.
     * @param onUpdate [UpdateStatement]를 인자로 받는 람다. UPDATE 절에 할당할 값을 지정합니다.
     *  컬럼에 INSERT 값을 그대로 사용하려면 `insertValue()`를 호출하세요.
     *  null 이면 INSERT 값으로 모든 컬럼을 업데이트합니다.
     * @param onUpdateExclude UPDATE에서 제외할 컬럼 목록. null 이면 INSERT 값으로 모든 컬럼을 업데이트합니다.
     * @param shouldReturnGeneratedValues 새로 생성된 값(자동 증가 ID 등) 반환 여부
     * @return Upsert 된 엔티티 목록
     */
    fun <D : Any> batchUpsert(
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
     * 엄격한 일관성이 필요한 경우 `SERIALIZABLE` 격리 수준을 사용하세요.
     *
     * @param pageNumber 페이지 번호 (0 이상)
     * @param pageSize 페이지 크기 (1 이상)
     * @param sortOrder 정렬 순서 (기본값: [SortOrder.ASC])
     * @param predicate 조건 (기본값: `Op.TRUE` - 전체 조회)
     * @return 페이징 결과 [ExposedPage] (content, totalCount, pageNumber, pageSize, totalPages 포함)
     *
     * ## 사용 예
     *
     * ```kotlin
     * transaction {
     *     // 첫 번째 페이지, 페이지당 20개, lastName이 "Depp"인 엔티티만 조회
     *     val page = repo.findPage(
     *         pageNumber = 0,
     *         pageSize   = 20,
     *     ) { ActorTable.lastName eq "Depp" }
     *
     *     println(page.content)    // 조회된 엔티티 목록
     *     println(page.totalCount) // 조건에 맞는 전체 엔티티 수
     *     println(page.totalPages) // 전체 페이지 수
     *     println(page.pageNumber) // 현재 페이지 번호
     * }
     * ```
     */
    fun findPage(
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
            )
        return ExposedPage(
            content = content,
            totalCount = totalCount,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }
}

/**
 * [Int] 기본키를 사용하는 [JdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [IntIdTable] 구현체
 * @param E 엔티티 타입
 */
interface IntJdbcRepository<T : IntIdTable, E : Any> : JdbcRepository<Int, T, E>

/**
 * [Long] 기본키를 사용하는 [JdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [LongIdTable] 구현체
 * @param E 엔티티 타입
 *
 * ## 사용 예
 *
 * ```kotlin
 * object ActorTable : LongIdTable("actors") {
 *     val firstName = varchar("first_name", 50)
 * }
 *
 * class ActorRepository : LongJdbcRepository<ActorTable, ActorRecord> {
 *     override val table = ActorTable
 *     override fun ResultRow.toEntity() = ActorRecord(
 *         id        = this[ActorTable.id].value,
 *         firstName = this[ActorTable.firstName],
 *     )
 * }
 * ```
 */
interface LongJdbcRepository<T : LongIdTable, E : Any> : JdbcRepository<Long, T, E>

/**
 * Kotlin [Uuid] 기본키를 사용하는 [JdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [UuidTable] 구현체
 * @param E 엔티티 타입
 */
@OptIn(ExperimentalUuidApi::class)
interface UuidJdbcRepository<T : UuidTable, E : Any> : JdbcRepository<Uuid, T, E>

/**
 * [java.util.UUID] 기본키를 사용하는 [JdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [UUIDTable] 구현체
 * @param E 엔티티 타입
 */
interface UUIDJdbcRepository<T : UUIDTable, E : Any> : JdbcRepository<UUID, T, E>

/**
 * [String] 기본키를 사용하는 [JdbcRepository]의 편의 타입 별칭입니다.
 *
 * @param T [IdTable]<String> 구현체
 * @param E 엔티티 타입
 */
interface StringJdbcRepository<T : IdTable<String>, E : Any> : JdbcRepository<String, T, E>
