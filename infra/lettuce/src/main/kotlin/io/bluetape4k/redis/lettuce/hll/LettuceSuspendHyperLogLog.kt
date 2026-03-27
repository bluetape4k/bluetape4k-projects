package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await

/**
 * Lettuce 기반 HyperLogLog (Coroutine/Suspend).
 *
 * Redis의 PFADD/PFCOUNT/PFMERGE 명령을 코루틴으로 래핑합니다.
 *
 * **클러스터 주의:** [countWith]/[mergeWith]에서 여러 키를 전달할 경우,
 * 모두 동일한 Redis 슬롯에 있어야 합니다 (hash tag 사용 권장).
 *
 * @param V 원소 타입 (connection의 Codec으로 직렬화)
 */
class LettuceSuspendHyperLogLog<V : Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val name: String,
) : AutoCloseable {

    companion object : KLogging()

    private val asyncCommands: RedisAsyncCommands<String, V> get() = connection.async()

    /**
     * 원소를 추가합니다. HLL 내부 표현이 변경되면 true를 반환합니다.
     */
    suspend fun add(vararg elements: V): Boolean {
        val changed = asyncCommands.pfadd(name, *elements).await() == 1L
        log.debug { "SuspendHyperLogLog add: name=$name, changed=$changed" }
        return changed
    }

    /**
     * 이 HLL의 고유 원소 수 추정값을 반환합니다.
     */
    suspend fun count(): Long = asyncCommands.pfcount(name).await()

    /**
     * 이 HLL과 [others]를 합산한 고유 원소 수 추정값을 반환합니다.
     */
    suspend fun countWith(vararg others: LettuceSuspendHyperLogLog<V>): Long {
        val keys = arrayOf(name) + others.map { it.name }.toTypedArray()
        return asyncCommands.pfcount(*keys).await()
    }

    /**
     * 이 HLL과 [others]를 [destName]으로 병합합니다 (PFMERGE).
     */
    suspend fun mergeWith(destName: String, vararg others: LettuceSuspendHyperLogLog<V>) {
        val sourceKeys = arrayOf(name) + others.map { it.name }.toTypedArray()
        asyncCommands.pfmerge(destName, *sourceKeys).await()
        log.debug { "SuspendHyperLogLog merge: sources=${sourceKeys.toList()} → dest=$destName" }
    }

    override fun close() = connection.close()
}
