package io.bluetape4k.exposed.r2dbc.caffeine.repository

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.io.Serializable
import java.util.concurrent.CompletableFuture

/**
 * Exposed R2DBC + Caffeine 로컬 캐시를 결합한 추상 레포지토리.
 *
 * JDBC 의존 없이 Caffeine [AsyncCache]를 사용하여 인프로세스 캐싱을 제공합니다.
 * R2DBC `suspendTransaction`을 통해 모든 DB 접근이 suspend 함수로 이루어집니다.
 *
 * 서브클래스는 4개 추상 멤버를 구현합니다:
 * - [table]: Exposed [IdTable]
 * - [ResultRow.toEntity]: ResultRow → E 변환 (suspend)
 * - [UpdateStatement.updateEntity]: UPDATE 컬럼 매핑
 * - [BatchInsertStatement.insertEntity]: INSERT 컬럼 매핑
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입. 캐시 저장을 위해 [Serializable] 구현 필수.
 * @param config [LocalCacheConfig] 설정
 */
abstract class AbstractR2dbcCaffeineRepository<ID: Any, E: Serializable>(
    override val config: LocalCacheConfig = LocalCacheConfig.WRITE_THROUGH,
): R2dbcCaffeineRepository<ID, E> {

    companion object: KLogging()

    abstract override val table: IdTable<ID>

    /** [ResultRow]를 엔티티 [E]로 변환하는 suspend 함수 */
    abstract override suspend fun ResultRow.toEntity(): E

    /** 기존 엔티티 UPDATE 시 컬럼 매핑 */
    abstract fun UpdateStatement.updateEntity(entity: E)

    /** 신규 엔티티 INSERT 시 컬럼 매핑 */
    abstract fun BatchInsertStatement.insertEntity(entity: E)

    /** 엔티티 ID를 캐시 키 문자열로 직렬화합니다 (기본: toString()) */
    open fun serializeKey(id: ID): String = id.toString()

    // -------------------------------------------------------------------------
    // R2dbcCacheRepository 필수 프로퍼티 구현
    // -------------------------------------------------------------------------

    /** 캐시 이름 (키 접두사로 사용) */
    override val cacheName: String
        get() = config.keyPrefix

    /** 캐시 저장 방식 — Caffeine은 항상 LOCAL */
    override val cacheMode: CacheMode
        get() = CacheMode.LOCAL

    /** 캐시 쓰기 전략 */
    override val cacheWriteMode: CacheWriteMode
        get() = config.writeMode

    // -------------------------------------------------------------------------
    // Caffeine AsyncCache
    // -------------------------------------------------------------------------

    override val cache: AsyncCache<String, E> by lazy {
        Caffeine.newBuilder()
            .maximumSize(config.maximumSize)
            .expireAfterWrite(config.expireAfterWrite)
            .apply { config.expireAfterAccess?.let { expireAfterAccess(it) } }
            .buildAsync()
    }

    // -------------------------------------------------------------------------
    // Write-Behind 지원
    // -------------------------------------------------------------------------

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val writeBehindQueue: Channel<Pair<ID, E>> by lazy {
        Channel(capacity = config.writeBehindQueueCapacity)
    }

    private val writeBehindJob by lazy {
        scope.launch {
            val batch = mutableListOf<Pair<ID, E>>()
            try {
                for (entry in writeBehindQueue) {
                    batch.add(entry)
                    // 큐에 남아있는 항목을 배치 크기까지 추가로 수집
                    while (batch.size < config.writeBehindBatchSize) {
                        val next = writeBehindQueue.tryReceive().getOrNull() ?: break
                        batch.add(next)
                    }
                    if (batch.isNotEmpty()) {
                        flushBatch(batch)
                        batch.clear()
                    }
                }
            } finally {
                // 채널 닫힌 후 남은 항목 처리
                if (batch.isNotEmpty()) {
                    flushBatch(batch)
                }
            }
        }
    }

    /**
     * Write-Behind 배치를 DB에 flush합니다.
     * AutoIncrement 테이블의 경우 신규 엔티티는 DB에 삽입하지 않습니다.
     */
    private suspend fun flushBatch(batch: List<Pair<ID, E>>) {
        try {
            suspendTransaction {
                for ((id, entity) in batch) {
                    val updated = table.update({ table.id eq id }) {
                        it.updateEntity(entity)
                    }
                    // AutoInc 테이블은 DB가 ID를 할당하므로 클라이언트 생성 ID로 INSERT하지 않는다
                    if (updated == 0 && table.id.autoIncColumnType == null) {
                        table.batchInsert(listOf(entity)) {
                            insertEntity(it)
                        }
                    }
                }
            }
            log.debug { "Write-Behind: ${batch.size}건 DB flush 완료" }
        } catch (e: Exception) {
            log.warn(e) { "Write-Behind: ${batch.size}건 DB flush 실패" }
        }
    }

    // -------------------------------------------------------------------------
    // DB 직접 조회 (캐시 우회)
    // -------------------------------------------------------------------------

    override suspend fun findByIdFromDb(id: ID): E? =
        suspendTransaction {
            table
                .selectAll()
                .where { table.id eq id }
                .singleOrNull()
                ?.toEntity()
        }

    override suspend fun findAllFromDb(ids: Collection<ID>): List<E> =
        suspendTransaction {
            if (ids.isEmpty()) return@suspendTransaction emptyList()
            table
                .selectAll()
                .where { table.id inList ids }
                .map { it.toEntity() }
                .toList()
        }

    override suspend fun countFromDb(): Long =
        suspendTransaction {
            table.selectAll().count()
        }

    // -------------------------------------------------------------------------
    // 캐시 기반 조회 (Read-through)
    // -------------------------------------------------------------------------

    override suspend fun containsKey(id: ID): Boolean = get(id) != null

    override suspend fun get(id: ID): E? {
        val key = serializeKey(id)
        val cached = cache.getIfPresent(key)?.await()
        if (cached != null) return cached

        val fromDb = findByIdFromDb(id) ?: return null
        cache.put(key, CompletableFuture.completedFuture(fromDb))
        return fromDb
    }

    override suspend fun getAll(ids: Collection<ID>): Map<ID, E> {
        if (ids.isEmpty()) return emptyMap()

        val result = mutableMapOf<ID, E>()
        val missedIds = mutableListOf<ID>()

        for (id in ids) {
            val key = serializeKey(id)
            val cached = cache.getIfPresent(key)?.await()
            if (cached != null) {
                result[id] = cached
            } else {
                missedIds.add(id)
            }
        }

        if (missedIds.isNotEmpty()) {
            val fromDb = findAllFromDb(missedIds)
            for (entity in fromDb) {
                val id = extractId(entity)
                result[id] = entity
                cache.put(serializeKey(id), CompletableFuture.completedFuture(entity))
            }
        }

        return result
    }

    override suspend fun findAll(
        limit: Int?,
        offset: Long?,
        sortBy: Expression<*>,
        sortOrder: SortOrder,
        where: () -> Op<Boolean>,
    ): List<E> {
        val entities =
            suspendTransaction {
                table
                    .selectAll()
                    .where(where)
                    .apply {
                        orderBy(sortBy, sortOrder)
                        limit?.let { limit(it) }
                        offset?.let { offset(it) }
                    }.map { with(this@AbstractR2dbcCaffeineRepository) { it.toEntity() } }
                    .toList()
            }
        // 조회 결과를 캐시에 적재
        if (entities.isNotEmpty()) {
            entities.forEach { entity ->
                runCatching {
                    val id = extractId(entity)
                    cache.put(serializeKey(id), CompletableFuture.completedFuture(entity))
                }
            }
        }
        return entities
    }

    /**
     * 엔티티에서 ID를 추출합니다.
     * [findAll] (where 조건 버전) 사용 시 서브클래스에서 override 필요.
     */
    override fun extractId(entity: E): ID =
        error(
            "findAll(where) 사용 시 extractId(entity)를 오버라이드하거나 " +
                "엔티티에서 ID를 추출하는 방법을 제공해야 합니다."
        )

    // -------------------------------------------------------------------------
    // 쓰기 (캐시 + DB)
    // -------------------------------------------------------------------------

    override suspend fun put(id: ID, entity: E) {
        val key = serializeKey(id)
        cache.put(key, CompletableFuture.completedFuture(entity))

        when (config.writeMode) {
            CacheWriteMode.WRITE_THROUGH -> writeToDb(id, entity)
            CacheWriteMode.WRITE_BEHIND -> {
                // Write-Behind Job 초기화 보장
                writeBehindJob
                writeBehindQueue.send(id to entity)
            }

            else -> { /* READ_ONLY: 캐시만 갱신 */
            }
        }
    }

    override suspend fun putAll(entities: Map<ID, E>, batchSize: Int) {
        batchSize.requirePositiveNumber("batchSize")
        entities.forEach { (id, entity) -> put(id, entity) }
    }

    /**
     * Write-Through 시 단일 엔티티를 DB에 저장합니다.
     * AutoIncrement 테이블의 경우 신규 엔티티는 DB에 삽입하지 않습니다.
     */
    private suspend fun writeToDb(id: ID, entity: E) {
        suspendTransaction {
            val updated = table.update({ table.id eq id }) {
                it.updateEntity(entity)
            }
            // AutoInc 테이블은 DB가 ID를 할당하므로 클라이언트 생성 ID로 INSERT하지 않는다
            if (updated == 0 && table.id.autoIncColumnType == null) {
                table.batchInsert(listOf(entity)) {
                    insertEntity(it)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 삭제
    // -------------------------------------------------------------------------

    override suspend fun invalidate(id: ID) {
        cache.synchronous().invalidate(serializeKey(id))
    }

    override suspend fun invalidateAll(ids: Collection<ID>) {
        val keys = ids.map { serializeKey(it) }
        cache.synchronous().invalidateAll(keys)
    }

    // -------------------------------------------------------------------------
    // 캐시 관리
    // -------------------------------------------------------------------------

    override suspend fun clear() {
        cache.synchronous().invalidateAll()
    }

    override fun close() {
        if (config.writeMode == CacheWriteMode.WRITE_BEHIND) {
            writeBehindQueue.close()
            runBlocking { writeBehindJob.join() }
        }
        cache.synchronous().invalidateAll()
        scope.cancel()
    }
}
