package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.SuspendedMapWriter
import io.bluetape4k.resilience4j.retry.withRetry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * R2DBC `suspendTransaction`을 사용해 DB에 엔티티를 쓰는 추상 [SuspendedMapWriter] 구현체.
 *
 * `runBlocking` 없이 코루틴 네이티브로 동작하며, Resilience4j [Retry]로 일시적 DB 오류를 자동 재시도한다.
 * [LettuceSuspendedLoadedMap]의 Write-through / Write-behind 전략에서 이 Writer가 사용된다.
 *
 * ### 사용 예시
 * ```kotlin
 * class MyWriter(table: IdTable<Long>) : R2dbcEntityMapWriter<Long, MyEntity>() {
 *     override suspend fun writeEntities(map: Map<Long, MyEntity>) { ... }
 *     override suspend fun deleteEntities(keys: Collection<Long>) { ... }
 * }
 * ```
 *
 * @param ID 키(PK) 타입
 * @param E 엔티티(DTO) 타입
 * @param retryConfig Resilience4j [RetryConfig] (기본: ofDefaults — 3회 재시도)
 * @see SuspendedMapWriter
 * @see R2dbcExposedEntityMapWriter
 */
abstract class R2dbcEntityMapWriter<ID: Any, E: Any>(
    retryConfig: RetryConfig = RetryConfig.ofDefaults(),
): SuspendedMapWriter<ID, E> {
    private val retry = Retry.of("exposed-r2dbc-lettuce-writer", retryConfig)

    /**
     * [map]의 엔티티를 `suspendTransaction` 내에서 DB에 일괄 저장한다.
     * Resilience4j retry로 일시적 오류를 재시도한다.
     */
    override suspend fun write(map: Map<ID, E>) {
        withRetry<Unit>(retry) {
            suspendTransaction { writeEntities(map) }
        }
    }

    /**
     * [keys]에 해당하는 엔티티를 `suspendTransaction` 내에서 DB에서 일괄 삭제한다.
     * Resilience4j retry로 일시적 오류를 재시도한다.
     */
    override suspend fun delete(keys: Collection<ID>) {
        withRetry<Unit>(retry) {
            suspendTransaction { deleteEntities(keys) }
        }
    }

    /**
     * 여러 엔티티를 일괄 저장(upsert)한다.
     * 서브클래스에서 실제 INSERT/UPDATE 로직을 구현한다.
     */
    protected abstract suspend fun writeEntities(map: Map<ID, E>)

    /**
     * 여러 엔티티를 일괄 삭제한다.
     * 서브클래스에서 실제 DELETE 로직을 구현한다.
     */
    protected abstract suspend fun deleteEntities(keys: Collection<ID>)
}
