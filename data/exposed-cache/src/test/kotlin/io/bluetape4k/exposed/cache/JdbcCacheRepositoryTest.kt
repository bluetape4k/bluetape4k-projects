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
 * [JdbcCacheRepository] 인터페이스 단위 테스트.
 *
 * companion 상수 및 close() 기본 구현 경로를 커버합니다.
 */
class JdbcCacheRepositoryTest {

    companion object : KLogging()

    // ----------------------------------------------------------------
    // Companion 상수
    // ----------------------------------------------------------------

    @Test
    fun `DEFAULT_BATCH_SIZE 상수는 500이다`() {
        JdbcCacheRepository.DEFAULT_BATCH_SIZE shouldBeEqualTo 500
    }

    @Test
    fun `Companion 객체에 접근할 수 있다`() {
        // const val은 인라인되므로 companion을 직접 참조해야 초기화가 트리거된다.
        val companion = JdbcCacheRepository.Companion
        companion.shouldNotBeNull()
    }

    // ----------------------------------------------------------------
    // close() 기본 구현 — 인터페이스 DefaultImpls 경로
    // ----------------------------------------------------------------

    @Test
    fun `close() 기본 구현은 예외 없이 완료된다`() {
        val repo = MinimalJdbcCacheRepository()
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

    /** close()를 오버라이드하지 않아 인터페이스 기본 구현을 그대로 사용하는 스텁. */
    private class MinimalJdbcCacheRepository : JdbcCacheRepository<Long, DummyEntity> {
        override val table: IdTable<Long> = mockk(relaxed = true)
        override val cacheName = "test"
        override val cacheMode = CacheMode.LOCAL
        override val cacheWriteMode = CacheWriteMode.READ_ONLY

        override fun ResultRow.toEntity() = DummyEntity()
        override fun extractId(entity: DummyEntity) = entity.id

        override fun findByIdFromDb(id: Long): DummyEntity? = null
        override fun findAllFromDb(ids: Collection<Long>): List<DummyEntity> = emptyList()
        override fun countFromDb(): Long = 0L
        override fun findAll(
            limit: Int?,
            offset: Long?,
            sortBy: Expression<*>,
            sortOrder: SortOrder,
            where: () -> Op<Boolean>,
        ): List<DummyEntity> = emptyList()

        override fun containsKey(id: Long) = false
        override fun get(id: Long): DummyEntity? = null
        override fun getAll(ids: Collection<Long>): Map<Long, DummyEntity> = emptyMap()
        override fun put(id: Long, entity: DummyEntity) {}
        override fun putAll(entities: Map<Long, DummyEntity>, batchSize: Int) {}
        override fun invalidate(id: Long) {}
        override fun invalidateAll(ids: Collection<Long>) {}
        override fun clear() {}
        // close()는 오버라이드하지 않음 → 인터페이스 기본 구현( {} ) 사용
    }
}
