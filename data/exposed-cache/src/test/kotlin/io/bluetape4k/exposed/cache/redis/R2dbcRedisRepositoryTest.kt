package io.bluetape4k.exposed.cache.redis

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.JdbcCacheRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.mockk.mockk
import io.bluetape4k.assertions.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.junit.jupiter.api.Test
import java.io.Serializable

/**
 * [R2dbcRedisRepository] 인터페이스 단위 테스트.
 *
 * invalidateByPattern의 기본 count 파라미터($default 메서드 경로)를 커버합니다.
 */
class R2dbcRedisRepositoryTest {

    companion object : KLogging()

    // ----------------------------------------------------------------
    // invalidateByPattern 기본 파라미터 ($default 메서드 경로)
    // ----------------------------------------------------------------

    @Test
    fun `invalidateByPattern을 count 없이 호출하면 DEFAULT_BATCH_SIZE가 사용된다`() {
        var capturedCount = -1
        val repo: R2dbcRedisRepository<Long, DummyEntity> = object : R2dbcRedisRepository<Long, DummyEntity> {
            override val table: IdTable<Long> = mockk(relaxed = true)
            override val cacheName = "test"
            override val cacheMode = CacheMode.LOCAL
            override val cacheWriteMode = CacheWriteMode.READ_ONLY
            override suspend fun ResultRow.toEntity() = DummyEntity()
            override fun extractId(entity: DummyEntity) = entity.id
            override suspend fun findByIdFromDb(id: Long): DummyEntity? = null
            override suspend fun findAllFromDb(ids: Collection<Long>) = emptyList<DummyEntity>()
            override suspend fun countFromDb() = 0L
            override suspend fun findAll(limit: Int?, offset: Long?, sortBy: Expression<*>, sortOrder: SortOrder, where: () -> Op<Boolean>) = emptyList<DummyEntity>()
            override suspend fun containsKey(id: Long) = false
            override suspend fun get(id: Long): DummyEntity? = null
            override suspend fun getAll(ids: Collection<Long>) = emptyMap<Long, DummyEntity>()
            override suspend fun put(id: Long, entity: DummyEntity) {}
            override suspend fun putAll(entities: Map<Long, DummyEntity>, batchSize: Int) {}
            override suspend fun invalidate(id: Long) {}
            override suspend fun invalidateAll(ids: Collection<Long>) {}
            override suspend fun clear() {}
            override suspend fun invalidateByPattern(patterns: String, count: Int): Long {
                capturedCount = count
                return 0L
            }
        }

        // count를 생략하면 인터페이스의 기본값 DEFAULT_BATCH_SIZE가 $default 경로로 주입된다.
        runSuspendIO { repo.invalidateByPattern("*test*") }
        capturedCount shouldBeEqualTo JdbcCacheRepository.DEFAULT_BATCH_SIZE
    }

    // ----------------------------------------------------------------
    // Stub entity
    // ----------------------------------------------------------------

    private data class DummyEntity(val id: Long = 0L) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }
}
