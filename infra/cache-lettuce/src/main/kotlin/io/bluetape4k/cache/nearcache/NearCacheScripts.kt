package io.bluetape4k.cache.nearcache

import io.bluetape4k.redis.lettuce.script.RedisScript

/**
 * NearCache 전용 Lua 스크립트 모음.
 *
 * 개선: 기존에는 [LettuceNearCache]/[LettuceSuspendNearCache] 각각에서 raw string 상수를
 * 사용해 Redis 로 스크립트 원문을 매 호출마다 전송했습니다. 네트워크 비용·Redis 스크립트
 * 캐시 lookup 비용이 중복으로 발생했습니다.
 *
 * 이제 [RedisScript] 로 래핑해 로컬에서 계산한 SHA1 을 재사용하고, 실행부에서는 EVALSHA
 * 를 사용한 뒤 NOSCRIPT 가 발생하면 원문 전송 (EVAL) 으로 fallback 합니다.
 */
internal object NearCacheScripts {

    /**
     * CAS(Compare-And-Set) 스크립트.
     *
     * - GET 으로 현재 값을 가져와 `ARGV[1]` (oldValue) 과 비교
     * - 같다면 `SET KEY ARGV[2] XX KEEPTTL` 로 교체 후 `1` 반환
     * - 다르거나 없으면 `0` 반환
     *
     * `KEEPTTL` 옵션으로 기존 TTL 을 유지해 교체로 인해 만료가 초기화되지 않도록 한다.
     */
    val COMPARE_AND_SET: RedisScript = RedisScript(
        """
        local current = redis.call('GET', KEYS[1])
        if current == false or current ~= ARGV[1] then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[2], 'XX', 'KEEPTTL')
        return 1
        """.trimIndent()
    )
}
