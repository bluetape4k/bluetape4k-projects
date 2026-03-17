package io.bluetape4k.exposed.r2dbc.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapWriter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Resilience4j Retry를 내장한 추상 [MapWriter] 구현체.
 *
 * [LettuceLoadedMap]의 동기 [MapWriter] 인터페이스와 R2DBC suspend API를 브리지한다.
 * 내부적으로 `runBlocking(Dispatchers.IO)`를 사용하여 suspend 호출을 동기로 래핑한다.
 *
 * @param ID 키 타입
 * @param E 엔티티(DTO) 타입
 * @param retryConfig Resilience4j [RetryConfig]
 */
abstract class R2dbcEntityMapWriter<ID : Any, E : Any>(
    retryConfig: RetryConfig = RetryConfig.ofDefaults(),
) : MapWriter<ID, E> {
    private val retry = Retry.of("exposed-r2dbc-lettuce-writer", retryConfig)

    override fun write(map: Map<ID, E>) =
        Retry
            .decorateRunnable(retry) {
                runBlocking(Dispatchers.IO) {
                    suspendTransaction { writeEntities(map) }
                }
            }.run()

    override fun delete(keys: Collection<ID>) =
        Retry
            .decorateRunnable(retry) {
                runBlocking(Dispatchers.IO) {
                    suspendTransaction { deleteEntities(keys) }
                }
            }.run()

    /**
     * 여러 엔티티를 일괄 저장(upsert)한다.
     */
    protected abstract suspend fun writeEntities(map: Map<ID, E>)

    /**
     * 여러 엔티티를 일괄 삭제한다.
     */
    protected abstract suspend fun deleteEntities(keys: Collection<ID>)
}
