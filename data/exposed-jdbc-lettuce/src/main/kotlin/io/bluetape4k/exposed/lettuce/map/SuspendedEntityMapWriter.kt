package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.redis.lettuce.map.SuspendedMapWriter
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync

/**
 * JDBC `suspendedTransactionAsync`를 사용해 DB에 엔티티를 쓰는 추상 [SuspendedMapWriter] 구현체.
 *
 * Blocking JDBC I/O를 [Dispatchers.IO]에서 안전하게 실행하며,
 * Resilience4j retry를 코루틴 네이티브로 지원한다.
 *
 * @param ID 키 타입
 * @param E 엔티티(DTO) 타입
 * @param retryConfig Resilience4j [RetryConfig] (기본: 3회 재시도)
 */
abstract class SuspendedEntityMapWriter<ID: Any, E: Any>(
    retryConfig: RetryConfig = RetryConfig.ofDefaults(),
): SuspendedMapWriter<ID, E> {
    private val retry = Retry.of("exposed-jdbc-lettuce-suspended-writer", retryConfig)

    @Suppress("DEPRECATION")
    override suspend fun write(map: Map<ID, E>) {
        retry.executeSuspendFunction {
            suspendedTransactionAsync(Dispatchers.IO) { writeEntities(map) }.await()
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun delete(keys: Collection<ID>) {
        retry.executeSuspendFunction {
            suspendedTransactionAsync(Dispatchers.IO) { deleteEntities(keys) }.await()
        }
    }

    /**
     * 여러 엔티티를 일괄 저장(upsert)한다.
     */
    protected abstract fun writeEntities(map: Map<ID, E>)

    /**
     * 여러 엔티티를 일괄 삭제한다.
     */
    protected abstract fun deleteEntities(keys: Collection<ID>)
}
