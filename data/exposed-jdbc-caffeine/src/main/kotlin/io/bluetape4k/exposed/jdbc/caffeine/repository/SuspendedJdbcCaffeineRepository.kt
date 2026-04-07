package io.bluetape4k.exposed.jdbc.caffeine.repository

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.SuspendedJdbcCacheRepository
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import java.io.Serializable

/**
 * Exposed JDBC + Caffeine 로컬 캐시 suspend 저장소 인터페이스.
 *
 * JDBC `suspendedTransactionAsync`를 통해 모든 DB 접근이 suspend 함수로 이루어지며,
 * Caffeine [Cache]를 사용하여 인프로세스 캐싱을 제공합니다.
 *
 * @param ID PK 타입
 * @param E 엔티티(DTO) 타입. 캐시 저장을 위해 [Serializable] 구현 필수.
 */
interface SuspendedJdbcCaffeineRepository<ID: Any, E: Serializable>: SuspendedJdbcCacheRepository<ID, E> {

    companion object: KLogging() {
        const val DEFAULT_BATCH_SIZE = 500
    }

    /**
     * Caffeine 로컬 캐시 설정.
     */
    val config: LocalCacheConfig

    /**
     * Caffeine 동기 캐시. 키는 문자열로 직렬화된 ID입니다.
     */
    val cache: Cache<String, E>

    /**
     * 이 레포지토리가 사용하는 Exposed [IdTable].
     */
    override val table: IdTable<ID>

    /**
     * [ResultRow]를 엔티티 [E]로 변환합니다.
     */
    override fun ResultRow.toEntity(): E

    /**
     * 엔티티에서 식별자를 추출합니다.
     */
    override fun extractId(entity: E): ID
}
