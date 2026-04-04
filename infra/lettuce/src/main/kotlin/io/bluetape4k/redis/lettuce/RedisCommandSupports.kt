package io.bluetape4k.redis.lettuce

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.RedisClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis 서버의 명령어 지원 여부를 [RedisClient] 기준으로 캐싱하여 제공합니다.
 *
 * 동일 [RedisClient] 인스턴스(= 동일 endpoint)에 대한 조회 결과는 JVM 수명 동안 재사용됩니다.
 * `COMMAND INFO` 왕복 비용은 클라이언트당 명령어별로 최초 1회만 발생합니다.
 *
 * ```kotlin
 * val supportsHSetEx = RedisCommandSupports.supportsHSetEx(redisClient)
 * if (supportsHSetEx) {
 *     commands.hsetex(key, args, map)
 * } else {
 *     commands.hset(key, map)
 *     commands.expire(key, ttl)
 * }
 * ```
 */
object RedisCommandSupports: KLogging() {

    private val cache = ConcurrentHashMap<String, Boolean>()

    /**
     * Redis 서버가 해당 명령어를 지원하는지 확인합니다.
     *
     * [RedisClient] identity를 캐시 키로 사용하므로 동일 클라이언트에 대해 중복 조회가 발생하지 않습니다.
     * 내부적으로 임시 연결을 열어 `COMMAND INFO`를 실행하고 즉시 닫습니다.
     *
     * @param redisClient Redis 서버에 연결된 클라이언트
     * @param command 확인할 Redis 명령어 (대소문자 무관, 내부적으로 대문자로 정규화)
     * @return 서버가 해당 명령어를 지원하면 true
     */
    fun supports(redisClient: RedisClient, command: String): Boolean {
        val cacheKey = "${System.identityHashCode(redisClient)}:${command.uppercase()}"
        return cache.getOrPut(cacheKey) {
            val conn = redisClient.connect()
            try {
                runCatching {
                    val result = conn.sync().commandInfo(command)
                    val supported = result.isNotEmpty()
                    log.debug { "RedisCommandSupports: command=${command.uppercase()}, supported=$supported" }
                    supported
                }.getOrDefault(false)
            } finally {
                conn.close()
            }
        }
    }

    /**
     * `HSETEX` 명령어 지원 여부를 확인합니다. (Redis 8.0+)
     *
     * ```kotlin
     * if (RedisCommandSupports.supportsHSetEx(redisClient)) {
     *     // Redis 8.0+ HSETEX 사용 가능
     *     commands.hsetex(key, ttl, args, map)
     * }
     * ```
     *
     * @param redisClient Redis 서버에 연결된 클라이언트
     */
    fun supportsHSetEx(redisClient: RedisClient): Boolean = supports(redisClient, "HSETEX")

    /**
     * `LMPOP` 명령어 지원 여부를 확인합니다. (Redis 7.0+)
     *
     * ```kotlin
     * if (RedisCommandSupports.supportsLMPop(redisClient)) {
     *     // Redis 7.0+ LMPOP 사용 가능
     *     commands.lmpop(LMPopArgs.Builder.left().count(10), "mylist")
     * }
     * ```
     *
     * @param redisClient Redis 서버에 연결된 클라이언트
     */
    fun supportsLMPop(redisClient: RedisClient): Boolean = supports(redisClient, "LMPOP")

    /**
     * `WAITAOF` 명령어 지원 여부를 확인합니다. (Redis 7.2+)
     *
     * ```kotlin
     * if (RedisCommandSupports.supportsWaitAof(redisClient)) {
     *     // Redis 7.2+ WAITAOF 사용 가능
     *     commands.waitaof(1, 0, 0)
     * }
     * ```
     *
     * @param redisClient Redis 서버에 연결된 클라이언트
     */
    fun supportsWaitAof(redisClient: RedisClient): Boolean = supports(redisClient, "WAITAOF")

    /**
     * 캐시를 초기화합니다. 주로 테스트 격리 목적으로 사용합니다.
     */
    fun clearCache() {
        cache.clear()
    }
}
