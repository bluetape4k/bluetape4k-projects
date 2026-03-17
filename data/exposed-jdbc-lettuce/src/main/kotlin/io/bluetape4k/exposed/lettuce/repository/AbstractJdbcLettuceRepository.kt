package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.map.ExposedEntityMapLoader
import io.bluetape4k.exposed.lettuce.map.ExposedEntityMapWriter
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceLoadedMap
import io.lettuce.core.RedisClient
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed DSL + Lettuce Redis 캐시를 결합한 추상 레포지토리.
 *
 * [JdbcLettuceRepository] 인터페이스를 구현하며, 서브클래스는 4개 추상 멤버를 구현한다:
 * - [table]: Exposed [IdTable]
 * - [ResultRow.toEntity]: ResultRow → E 변환
 * - [UpdateStatement.updateEntity]: UPDATE 컬럼 매핑
 * - [BatchInsertStatement.insertEntity]: INSERT 컬럼 매핑
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입
 * @param client Lettuce [RedisClient]
 * @param config [LettuceCacheConfig] 설정
 */
abstract class AbstractJdbcLettuceRepository<ID: Any, E: Any>(
    client: RedisClient,
    override val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
) : JdbcLettuceRepository<ID, E> {
    abstract override val table: IdTable<ID>

    abstract fun ResultRow.toEntity(): E

    abstract fun UpdateStatement.updateEntity(entity: E)

    abstract fun BatchInsertStatement.insertEntity(entity: E)

    open fun serializeKey(id: ID): String = id.toString()

    protected val cache: LettuceLoadedMap<ID, E> by lazy {
        LettuceLoadedMap(
            client = client,
            loader =
                ExposedEntityMapLoader(
                    table = table,
                    toEntity = { row -> with(this@AbstractJdbcLettuceRepository) { row.toEntity() } }
                ),
            writer =
                ExposedEntityMapWriter(
                    table = table,
                    writeMode = config.writeMode,
                    updateEntity = { stmt, e -> with(this@AbstractJdbcLettuceRepository) { stmt.updateEntity(e) } },
                    insertEntity = { stmt, e -> with(this@AbstractJdbcLettuceRepository) { stmt.insertEntity(e) } },
                    retryAttempts = config.writeRetryAttempts,
                    retryInterval = config.writeRetryInterval
                ),
            config = config,
            keySerializer = ::serializeKey
        )
    }

    // -------------------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // -------------------------------------------------------------------------

    override fun findByIdFromDb(id: ID): E? =
        transaction {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.let { with(this@AbstractJdbcLettuceRepository) { it.toEntity() } }
        }

    override fun findAllFromDb(ids: Collection<ID>): List<E> =
        transaction {
            if (ids.isEmpty()) return@transaction emptyList()
            table
                .selectAll()
                .where { table.id inList ids }
                .map { with(this@AbstractJdbcLettuceRepository) { it.toEntity() } }
        }

    override fun countFromDb(): Long =
        transaction {
            table.selectAll().count()
        }

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    override fun findById(id: ID): E? = cache[id]

    override fun findAll(ids: Collection<ID>): Map<ID, E> = cache.getAll(ids.toSet())

    override fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E> {
        val entities =
            transaction {
                table
                    .selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.let { limit(it) }
                        offset?.let { offset(it) }
                    }.map { with(this@AbstractJdbcLettuceRepository) { it.toEntity() } }
            }
        // 조회 결과를 캐시에 적재
        entities.forEach { entity ->
            runCatching { cache[extractId(entity)] = entity }
        }
        return entities
    }

    /**
     * 엔티티에서 ID를 추출한다.
     * 기본 구현은 [table]을 통한 DB 재조회이며, 서브클래스에서 직접 오버라이드 권장.
     */
    protected open fun extractId(entity: E): ID {
        error(
            "findAll(where) 사용 시 extractId(entity)를 오버라이드하거나 " +
                "엔티티에서 ID를 추출하는 방법을 제공해야 합니다."
        )
    }

    // -------------------------------------------------------------------------
    // 쓰기
    // -------------------------------------------------------------------------

    override fun save(
        id: ID,
        entity: E,
    ) {
        cache[id] = entity
    }

    override fun saveAll(entities: Map<ID, E>) {
        entities.forEach { (id, entity) -> cache[id] = entity }
    }

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    override fun delete(id: ID) {
        cache.delete(id)
    }

    override fun deleteAll(ids: Collection<ID>) {
        cache.deleteAll(ids)
    }

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    override fun clearCache() {
        cache.clear()
    }

    override fun close() = cache.close()
}
