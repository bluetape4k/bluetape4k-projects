package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

/**
 * Lettuce 기반 HyperLogLog (Sync).
 *
 * Redis의 PFADD/PFCOUNT/PFMERGE 명령을 래핑합니다.
 * 카디널리티 추정 오차율 ≈ 0.81%.
 *
 * **클러스터 주의:** [countWith]/[mergeWith]에서 여러 키를 전달할 경우,
 * 모두 동일한 Redis 슬롯에 있어야 합니다 (hash tag 사용 권장).
 *
 * @param V 원소 타입 (connection의 Codec으로 직렬화)
 */
class LettuceHyperLogLog<V : Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val name: String,
) : AutoCloseable {

    companion object : KLogging()

    private val commands: RedisCommands<String, V> = connection.sync()

    /**
     * 원소를 추가합니다. HLL 내부 표현이 변경되면 true를 반환합니다.
     */
    fun add(vararg elements: V): Boolean {
        val changed = commands.pfadd(name, *elements) == 1L
        log.debug { "HyperLogLog add: name=$name, elements=${elements.size}, changed=$changed" }
        return changed
    }

    /**
     * 이 HLL의 고유 원소 수 추정값을 반환합니다.
     */
    fun count(): Long = commands.pfcount(name)

    /**
     * 이 HLL과 [others]를 합산한 고유 원소 수 추정값을 반환합니다.
     * HLL 자체는 변경되지 않습니다.
     */
    fun countWith(vararg others: LettuceHyperLogLog<V>): Long {
        val keys = arrayOf(name) + others.map { it.name }.toTypedArray()
        return commands.pfcount(*keys)
    }

    /**
     * 이 HLL과 [others]를 [destName]으로 병합합니다 (PFMERGE).
     */
    fun mergeWith(destName: String, vararg others: LettuceHyperLogLog<V>) {
        val sourceKeys = arrayOf(name) + others.map { it.name }.toTypedArray()
        commands.pfmerge(destName, *sourceKeys)
        log.debug { "HyperLogLog merge: sources=${sourceKeys.toList()} → dest=$destName" }
    }

    override fun close() = connection.close()
}
