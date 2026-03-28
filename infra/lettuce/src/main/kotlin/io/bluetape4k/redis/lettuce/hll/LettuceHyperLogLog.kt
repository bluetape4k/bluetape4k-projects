package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

/**
 * Redis HyperLogLog 명령을 래핑한 동기 구현입니다.
 *
 * `PFADD`, `PFCOUNT`, `PFMERGE`를 감싸서 근사 카디널리티를 계산합니다.
 *
 * @property connection HyperLogLog 값 타입을 담는 Redis 연결
 * @property name HyperLogLog Redis 키
 */
class LettuceHyperLogLog<V: Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val name: String,
): AutoCloseable {

    companion object: KLogging()

    private val commands: RedisCommands<String, V> = connection.sync()

    /** 원소를 추가하고 구조가 변경되었는지 반환합니다. */
    fun add(vararg elements: V): Boolean {
        val changed = commands.pfadd(name, *elements) == 1L
        log.debug { "HyperLogLog add: name=$name, changed=$changed" }
        return changed
    }

    /** 현재 근사 카디널리티를 반환합니다. */
    fun count(): Long = commands.pfcount(name)

    /** 현재 HLL과 다른 HLL들의 합산 근사 카디널리티를 반환합니다. */
    fun countWith(vararg others: LettuceHyperLogLog<V>): Long {
        val keys = arrayOf(name) + others.map { it.name }.toTypedArray()
        return commands.pfcount(*keys)
    }

    /** 현재 HLL과 다른 HLL들을 [destName]으로 병합합니다. */
    fun mergeWith(destName: String, vararg others: LettuceHyperLogLog<V>) {
        val sourceKeys = arrayOf(name) + others.map { it.name }.toTypedArray()
        commands.pfmerge(destName, *sourceKeys)
        log.debug { "HyperLogLog merge: sources=${sourceKeys.toList()} -> dest=$destName" }
    }

    override fun close() = connection.close()
}
