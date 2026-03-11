package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Lettuce Redis 클라이언트를 이용한 분산 세마포어(Distributed Semaphore) 구현체입니다.
 *
 * Redis의 카운터(잔여 허가 수)를 사용하여 세마포어를 구현합니다.
 * Lua 스크립트를 통해 acquire/release를 원자적으로 처리합니다.
 *
 * 동기, 비동기(CompletableFuture) 2가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendSemaphore]를 사용하세요.
 *
 * ```kotlin
 * val semaphore = LettuceSemaphore(connection, "my-semaphore", totalPermits = 3)
 * semaphore.initialize() // 또는 trySetPermits(3)
 *
 * // 동기 방식
 * if (semaphore.tryAcquire()) {
 *     try { doWork() } finally { semaphore.release() }
 * }
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param semaphoreKey Redis에 저장될 세마포어 키
 * @param totalPermits 전체 허가 수
 */
class LettuceSemaphore(
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

    private val syncCommands: RedisCommands<String, String> get() = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    init {
        totalPermits.requirePositiveNumber("totalPermits")
    }

    /**
     * 세마포어를 초기화합니다. (이미 존재하면 무시)
     * `SET semaphoreKey totalPermits NX` 명령을 사용합니다.
     */
    fun initialize() {
        val args = SetArgs().nx()
        syncCommands.set(semaphoreKey, totalPermits.toString(), args)
        log.debug { "세마포어 초기화: semaphoreKey=$semaphoreKey, totalPermits=$totalPermits" }
    }

    /**
     * 허가 수를 강제로 설정합니다.
     *
     * @param permits 설정할 허가 수 (양수여야 합니다)
     * @throws IllegalArgumentException permits가 0 이하인 경우
     */
    fun trySetPermits(permits: Int) {
        permits.requirePositiveNumber("permits")
        syncCommands.set(semaphoreKey, permits.toString())
        log.debug { "세마포어 허가 수 설정: semaphoreKey=$semaphoreKey, permits=$permits" }
    }

    /**
     * 현재 사용 가능한 허가 수를 반환합니다.
     *
     * @return 잔여 허가 수 (초기화 안 된 경우 0)
     */
    fun availablePermits(): Int =
        syncCommands.get(semaphoreKey)?.toIntOrNull() ?: 0

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 즉시 허가 획득을 시도합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부
     */
    fun tryAcquire(permits: Int = 1): Boolean {
        permits.requirePositiveNumber("permits")

        val result = syncCommands.eval<Long>(
            ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString()
        )
        val acquired = result >= 0
        log.debug { "Semaphore tryAcquire: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
        return acquired
    }

    /**
     * 허가를 획득할 때까지 지정된 시간 동안 대기합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @throws IllegalStateException 지정된 시간 내에 허가를 획득하지 못한 경우
     */
    fun acquire(permits: Int = 1, waitTime: Duration = Duration.ofSeconds(30)) {
        permits.requirePositiveNumber("permits")

        val deadline = System.currentTimeMillis() + waitTime.toMillis()
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(permits)) return
            Thread.sleep(RETRY_DELAY_MS)
        }
        throw IllegalStateException("세마포어 획득 시간 초과: semaphoreKey=$semaphoreKey, permits=${permits}")
    }

    /**
     * 허가를 반납합니다.
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     */
    fun release(permits: Int = 1) {
        permits.requirePositiveNumber("permits")

        val remaining = syncCommands.eval<Long>(
            RELEASE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString(), totalPermits.toString()
        )
        log.debug { "Semaphore release: key=$semaphoreKey, permits=$permits, remaining=$remaining" }
    }

    // =========================================================================
    // 비동기 API (CompletableFuture)
    // =========================================================================

    /**
     * 즉시 허가 획득을 비동기로 시도합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부를 담은 CompletableFuture
     */
    fun tryAcquireAsync(permits: Int = 1): CompletableFuture<Boolean> {
        permits.requirePositiveNumber("permits")

        return asyncCommands.eval<Long>(
            ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString()
        ).toCompletableFuture().thenApply { result ->
            val acquired = result >= 0
            log.debug { "Semaphore tryAcquireAsync: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
            acquired
        }
    }

    /**
     * 허가를 획득할 때까지 비동기로 대기합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @return 완료를 나타내는 CompletableFuture
     */
    fun acquireAsync(permits: Int = 1, waitTime: Duration = Duration.ofSeconds(30)): CompletableFuture<Unit> {
        permits.requirePositiveNumber("permits")
        val deadline = System.currentTimeMillis() + waitTime.toMillis()

        fun attempt(): CompletableFuture<Unit> =
            tryAcquireAsync(permits).thenCompose { acquired ->
                if (acquired) {
                    CompletableFuture.completedFuture(Unit)
                } else if (System.currentTimeMillis() < deadline) {
                    // Thread.sleep 대신 delayedExecutor 를 사용하여 스레드 풀을 차단하지 않음
                    val delayed = CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                    CompletableFuture.runAsync({}, delayed).thenCompose { attempt() }
                } else {
                    CompletableFuture.failedFuture(
                        IllegalStateException("세마포어 획득 시간 초과 (async): semaphoreKey=$semaphoreKey")
                    )
                }
            }

        return attempt()
    }

    /**
     * 허가를 비동기로 반납합니다.
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     * @return 완료를 나타내는 CompletableFuture
     */
    fun releaseAsync(permits: Int = 1): CompletableFuture<Unit> {
        permits.requirePositiveNumber("permits")

        return asyncCommands.eval<Long>(
            RELEASE_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(semaphoreKey), permits.toString(), totalPermits.toString()
        ).toCompletableFuture().thenApply { remaining ->
            log.debug { "Semaphore releaseAsync: key=$semaphoreKey, permits=$permits, remaining=$remaining" }
        }
    }
}
