package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lettuce Redis 클라이언트를 이용한 분산 세마포어의 코루틴(suspend) 구현체입니다.
 *
 * [LettuceSemaphore]의 코루틴 버전으로, Redis의 카운터(잔여 허가 수), owner token hash,
 * expiry sorted set을 사용하여 세마포어를 구현합니다.
 * Lua 스크립트를 통해 acquire/release/expired-owner cleanup을 원자적으로 처리합니다.
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
 * @param leaseTime 획득한 permit owner token의 lease time
 */
class LettuceSuspendSemaphore(
    private val connection: StatefulRedisConnection<String, String>,
    val semaphoreKey: String,
    val totalPermits: Int,
    private val leaseTime: Duration = Duration.ofSeconds(30),
) {
    companion object: KLogging() {
        private const val RETRY_DELAY_MS = 50L
    }

    // 개선: getter → final field 로 변경 (매 호출 connection.async() 호출 제거).
    private val asyncCommands: RedisAsyncCommands<String, String> = connection.async()
    private val ownersKey = "$semaphoreKey:owners"
    private val expirationsKey = "$semaphoreKey:expirations"
    private val localPermits = LocalSemaphorePermits()
    private val leaseMillis = leaseTime.requirePositiveMillis("leaseTime").toMillis()

    init {
        totalPermits.requirePositiveNumber("totalPermits")
    }

    /**
     * 세마포어를 초기화합니다. (이미 존재하면 무시)
     * `SET semaphoreKey totalPermits NX` 명령을 사용합니다.
     */
    suspend fun initialize() {
        asyncCommands.set(semaphoreKey, totalPermits.toString(), SetArgs().nx()).await()
        log.debug { "세마포어 초기화: semaphoreKey=$semaphoreKey, totalPermits=$totalPermits" }
    }

    /**
     * 허가 수를 강제로 설정합니다.
     *
     * @param permits 설정할 허가 수 (양수여야 합니다)
     * @throws IllegalArgumentException permits가 0 이하인 경우
     */
    suspend fun trySetPermits(permits: Int) {
        permits.requirePositiveNumber("permits")
        asyncCommands.set(semaphoreKey, permits.toString()).await()
        asyncCommands.del(ownersKey, expirationsKey).await()
        localPermits.clear()
        log.debug { "세마포어 허가 수 설정: semaphoreKey=$semaphoreKey, permits=$permits" }
    }

    /**
     * 현재 사용 가능한 허가 수를 반환합니다.
     *
     * @return 잔여 허가 수 (초기화 안 된 경우 0)
     */
    suspend fun availablePermits(): Int =
        RedisScriptRunner.runSuspending<Long>(
            asyncCommands, LettuceSemaphoreScripts.AVAILABLE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(), nowMillis().toString(), totalPermits.toString()
        ).toInt()

    // =========================================================================
    // 코루틴 API (suspend)
    // =========================================================================

    /**
     * 즉시 허가 획득을 코루틴으로 시도합니다.
     *
     * ```kotlin
     * val semaphore = LettuceSuspendSemaphore(connection, "my-sem", totalPermits = 3)
     * semaphore.initialize()
     * val acquired = semaphore.tryAcquire()
     * // acquired == true
     * ```
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부
     */
    suspend fun tryAcquire(permits: Int = 1): Boolean {
        permits.requirePositiveNumber("permits")
        val token = UUID.randomUUID().toString()
        val now = nowMillis()

        val result = RedisScriptRunner.runSuspending<Long>(
            asyncCommands, LettuceSemaphoreScripts.ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(),
            now.toString(),
            totalPermits.toString(),
            permits.toString(),
            token,
            (now + leaseMillis).toString(),
        )
        val acquired = result >= 0
        if (acquired) {
            localPermits.record(token, permits)
        }
        log.debug { "Semaphore tryAcquire: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
        return acquired
    }

    /**
     * 허가를 획득할 때까지 코루틴으로 대기합니다.
     *
     * ```kotlin
     * val semaphore = LettuceSuspendSemaphore(connection, "my-sem", totalPermits = 3)
     * semaphore.initialize()
     * semaphore.acquire()
     * try {
     *     doWork()
     * } finally {
     *     semaphore.release()
     * }
     * ```
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @throws IllegalStateException 지정된 시간 내에 허가를 획득하지 못한 경우
     */
    suspend fun acquire(permits: Int = 1, waitTime: Duration = Duration.ofSeconds(30)) {
        permits.requirePositiveNumber("permits")

        val deadline = System.currentTimeMillis() + waitTime.toMillis()
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(permits)) return
            delay(RETRY_DELAY_MS.milliseconds)
        }
        throw IllegalStateException("세마포어 획득 시간 초과 (suspend): semaphoreKey=$semaphoreKey, permits=$permits")
    }

    /**
     * 허가를 코루틴으로 반납합니다.
     *
     * ```kotlin
     * semaphore.acquire()
     * try {
     *     doWork()
     * } finally {
     *     semaphore.release()
     *     val remaining = semaphore.availablePermits()
     *     // remaining == totalPermits (반납 후 복원)
     * }
     * ```
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     */
    suspend fun release(permits: Int = 1) {
        permits.requirePositiveNumber("permits")

        val releases = localPermits.select(permits)
        releases.forEach { release ->
            val remaining = RedisScriptRunner.runSuspending<Long>(
                asyncCommands, LettuceSemaphoreScripts.RELEASE_SCRIPT, ScriptOutputType.INTEGER,
                semaphoreKeys(),
                nowMillis().toString(),
                totalPermits.toString(),
                release.permits.toString(),
                release.token,
            )
            handleReleaseResult(release, remaining)
            log.debug { "Semaphore release: key=$semaphoreKey, permits=${release.permits}, remaining=$remaining" }
        }
    }

    private fun handleReleaseResult(release: PermitRelease, remaining: Long) {
        when {
            remaining >= 0L -> localPermits.markReleased(release)
            remaining == -1L -> {
                localPermits.markLost(release)
                error("Semaphore permits are no longer owned or already expired: semaphoreKey=$semaphoreKey")
            }
            else -> error("Semaphore release exceeds owned permits: semaphoreKey=$semaphoreKey")
        }
    }

    private fun semaphoreKeys(): Array<String> = arrayOf(semaphoreKey, ownersKey, expirationsKey)

    private fun nowMillis(): Long = System.currentTimeMillis()
}
