package io.bluetape4k.exposed.cache

import io.bluetape4k.logging.KLogging
import io.mockk.mockk
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.junit.jupiter.api.Test
import java.io.Serializable

/**
 * [R2dbcCacheRepository] 인터페이스 단위 테스트.
 *
 * companion 상수 및 close() 기본 구현 경로를 커버합니다.
 */
class R2dbcCacheRepositoryTest {

    companion object : KLogging()

    // ----------------------------------------------------------------
    // Companion 상수
    // ----------------------------------------------------------------

    @Test
    fun `DEFAULT_BATCH_SIZE 상수는 500이다`() {
        R2dbcCacheRepository.DEFAULT_BATCH_SIZE shouldBeEqualTo 500
    }

    @Test
    fun `Companion 객체에 접근할 수 있다`() {
        val companion = R2dbcCacheRepository.Companion
        companion.shouldNotBeNull()
    }

    // ----------------------------------------------------------------
    // close() 기본 구현 — 인터페이스 DefaultImpls 경로
    // ----------------------------------------------------------------

    @Test
    fun `close() 기본 구현은 예외 없이 완료된다`() {
        val repo = MinimalR2dbcCacheRepository()
        repo.close()
    }

    // ----------------------------------------------------------------
    // Stub
    // ----------------------------------------------------------------

    private data class DummyEntity(val id: Long = 0L) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /** close()를 오버라이드하지 않아 인터페이스 기본 구현을 그대로 사용하는 스텁.
     *  R2DBC 는 ResultRow.toEntity()도 suspend 함수임에 유의.
     */
    private class MinimalR2dbcCacheRepository : R2dbcCacheRepository<Long, DummyEntity> {
        override val table: IdTable<Long> = mockk(relaxed = true)
        override val cacheName = "test"
        override val cacheMode = CacheMode.LOCAL
        override val cacheWriteMode = CacheWriteMode.READ_ONLY

        override suspend fun ResultRow.toEntity() = DummyEntity()
        override fun extractId(entity: DummyEntity) = entity.id

        override suspend fun findByIdFromDb(id: Long): DummyEntity? = null
        override suspend fun findAllFromDb(ids: Collection<Long>): List<DummyEntity> = emptyList()
        override suspend fun countFromDb(): Long = 0L
        override suspend fun findAll(
            limit: Int?,
            offset: Long?,
            sortBy: Expression<*>,
            sortOrder: SortOrder,
            where: () -> Op<Boolean>,
        ): List<DummyEntity> = emptyList()

        override suspend fun containsKey(id: Long) = false
        override suspend fun get(id: Long): DummyEntity? = null
        override suspend fun getAll(ids: Collection<Long>): Map<Long, DummyEntity> = emptyMap()
        override suspend fun put(id: Long, entity: DummyEntity) {}
        override suspend fun putAll(entities: Map<Long, DummyEntity>, batchSize: Int) {}
        override suspend fun invalidate(id: Long) {}
        override suspend fun invalidateAll(ids: Collection<Long>) {}
        override suspend fun clear() {}
        // close()는 오버라이드하지 않음 → 인터페이스 기본 구현( {} ) 사용
    }
}
