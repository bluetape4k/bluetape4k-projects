package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

/**
 * Redis HyperLogLog 명령을 래핑한 코루틴 구현입니다.
 *
 * `PFADD`, `PFCOUNT`, `PFMERGE`를 suspend 함수로 제공합니다.
 */
class LettuceSuspendHyperLogLog<V: Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val name: String,
): AutoCloseable {

    companion object: KLogging()

    private val asyncCommands: RedisAsyncCommands<String, V> get() = connection.async()

    /** 원소를 추가하고 구조가 변경되었는지 반환합니다. */
    suspend fun add(vararg elements: V): Boolean {
        val changed = asyncCommands.pfadd(name, *elements).awaitSuspending() == 1L
        log.debug { "SuspendHyperLogLog add: name=$name, changed=$changed" }
        return changed
    }

    /** 현재 근사 카디널리티를 반환합니다. */
    suspend fun count(): Long = asyncCommands.pfcount(name).awaitSuspending()

    /** 현재 HLL과 다른 HLL들의 합산 근사 카디널리티를 반환합니다. */
    suspend fun countWith(vararg others: LettuceSuspendHyperLogLog<V>): Long {
        val keys = arrayOf(name) + others.map { it.name }.toTypedArray()
        return asyncCommands.pfcount(*keys).awaitSuspending()
    }

    /** 현재 HLL과 다른 HLL들을 [destName]으로 병합합니다. */
    suspend fun mergeWith(destName: String, vararg others: LettuceSuspendHyperLogLog<V>) {
        val sourceKeys = arrayOf(name) + others.map { it.name }.toTypedArray()
        asyncCommands.pfmerge(destName, *sourceKeys).awaitSuspending()
        log.debug { "SuspendHyperLogLog merge: sources=${sourceKeys.toList()} -> dest=$destName" }
    }

    override fun close() = connection.close()
}
