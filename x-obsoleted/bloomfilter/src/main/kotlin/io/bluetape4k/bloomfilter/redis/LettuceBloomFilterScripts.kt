package io.bluetape4k.bloomfilter.redis

/**
 * Lettuce 기반 Bloom Filter 구현체들이 공유하는 Lua Script 모음
 *
 * ## 동작/계약
 * - [ADD_SCRIPT]: KEYS[1] 키에 ARGV 의 모든 오프셋 비트를 원자적으로 1로 설정합니다.
 * - [CONTAINS_SCRIPT]: KEYS[1] 키에서 ARGV 의 모든 오프셋 비트를 원자적으로 조회하여,
 *   하나라도 0이면 즉시 `0`을 반환하고, 모두 1이면 `1`을 반환합니다.
 * - Redis Lua 스크립트는 단일 Redis 인스턴스에서 원자적으로 실행됩니다.
 */
internal object LettuceBloomFilterScripts {

    /**
     * Lua Script: 모든 오프셋 비트를 원자적으로 설정합니다.
     *
     * KEYS[1] = bloomName, ARGV = offset 목록
     */
    val ADD_SCRIPT = """
        for i = 1, #ARGV do
            redis.call('SETBIT', KEYS[1], ARGV[i], 1)
        end
        return 1
    """.trimIndent()

    /**
     * Lua Script: 모든 오프셋 비트를 원자적으로 조회하여 하나라도 0이면 0을 반환합니다.
     *
     * KEYS[1] = bloomName, ARGV = offset 목록
     */
    val CONTAINS_SCRIPT = """
        for i = 1, #ARGV do
            if redis.call('GETBIT', KEYS[1], ARGV[i]) == 0 then
                return 0
            end
        end
        return 1
    """.trimIndent()
}
