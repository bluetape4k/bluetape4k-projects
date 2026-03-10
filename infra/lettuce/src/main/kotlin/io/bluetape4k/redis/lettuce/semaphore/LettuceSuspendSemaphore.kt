package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Lettuce Redis 클라이언트를 이용한 분산 세마포어의 코루틴(suspend) 구현체입니다.
 *
 * [LettuceSemaphore]의 코루틴 버전으로, Redis의 카운터(잔여 허가 수)를 사용하여 세마포어를 구현합니다.
 * Lua 스크립트를 통해 acquire/release를 원자적으로 처리합니다.
 *
 * ```kotlin
 * val semaphore = LettuceSuspendSemaphore(connection, "my-semaphore", totalPermits = 3)
 * semaphore.initialize()
 *
 * if (semaphore.tryAcquire()) {
 *     try { doWork() } finally { semaphore.release() }
 * }
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param semaphoreKey Redis에 저장될 세마포어 키
 * @param totalPermits 전체 허가 수
 */
class LettuceSuspendSemaphore(
    private val connection: StatefulRedisConnection<String, String>,
    val semaphoreKey: String,
    val totalPermits: Int,
) {
    companion object: KLogging() {
        private const val RETRY_DELAY_MS = 50L

        /**
         * Lua: 원자적 acquire
         * KEYS[1]=semaphoreKey, ARGV[1]=permits
         * 반환: 남은 허가 수 (허가 수 부족 시 -1)
         */
        private const val ACQUIRE_SCRIPT = """
local v = tonumber(redis.call('get', KEYS[1]))
if v and v >= tonumber(ARGV[1]) then
  return redis.call('decrby', KEYS[1], ARGV[1])
else
  return -1
end"""

        /**
         * Lua: 원자적 release (최대값 초과 방지)
         * KEYS[1]=semaphoreKey, ARGV[1]=permits, ARGV[2]=maxPermits
         * 반환: 현재 남은 허가 수
         */
        private const val RELEASE_SCRIPT = """
local v = tonumber(redis.call('incrby', KEYS[1], ARGV[1]))
if v > tonumber(ARGV[2]) then
  redis.call('set', KEYS[1], ARGV[2])
  return tonumber(ARGV[2])
end
return v"""
    }

    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /**
     * 세마포어를 초기화합니다. (이미 존재하면 무시)
     * `SET semaphoreKey totalPermits NX` 명령을 사용합니다.
     */
    suspend fun initialize() {
        asyncCommands.set(semaphoreKey, totalPermits.toString(), SetArgs().nx()).awaitSuspending()
        log.debug { "세마포어 초기화: semaphoreKey=$semaphoreKey, totalPermits=$totalPermits" }
    }

    /**
     * 허가 수를 강제로 설정합니다.
     *
     * @param permits 설정할 허가 수
     */
    suspend fun trySetPermits(permits: Int) {
        asyncCommands.set(semaphoreKey, permits.toString()).awaitSuspending()
        log.debug { "세마포어 허가 수 설정: semaphoreKey=$semaphoreKey, permits=$permits" }
    }

    /**
     * 현재 사용 가능한 허가 수를 반환합니다.
     *
     * @return 잔여 허가 수 (초기화 안 된 경우 0)
     */
    suspend fun availablePermits(): Int =
        asyncCommands.get(semaphoreKey).awaitSuspending()?.toIntOrNull() ?: 0

    // =========================================================================
    // 코루틴 API (suspend)
    // =========================================================================

    /**
     * 즉시 허가 획득을 코루틴으로 시도합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부
     */
    suspend fun tryAcquire(permits: Int = 1): Boolean {
        require(permits > 0) { "permits는 양수여야 합니다: $permits" }
        val result = asyncCommands.eval<Long>(
            ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString()
        ).awaitSuspending()
        val acquired = result >= 0
        log.debug { "Semaphore tryAcquire: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
        return acquired
    }

    /**
     * 허가를 획득할 때까지 코루틴으로 대기합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @throws IllegalStateException 지정된 시간 내에 허가를 획득하지 못한 경우
     */
    suspend fun acquire(permits: Int = 1, waitTime: Duration = 30.seconds) {
        require(permits > 0) { "permits는 양수여야 합니다: $permits" }
        val deadline = System.currentTimeMillis() + waitTime.inWholeMilliseconds
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(permits)) return
            delay(RETRY_DELAY_MS.milliseconds)
        }
        throw IllegalStateException("세마포어 획득 시간 초과 (suspend): semaphoreKey=$semaphoreKey, permits=$permits")
    }

    /**
     * 허가를 코루틴으로 반납합니다.
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     */
    suspend fun release(permits: Int = 1) {
        require(permits > 0) { "permits는 양수여야 합니다: $permits" }
        val remaining = asyncCommands.eval<Long>(
            RELEASE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString(), totalPermits.toString()
        ).awaitSuspending()
        log.debug { "Semaphore release: key=$semaphoreKey, permits=$permits, remaining=$remaining" }
    }
}
