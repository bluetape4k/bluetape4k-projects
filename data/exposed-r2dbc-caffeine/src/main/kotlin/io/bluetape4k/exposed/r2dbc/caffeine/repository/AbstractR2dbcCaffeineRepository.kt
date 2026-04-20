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
import kotlinx.coroutines.CancellationException
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

    /**
     * Write-Behind 백그라운드 Job.
     *
     * 채널에서 항목을 수신하여 배치 크기([LocalCacheConfig.writeBehindBatchSize])만큼
     * 모아서 [flushBatch]로 DB에 일괄 기록합니다.
     * 채널이 닫히면 for 루프가 종료되고, finally 블록에서 미처리 항목을 마지막으로 flush합니다.
     * lazy 초기화이므로 WRITE_BEHIND 모드로 처음 put()이 호출될 때 Job이 시작됩니다.
     */
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
                // 채널 닫힌 후에도 루프에서 빠져나온 시점의 미처리 항목을 DB에 기록해야
                // 데이터 유실을 방지할 수 있다.
                if (batch.isNotEmpty()) {
                    flushBatch(batch)
                }
            }
        }
    }

    /**
     * Write-Behind 배치를 DB에 flush합니다.
     *
     * AutoIncrement 테이블의 경우 신규 엔티티는 DB에 삽입하지 않습니다.
     * 코루틴 취소 시 [CancellationException]은 반드시 재던져야 하므로, Exception 캐치 전에 별도로 처리합니다.
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
        } catch (e: CancellationException) {
            // 코루틴 취소는 반드시 재던져야 한다 — 삼키면 구조적 동시성이 깨진다
            throw e
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
        // 조회 결과를 캐시에 적재.
        // extractId()가 UnsupportedOperationException을 던질 수 있으므로 Exception만 캐치하되,
        // CancellationException은 재던져 코루틴 취소 신호가 유실되지 않도록 한다.
        if (entities.isNotEmpty()) {
            entities.forEach { entity ->
                try {
                    val id = extractId(entity)
                    cache.put(serializeKey(id), CompletableFuture.completedFuture(entity))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.warn(e) { "findAll: 캐시 적재 실패 — extractId()가 구현되지 않았을 수 있습니다" }
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
                // writeBehindJob은 lazy이므로 첫 send() 전에 명시적으로 접근하여
                // 백그라운드 소비 루프가 시작되도록 보장한다.
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
     *
     * UPDATE를 먼저 시도하고 영향 행이 0이면 INSERT로 upsert를 구현합니다.
     * AutoIncrement 테이블은 DB가 ID를 자동 할당하므로 클라이언트 생성 ID로
     * INSERT하면 충돌이 발생할 수 있어 신규 엔티티 삽입을 건너뜁니다.
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

    /**
     * 레포지토리를 닫습니다.
     *
     * Write-Behind 모드인 경우 채널을 닫아 새로운 항목 수신을 중단합니다.
     * writeBehindJob은 채널이 닫힌 후 남은 항목을 모두 처리하고 종료됩니다.
     * `runBlocking`을 사용하면 Virtual Thread와 충돌하므로, scope를 취소하여
     * Job이 자연스럽게 종료되도록 위임합니다.
     */
    override fun close() {
        if (config.writeMode == CacheWriteMode.WRITE_BEHIND) {
            writeBehindQueue.close()
            // writeBehindJob은 채널 닫힘을 감지하고 남은 배치를 처리한 뒤 자동 종료됩니다.
            // runBlocking을 쓰면 Virtual Thread와 충돌하므로, scope 취소로 대신합니다.
        }
        cache.synchronous().invalidateAll()
        scope.cancel()
    }
}
